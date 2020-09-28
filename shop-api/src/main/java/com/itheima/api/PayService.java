package com.itheima.api;

import com.itheima.entity.Result;
import com.itheima.shop.pojo.TradePay;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;

/**
 * @Classname PayService
 * @Description TODO
 * @Date 2020/9/24 17:19
 * @Author Danrbo
 */
public interface PayService {
    Result createPayment(TradePay tradePay);

    Result callbackPayment(TradePay tradePay) throws InterruptedException, RemotingException, MQClientException, MQBrokerException;

    TradePay findOne(Long payId);

}
