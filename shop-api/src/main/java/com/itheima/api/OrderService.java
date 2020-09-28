package com.itheima.api;

import com.itheima.entity.Result;
import com.itheima.shop.pojo.TradeOrder;

import java.io.UnsupportedEncodingException;

/**
 * @Classname OrderService
 * @Description TODO
 * @Date 2020/9/23 21:42
 * @Author Danrbo
 */
public interface OrderService {
    /**
     * 确认订单
     *
     * @param order 订单对象
     * @return Result 结果
     */
    Result confirmOrder(TradeOrder order) throws UnsupportedEncodingException;

    TradeOrder findOne(Long orderId);

    /**
     * 更新订单
     * @param tradeOrder 待更新的订单对象
     * @return 更新结果
     */
    Result updateOrder(TradeOrder tradeOrder);
}