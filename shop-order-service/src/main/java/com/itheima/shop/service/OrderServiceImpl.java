package com.itheima.shop.service;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.itheima.api.CouponService;
import com.itheima.api.GoodsService;
import com.itheima.api.OrderService;
import com.itheima.api.UserService;
import com.itheima.constant.ShopCode;
import com.itheima.entity.MqEntity;
import com.itheima.entity.Result;
import com.itheima.exception.CastException;
import com.itheima.shop.mapper.TradeOrderMapper;
import com.itheima.shop.pojo.*;
import com.itheima.utils.IDWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Classname OrderServiceImpl
 * @Description TODO
 * @Date 2020/9/23 21:44
 * @Author Danrbo
 */
@Slf4j
@Component
@Service(interfaceClass = OrderService.class)
public class OrderServiceImpl implements OrderService {
    @Reference
    GoodsService goodsService;
    @Reference
    UserService userService;
    @Autowired
    IDWorker idWorker;
    @Reference
    CouponService couponService;
    @Autowired
    TradeOrderMapper tradeOrderMapper;
    @Autowired
    RocketMQTemplate rocketMQTemplate;
    @Value("${mq.order.topic}")
    String topic;
    @Value("${mq.order.tag.cancel}")
    String tag;

    @Override
    public Result confirmOrder(TradeOrder order) {
        // 订单校验
        checkOrder(order);
        // 预订单创建
        createPreOrder(order);
        try {
            // 扣减库存
            reduceGoodsNum(order);
            // 扣减优惠券
            updateCouponStatus(order);
            // 扣减用户余额
            reduceUserMoneyPaid(order);
            // 模拟出现异常
            CastException.cast(ShopCode.SHOP_FAIL);
            // 设置订单为可见
            order.setOrderStatus(ShopCode.SHOP_ORDER_CONFIRM.getCode());
        } catch (Exception e) {
            // 发送优惠券回退、库存回退和余额回退的消息给各自服务
            MqEntity entity = new MqEntity();
            BeanUtil.copyProperties(order, entity);
            sendCancelOrder(topic, tag, order.getOrderId(), entity);
            return new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
        }
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }

    @Override
    public TradeOrder findOne(Long orderId) {
        if (orderId == null || orderId <= 0 ){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return tradeOrderMapper.selectByPrimaryKey(orderId);
    }

    @Override
    public Result updateOrder(TradeOrder tradeOrder) {
        if (tradeOrder == null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        int r = tradeOrderMapper.updateByPrimaryKeySelective(tradeOrder);
        if (r == 0){
            return new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
        }
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }


    private void sendCancelOrder(String topic, String tag, Long orderId, MqEntity entity) {
        try {
            Message message = new Message(topic, tag, orderId.toString(), JSON.toJSONString(entity).getBytes());
            // 发送消息
            rocketMQTemplate.getProducer().send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reduceUserMoneyPaid(TradeOrder order) {
        if (order == null || order.getUserId() == null || order.getMoneyPaid().compareTo(BigDecimal.ZERO) < 1) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        TradeUserMoneyLog tradeUserMoneyLog = new TradeUserMoneyLog();
        tradeUserMoneyLog.setCreateTime(new Date());
        tradeUserMoneyLog.setUserId(order.getUserId());
        tradeUserMoneyLog.setUseMoney(order.getMoneyPaid());
        tradeUserMoneyLog.setOrderId(order.getOrderId());
        tradeUserMoneyLog.setMoneyLogType(1);
        Result result = userService.updateUserMoneyPaid(tradeUserMoneyLog);
        if (!result.getSuccess()) {
            CastException.cast(ShopCode.SHOP_MONEY_PAID_LESS_ZERO);
        }

        log.info(String.format("订单：【%s】扣减余额：【%s】成功", order.getOrderId(), order.getMoneyPaid()));
    }

    private void updateCouponStatus(TradeOrder order) {
        Result result = couponService.updateCouponStatus(order);
        if (!result.getSuccess()) {
            CastException.cast(ShopCode.SHOP_COUPON_USE_FAIL);
        }
        log.info(String.format("订单：【%s】使用了优惠券【%s】", order.getOrderId(), order.getCouponId()));
    }

    private void reduceGoodsNum(TradeOrder order) {
        if (order == null) {
            CastException.cast(ShopCode.SHOP_ORDER_INVALID);
        }
        TradeGoodsNumberLog goodsNumberLog = new TradeGoodsNumberLog();
        goodsNumberLog.setGoodsNumber(order.getGoodsNumber());
        goodsNumberLog.setLogTime(new Date());
        goodsNumberLog.setGoodsId(order.getGoodsId());
        goodsNumberLog.setOrderId(order.getOrderId());
        Result result = goodsService.reduceGoodsNum(goodsNumberLog);
        if (!result.getSuccess()) {
            CastException.cast(ShopCode.SHOP_REDUCE_GOODS_NUM_FAIL);
        }
        log.info(String.format("订单：【%s】扣减商品【%s】库存：【%s】成功", order.getOrderId(), order.getGoodsId(), order.getGoodsNumber()));
    }

    private Long createPreOrder(TradeOrder order) {
        // 把订单状态设置为不可见
        order.setOrderStatus(ShopCode.SHOP_ORDER_NO_CONFIRM.getCode());
        // 订单编号
        order.setOrderId(idWorker.nextId());
        // 核算运费
        BigDecimal shippingFee = calculateShippingFee(order);
        if (shippingFee.compareTo(order.getShippingFee()) != 0) {
            CastException.cast(ShopCode.SHOP_ORDER_SHIPPINGFEE_INVALID);
        }
        // 核算订单总价
        BigDecimal orderAmount = order.getGoodsPrice().multiply(new BigDecimal(order.getGoodsNumber()));
        if (orderAmount.compareTo(order.getOrderAmount()) != 0) {
            CastException.cast(ShopCode.SHOP_ORDERAMOUNT_INVALID);
        }
        // 判断优惠券是否合法
        Long couponId = order.getCouponId();
        if (couponId != null) {
            TradeCoupon coupon = couponService.findOne(couponId);
            // 没有这种优惠券则抛出异常
            if (coupon == null) {
                CastException.cast(ShopCode.SHOP_COUPON_NO_EXIST);
            }
            // 判断优惠券是否使用过
            if (ShopCode.SHOP_COUPON_ISUSED.getCode().equals(coupon.getIsUsed())) {
                CastException.cast(ShopCode.SHOP_COUPON_ISUSED);
            }
            // 设置订单的优惠金额
            order.setCouponPaid(coupon.getCouponPrice());
        } else {
            order.setCouponPaid(BigDecimal.ZERO);
        }
        // 判断余额是否正确
        BigDecimal moneyPaid = order.getMoneyPaid();
        if (moneyPaid != null) {
            TradeUser user = userService.findOne(order.getUserId());
            if (user == null) {
                CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
            }
            // 如果订单的余额大于账户余额
            if (moneyPaid.compareTo(user.getUserMoney()) > 0) {
                CastException.cast(ShopCode.SHOP_MONEY_PAID_INVALID);
            }
        } else {
            // 余额为空则设置为 0
            order.setMoneyPaid(BigDecimal.ZERO);
        }
        // 核算订单总价 订单总价 = 商品总价 - 优惠券价格 - 余额
        order.setPayAmount(order.getOrderAmount().subtract(order.getCouponPaid().add(order.getMoneyPaid())));
        order.setAddTime(new Date());
        int r = tradeOrderMapper.insert(order);
        if (ShopCode.SHOP_SUCCESS.getCode() != r) {
            CastException.cast(ShopCode.SHOP_ORDER_SAVE_ERROR);
        }
        log.info("预订单创建成功");
        return order.getOrderId();

    }

    private BigDecimal calculateShippingFee(TradeOrder order) {
        // 总金额大于 100 元收取运费 10 元
        if (order.getOrderAmount().compareTo(new BigDecimal(100)) > 0) {
            return new BigDecimal(10);
        }
        return BigDecimal.ZERO;
    }

    public void checkOrder(TradeOrder order) {
        // 校验订单是否为空
        if (order == null) {
            CastException.cast(ShopCode.SHOP_ORDER_INVALID);
        }
        //校验商品是否存在
        assert order != null;
        TradeGoods goods = goodsService.findOne(order.getGoodsId());
        if (goods == null) {
            CastException.cast(ShopCode.SHOP_GOODS_NO_EXIST);
        }
        // 校验用户是否存在
        TradeUser user = userService.findOne(order.getUserId());
        if (user == null) {
            CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
        }
        //校验商品价格
        assert goods != null;
        if (order.getGoodsPrice().compareTo(goods.getGoodsPrice()) != 0) {
            CastException.cast(ShopCode.SHOP_GOODS_PRICE_INVALID);
        }
        // 校验库存
        if (order.getGoodsNumber() >= goods.getGoodsNumber()) {
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }
        log.info("订单校验通过");
    }
}
