package com.itheima.shop.mq;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.itheima.api.UserService;
import com.itheima.constant.ShopCode;
import com.itheima.entity.MqEntity;
import com.itheima.entity.Result;
import com.itheima.exception.CastException;
import com.itheima.shop.pojo.TradeUserMoneyLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
    private UserService userService;

    @Override
    public void onMessage(MessageExt message) {
        MqEntity mqEntity = null;
        TradeUserMoneyLog userMoneyLog = null;
        try {
            String body = new String(message.getBody(), RemotingHelper.DEFAULT_CHARSET);
            mqEntity = JSON.parseObject(body, MqEntity.class);
            if (mqEntity.getUserMoney() == null || mqEntity.getUserMoney().compareTo(BigDecimal.ZERO) <= 0) {
                CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
            }
            userMoneyLog = new TradeUserMoneyLog();
            userMoneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_REFUND.getCode());
            BeanUtil.copyProperties(mqEntity, userMoneyLog);
            userMoneyLog.setUseMoney(mqEntity.getUserMoney());
            Result result = doRollBackUserPaid(userMoneyLog);
            if (result.getSuccess()) {
                log.info(String.format("账户【%s】回退余额【%s】元成功", userMoneyLog.getUserId(), userMoneyLog.getUseMoney()));
            } else {
                CastException.cast(ShopCode.SHOP_USER_MONEY_REFUND_FAIL);
            }
        } catch (Exception e) {
            log.info(String.format("账户【%s】回退余额【%s】元失败", userMoneyLog.getUserId(), userMoneyLog.getUseMoney()));

        }
    }

    /**
     * 执行退账户月操作
     *
     * @param userMoneyLog 用户账户金额日志
     * @return 回退金额结果
     */
    private Result doRollBackUserPaid(TradeUserMoneyLog userMoneyLog) {
        // 回退用户余额
        return userService.updateUserMoneyPaid(userMoneyLog);
    }
}