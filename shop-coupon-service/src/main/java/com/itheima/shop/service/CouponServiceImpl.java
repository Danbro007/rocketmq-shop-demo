package com.itheima.shop.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.itheima.api.CouponService;
import com.itheima.constant.ShopCode;
import com.itheima.entity.Result;
import com.itheima.exception.CastException;
import com.itheima.shop.mapper.TradeCouponMapper;
import com.itheima.shop.pojo.TradeCoupon;
import com.itheima.shop.pojo.TradeOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @Classname CouponServiceImpl
 * @Description TODO
 * @Date 2020/9/23 23:28
 * @Author Danrbo
 */
@Component
@Service(interfaceClass = CouponService.class)
public class CouponServiceImpl implements CouponService {
    @Autowired
    TradeCouponMapper tradeCouponMapper;

    @Override
    public TradeCoupon findOne(Long couponId) {
        if (couponId == null) {
            CastException.cast(ShopCode.SHOP_COUPON_INVALIED);
        }
        return tradeCouponMapper.selectByPrimaryKey(couponId);
    }

    @Override
    public Result updateCouponStatus(TradeOrder order) {
        try {
            if (order.getCouponId() == null) {
                CastException.cast(ShopCode.SHOP_COUPON_INVALIED);
            }
            TradeCoupon coupon = findOne(order.getCouponId());
            if (coupon == null) {
                CastException.cast(ShopCode.SHOP_COUPON_NO_EXIST);
            }
            // 把优惠券标记为已使用
            coupon.setIsUsed(1);
            coupon.setUsedTime(new Date());
            coupon.setOrderId(order.getOrderId());

            int result = tradeCouponMapper.updateByPrimaryKey(coupon);
            if (result != ShopCode.SHOP_SUCCESS.getCode()) {
                CastException.cast(ShopCode.SHOP_COUPON_USE_FAIL);
            }
        } catch (Exception e) {
            return new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
        }
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }
}
