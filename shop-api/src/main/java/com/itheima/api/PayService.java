package com.itheima.api;

import com.itheima.shop.pojo.TradePay;

/**
 * @Classname PayService
 * @Description TODO
 * @Date 2020/9/24 17:19
 * @Author Danrbo
 */
public interface PayService {
    void createPayment(TradePay tradePay);

    void callbackPayment(TradePay tradePay);

}
