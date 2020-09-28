package com.itheima.shop.mq;

import com.alibaba.fastjson.JSON;
import com.itheima.constant.ShopCode;
import com.itheima.entity.MqEntity;
import com.itheima.exception.CastException;
import com.itheima.shop.mapper.TradeGoodsMapper;
import com.itheima.shop.mapper.TradeMqConsumerLogMapper;
import com.itheima.shop.pojo.TradeGoods;
import com.itheima.shop.pojo.TradeMqConsumerLog;
import com.itheima.shop.pojo.TradeMqConsumerLogExample;
import com.itheima.shop.pojo.TradeMqConsumerLogKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @Classname CancelMqMessageListener
 * @Description TODO
 * @Date 2020/9/25 13:51
 * @Author Danrbo
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}", messageModel = MessageModel.BROADCASTING)
public class CancelMqMessageListener implements RocketMQListener<MessageExt> {
    @Value("${mq.order.consumer.group.name}")
    private String groupName;

    @Autowired
    private TradeMqConsumerLogMapper tradeMqConsumerLogMapper;

    @Autowired
    private TradeGoodsMapper tradeGoodsMapper;

    private static Integer CONSUMER_TIMES_LIMIT = 3;

    // 监听商品库存回退信息
    @Override
    public void onMessage(MessageExt message) {
        String msgId = null;
        String body = null;
        String tags = null;
        String keys = null;
        try {
            msgId = message.getMsgId();
            body = new String(message.getBody(), RemotingHelper.DEFAULT_CHARSET);
            tags = message.getTags();
            keys = message.getKeys();
            MqEntity mqEntity = JSON.parseObject(body, MqEntity.class);
            // 查询mq里的消费消费记录
            TradeMqConsumerLog tradeMqConsumerLog = new TradeMqConsumerLog();
            tradeMqConsumerLog.setMsgTag(tags);
            tradeMqConsumerLog.setMsgKey(keys);
            tradeMqConsumerLog.setGroupName(groupName);
            TradeMqConsumerLog mqConsumerLogFromDb = tradeMqConsumerLogMapper.selectByPrimaryKey(tradeMqConsumerLog);
            if (mqConsumerLogFromDb != null) {
                mqConsumerLogExist(msgId, keys, mqConsumerLogFromDb);
            } else {
                mqConsumerLogFromDb = mqConsumerLogNotExist(body, tags, keys);
            }
            // 执行回退库存操作
            doRollbackGoods(mqEntity, mqConsumerLogFromDb);
        } catch (Exception e) {
            e.printStackTrace();
            // 执行消息消费失败的操作
            TradeMqConsumerLogKey mqConsumerLogKey = new TradeMqConsumerLogKey();
            mqConsumerLogKey.setGroupName(groupName);
            mqConsumerLogKey.setMsgKey(keys);
            mqConsumerLogKey.setMsgTag(tags);
            TradeMqConsumerLog mqConsumerLog = tradeMqConsumerLogMapper.selectByPrimaryKey(mqConsumerLogKey);
            // 之前没有消息消费记录则添加
            if (mqConsumerLog == null) {
                TradeMqConsumerLog consumerLog = new TradeMqConsumerLog();
                consumerLog.setMsgBody(body);
                consumerLog.setMsgTag(tags);
                consumerLog.setMsgId(msgId);
                consumerLog.setGroupName(groupName);
                consumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode());
                consumerLog.setConsumerTimes(1);
                tradeMqConsumerLogMapper.insert(consumerLog);
            } else {
                // 消息消费次数加 1
                mqConsumerLog.setConsumerTimes(mqConsumerLog.getConsumerTimes() + 1);
                tradeMqConsumerLogMapper.updateByPrimaryKeySelective(mqConsumerLog);
            }

        }


    }

    /**
     * 执行库存回退
     *
     * @param mqEntity MQ消息体
     */
    private void doRollbackGoods(MqEntity mqEntity, TradeMqConsumerLog mqConsumerLog) {
        if (mqEntity == null || mqEntity.getGoodsId() == null) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        Long goodsId = mqEntity.getGoodsId();
        Integer goodsNumber = mqEntity.getGoodsNumber();
        TradeGoods tradeGoodsFromDb = tradeGoodsMapper.selectByPrimaryKey(goodsId);
        if (tradeGoodsFromDb == null) {
            CastException.cast(ShopCode.SHOP_GOODS_NO_EXIST);
        }
        TradeGoods tradeGoods = new TradeGoods();
        tradeGoods.setGoodsNumber(goodsNumber + tradeGoodsFromDb.getGoodsNumber());
        tradeGoods.setGoodsId(goodsId);
        // 更新商品库存
        tradeGoodsMapper.updateByPrimaryKeySelective(tradeGoods);
        // 更新消息消费记录
        mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode());
        mqConsumerLog.setConsumerTimestamp(new Date());
        tradeMqConsumerLogMapper.updateByPrimaryKeySelective(mqConsumerLog);
        log.info("订单【%s】的商品【%s】库存回退成功");
    }

    /**
     * 消息消费记录不存在则添加新的消息消费记录
     *
     * @param body 消息体
     * @param tags 消息 tag
     * @param keys 消息 key
     */
    private TradeMqConsumerLog mqConsumerLogNotExist(String body, String tags, String keys) {
        // 说明之前没有消费过则添加新的MQ消费记录到数据库
        TradeMqConsumerLog newMqConsumerLog = new TradeMqConsumerLog();
        newMqConsumerLog.setGroupName(groupName);
        newMqConsumerLog.setMsgKey(keys);
        newMqConsumerLog.setMsgTag(tags);
        newMqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());
        newMqConsumerLog.setMsgBody(body);
        newMqConsumerLog.setConsumerTimes(0);
        tradeMqConsumerLogMapper.insert(newMqConsumerLog);
        return newMqConsumerLog;
    }

    /**
     * 消息消息之前存在，既可能有三种情况：
     * 1、消息已经处理过
     * 2、消息在处理中
     * 3、消息处理失败
     *
     * @param msgId            消息ID
     * @param keys             消息Key
     * @param mqConsumerFromDb 数据库查询到的消息消费日志记录
     */
    private void mqConsumerLogExist(String msgId, String keys, TradeMqConsumerLog mqConsumerFromDb) {
        // 1. 消息已经处理
        if (mqConsumerFromDb.getConsumerStatus().intValue() == ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode()) {
            log.info(String.format("消息ID【%s】已经处理过", msgId));
        }
        // 2. 消息处理中
        else if (mqConsumerFromDb.getConsumerStatus().intValue() == ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode()) {
            log.info(String.format("消息ID【%s】正在处理中", msgId));
        }
        // 3. 消费失败
        else {
            // 重复消费三次以上
            if (mqConsumerFromDb.getConsumerStatus() > CONSUMER_TIMES_LIMIT) {
                log.info(String.format("消息ID【%s】消费次数超过3次", msgId));
                return;
            }
            // 乐观锁再次尝试更新
            mqConsumerFromDb.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());
            TradeMqConsumerLogExample tradeMqConsumerLogExample = new TradeMqConsumerLogExample();
            TradeMqConsumerLogExample.Criteria criteria = tradeMqConsumerLogExample.createCriteria();
            criteria.andMsgIdEqualTo(msgId);
            criteria.andGroupNameEqualTo(groupName);
            criteria.andMsgKeyEqualTo(keys);
            criteria.andConsumerTimesEqualTo(mqConsumerFromDb.getConsumerStatus());
            int r = tradeMqConsumerLogMapper.updateByExampleSelective(mqConsumerFromDb, tradeMqConsumerLogExample);
            if (r < 1) {
                log.info("修改失败，可能是其他线程已经修改过了");
            }
        }
    }
}
