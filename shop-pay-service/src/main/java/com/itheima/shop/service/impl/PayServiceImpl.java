package com.itheima.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.itheima.api.PayService;
import com.itheima.constant.ShopCode;
import com.itheima.entity.Result;
import com.itheima.exception.CastException;
import com.itheima.shop.mapper.TradeMqProducerTempMapper;
import com.itheima.shop.mapper.TradePayMapper;
import com.itheima.shop.pojo.TradeMqProducerTemp;
import com.itheima.shop.pojo.TradePay;
import com.itheima.shop.pojo.TradePayExample;
import com.itheima.utils.IDWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @Classname PayService
 * @Description TODO
 * @Date 2020/9/28 10:30
 * @Author Danrbo
 */
@Slf4j
@Component
@Service
public class PayServiceImpl implements PayService {
    @Autowired
    private TradePayMapper tradePayMapper;

    @Autowired
    private TradeMqProducerTempMapper producerTempMapper;

    @Autowired
    private IDWorker idWorker;

    @Autowired
    RocketMQTemplate rocketMQTemplate;

    @Value("${mq.order.topic}")
    String topic;

    @Value("${mq.order.tag}")
    String tag;

    @Value("{rocketmq.producer.group}")
    String groupName;

    @Autowired
    ThreadPoolTaskExecutor threadPoolExecutor;

    @Override
    public Result createPayment(TradePay tradePay) {
        try {
            if (tradePay == null && tradePay.getOrderId() == null) {
                CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
            }
            // 查询是否已经有当前订单的已支付记录
            TradePayExample tradePayExample = new TradePayExample();
            TradePayExample.Criteria criteria = tradePayExample.createCriteria();
            criteria.andIsPaidEqualTo(ShopCode.SHOP_PAYMENT_IS_PAID.getCode());
            criteria.andOrderIdEqualTo(tradePay.getOrderId());
            int result = tradePayMapper.countByExample(tradePayExample);
            // 订单已经支付成功
            if (result > 0) {
                CastException.cast(ShopCode.SHOP_PAYMENT_IS_PAID);
            }
            // 设置支付订单的状态未支付
            tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
            // 保存到数据库中
            tradePay.setPayId(idWorker.nextId());
            tradePayMapper.insert(tradePay);
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
        } catch (Exception e) {
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
    }

    @Override
    public Result callbackPayment(TradePay tradePay){
        try {
            log.info("进入订单支付回调");
            if (tradePay == null
                    || tradePay.getPayId() == null
                    || tradePay.getOrderId() == null) {
                CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
            }
            // 订单支付成功
            if (tradePay.getIsPaid().intValue() == ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode()) {
                Long payId = tradePay.getPayId();
                TradePay tradePayFromDb = tradePayMapper.selectByPrimaryKey(payId);
                if (tradePayFromDb == null) {
                    CastException.cast(ShopCode.SHOP_PAYMENT_NOT_FOUND);
                }
                // 更新支付订单的状态为已支付
                tradePayFromDb.setIsPaid(ShopCode.SHOP_PAYMENT_IS_PAID.getCode());
                int result = tradePayMapper.updateByPrimaryKeySelective(tradePayFromDb);
                if (result == 1) {
                    log.info(String.format("更新支付订单ID【%s】为已支付状态成功", tradePayFromDb.getPayId()));
                    // 创建要生产的消息
                    TradeMqProducerTemp tradeMqProducerTemp = TradeMqProducerTemp.builder().createTime(new Date()).
                            groupName(groupName).
                            id(Long.toString(idWorker.nextId())).
                            msgTopic(topic).
                            msgTag(tag).
                            msgKey(Long.toString(tradePay.getPayId())).
                            msgBody(JSON.toJSONString(tradePay)).build();
                    producerTempMapper.insert(tradeMqProducerTemp);
                    log.info("消息持久化到数据库");
                    // 发送消息给其他服务修改状态，为了防止线程阻塞把任务放入线程池内。
                   threadPoolExecutor.submit(()->{
                       SendResult sendResult = null;
                       try {
                           sendResult = sendMessage(topic, tag, Long.toString(tradePay.getPayId()), JSON.toJSONString(tradePay));
                       } catch (Exception e) {
                           CastException.cast(ShopCode.SHOP_MQ_SEND_MESSAGE_FAIL);
                       }
                       // 消息发送成功
                       if (sendResult != null && SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
                           log.info(String.format("消息Id【%s】发送成功", tradeMqProducerTemp.getId()));
                           // 删除生产的消息
                           producerTempMapper.deleteByPrimaryKey(tradeMqProducerTemp.getId());
                           log.info(String.format("删除持久化在数据的消息Id【%s】成功", tradeMqProducerTemp.getId()));
                       }
                   });
                }
                // 订单支付失败
            } else {
                CastException.cast(ShopCode.SHOP_PAYMENT_PAY_ERROR);
            }
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
        } catch (Exception e) {
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
    }

    @Override
    public TradePay findOne(Long payId) {
        if (payId == null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return tradePayMapper.selectByPrimaryKey(payId);
    }

    private SendResult sendMessage(String topic, String tag, String key, String body) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        if (StringUtils.isEmpty(topic)) {
            CastException.cast(ShopCode.SHOP_MQ_TOPIC_IS_EMPTY);
        }
        if (StringUtils.isEmpty(body)) {
            CastException.cast(ShopCode.SHOP_MQ_MESSAGE_BODY_IS_EMPTY);
        }
        Message message = new Message(topic, tag, key, body.getBytes());
        return rocketMQTemplate.getProducer().send(message);
    }
}
