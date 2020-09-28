package com.itheima.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.itheima.api.GoodsService;
import com.itheima.constant.ShopCode;
import com.itheima.entity.Result;
import com.itheima.exception.CastException;
import com.itheima.shop.mapper.TradeGoodsMapper;
import com.itheima.shop.mapper.TradeGoodsNumberLogMapper;
import com.itheima.shop.pojo.TradeGoods;
import com.itheima.shop.pojo.TradeGoodsNumberLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @Classname GoodsServiceImpl
 * @Description TODO
 * @Date 2020/9/23 21:50
 * @Author Danrbo
 */
@Component
@Service(interfaceClass = GoodsService.class)
public class GoodsServiceImpl implements GoodsService {
    @Autowired
    TradeGoodsMapper tradeGoodsMapper;
    @Autowired
    TradeGoodsNumberLogMapper goodsNumberLogMapper;

    @Override
    public TradeGoods findOne(Long goodId) {
        return tradeGoodsMapper.selectByPrimaryKey(goodId);
    }

    @Override
    public Result reduceGoodsNum(TradeGoodsNumberLog tradeGoodsNumberLog) {
        try {
            // 对参数进行校验
            if (tradeGoodsNumberLog == null
                    || tradeGoodsNumberLog.getGoodsId() == null
                    || tradeGoodsNumberLog.getGoodsNumber() == null
                    || tradeGoodsNumberLog.getOrderId() == null) {
                CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
            }
            TradeGoods goods = tradeGoodsMapper.selectByPrimaryKey(tradeGoodsNumberLog.getGoodsId());
            if (goods == null) {
                CastException.cast(ShopCode.SHOP_GOODS_NO_EXIST);
            }
            int newGoodsNum = goods.getGoodsNumber() - tradeGoodsNumberLog.getGoodsNumber();
            // 库存不足
            if (newGoodsNum < 0) {
                CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
            }
            goods.setGoodsNumber(newGoodsNum);
            tradeGoodsMapper.updateByPrimaryKey(goods);
            tradeGoodsNumberLog.setLogTime(new Date());
            tradeGoodsNumberLog.setGoodsNumber(-(tradeGoodsNumberLog.getGoodsNumber()));
            goodsNumberLogMapper.insert(tradeGoodsNumberLog);
        } catch (Exception e) {
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }
}
