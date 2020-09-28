package com.itheima.shop.mq;

import com.alibaba.fastjson.JSON;
import com.itheima.api.OrderService;
import com.itheima.api.PayService;
import com.itheima.constant.ShopCode;
import com.itheima.exception.CastException;
import com.itheima.shop.pojo.TradeOrder;
import com.itheima.shop.pojo.TradePay;
import jdk.nashorn.internal.ir.annotations.Reference;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * @Classname PaymentMessageListener
 * @Description TODO
 * @Date 2020/9/28 11:50
 * @Author Danrbo
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.pay.topic}", messageModel = MessageModel.BROADCASTING, consumerGroup = "${mq.pay.consumer.group.name}")
public class PaymentMessageListener implements RocketMQListener<MessageExt> {

    @Autowired
    OrderService orderService;
    @Reference
    PayService payService;

    @Override
    public void onMessage(MessageExt message) {
        log.info("接收到支付成功的消息");
        try {
            TradePay tradePay = JSON.parseObject(new String(message.getBody(), Charset.defaultCharset()), TradePay.class);
            if (tradePay != null && tradePay.getPayId() != null) {
                TradeOrder tradeOrderFromDb = orderService.findOne(tradePay.getOrderId());
                if (tradeOrderFromDb == null) {
                    CastException.cast(ShopCode.SHOP_ORDER_NO_EXIST);
                }
                tradeOrderFromDb.setPayStatus(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
                orderService.updateOrder(tradeOrderFromDb);
                log.info("已把订单【%s】的支付状态修改为已支付状态");
            }
        } catch (Exception e) {
            CastException.cast(ShopCode.SHOP_ORDER_STATUS_UPDATE_FAIL);
        }


    }
}
