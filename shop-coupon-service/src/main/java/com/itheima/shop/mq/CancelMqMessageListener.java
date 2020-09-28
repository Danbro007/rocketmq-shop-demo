package com.itheima.shop.mq;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.itheima.constant.ShopCode;
import com.itheima.entity.MqEntity;
import com.itheima.exception.CastException;
import com.itheima.shop.mapper.TradeCouponMapper;
import com.itheima.shop.pojo.TradeCoupon;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Classname CancelOrderMessageListener
 * @Description TODO
 * @Date 2020/9/25 13:44
 * @Author Danrbo
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}", messageModel = MessageModel.BROADCASTING)
public class CancelMqMessageListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeCouponMapper tradeCouponMapper;

    @Override
    public void onMessage(MessageExt message) {
        String body = null;
        MqEntity mqEntity = null;
        Long couponId = null;
        // 解析消息
        try {
            body = new String(message.getBody(), RemotingHelper.DEFAULT_CHARSET);
            mqEntity = JSON.parseObject(body, MqEntity.class);
            couponId = mqEntity.getCouponId();
            // 回退优惠券状态
            doRollBackCoupon(couponId);
            log.info(String.format("优惠券【%s】回退成功", couponId));
        } catch (Exception e) {
            e.printStackTrace();
            log.info(String.format("优惠券【%s】回退失败", couponId));
        }
    }

    /**
     * 回退优惠券
     *
     * @param couponId 优惠券 ID
     */
    private void doRollBackCoupon(Long couponId) {
        if (couponId == null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        TradeCoupon tradeCouponFromDb = tradeCouponMapper.selectByPrimaryKey(couponId);
        if (tradeCouponFromDb == null) {
            CastException.cast(ShopCode.SHOP_COUPON_NO_EXIST);
        }
        TradeCoupon tradeCoupon = new TradeCoupon();
        BeanUtil.copyProperties(tradeCouponFromDb, tradeCoupon);
        tradeCoupon.setOrderId(null);
        tradeCoupon.setUsedTime(null);
        tradeCoupon.setIsUsed(ShopCode.SHOP_COUPON_UNUSED.getCode());
        tradeCoupon.setCouponId(couponId);
        // 把优惠券所有字段更新
        tradeCouponMapper.updateByPrimaryKey(tradeCoupon);
    }
}
