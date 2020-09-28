package com.itheima.shop.mq;

import com.alibaba.fastjson.JSON;
import com.itheima.api.OrderService;
import com.itheima.constant.ShopCode;
import com.itheima.entity.MqEntity;
import com.itheima.entity.Result;
import com.itheima.exception.CastException;
import com.itheima.shop.pojo.TradeOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

/**
 * @Classname CancelMqMessageListener
 * @Description TODO
 * @Date 2020/9/25 13:52
 * @Author Danrbo
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}", messageModel = MessageModel.BROADCASTING)
public class CancelMqMessageListener implements RocketMQListener<MessageExt> {
    @Autowired
    private OrderService orderService;

    // 监听商品订单
    @Override
    public void onMessage(MessageExt message) {
        MqEntity mqEntity = null;
        try {
            String body = new String(message.getBody(), RemotingHelper.DEFAULT_CHARSET);
            mqEntity = JSON.parseObject(body, MqEntity.class);
            TradeOrder tradeOrder = orderService.findOne(mqEntity.getOrderId());
            if (tradeOrder == null) {
                CastException.cast(ShopCode.SHOP_ORDER_NO_EXIST);
            }
            tradeOrder.setOrderStatus(ShopCode.SHOP_ORDER_CANCEL.getCode());
            Result result = orderService.updateOrder(tradeOrder);
            if (result.getSuccess()) {
                log.info(String.format("订单【%s】取消成功", mqEntity.getOrderId()));
            } else {
               CastException.cast(ShopCode.SHOP_USER_MONEY_REFUND_FAIL);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.info(String.format("订单【%s】取消失败", mqEntity.getOrderId()));
        }
    }

}