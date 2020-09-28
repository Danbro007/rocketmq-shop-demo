package com.itheima.api;

import com.itheima.entity.Result;
import com.itheima.shop.pojo.TradeGoods;
import com.itheima.shop.pojo.TradeGoodsNumberLog;


/**
 * @Classname GoodsService
 * @Description TODO
 * @Date 2020/9/23 21:45
 * @Author Danrbo
 */
public interface GoodsService {
    /**
     * 通过商品 ID 查找一个商品信息
     * @param goodId 商品ID
     * @return 商品信息
     */
    TradeGoods findOne(Long goodId);

    /**
     * 扣减商品库存
     * @param tradeGoodsNumberLog 商品日志对象
     * @return 扣减结果
     */
    Result reduceGoodsNum(TradeGoodsNumberLog tradeGoodsNumberLog);
}
