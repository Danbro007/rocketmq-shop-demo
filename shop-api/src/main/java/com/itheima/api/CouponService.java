package com.itheima.api;

import com.itheima.entity.Result;
import com.itheima.shop.pojo.TradeCoupon;
import com.itheima.shop.pojo.TradeOrder;

/**
 * @Classname CouponService
 * @Description TODO
 * @Date 2020/9/23 23:25
 * @Author Danrbo
 */
public interface CouponService {
    /**
     * 通过优惠券 ID 查找优惠券
     * @param couponId 优惠券ID
     * @return 优惠券对象
     */
    TradeCoupon findOne(Long couponId);

    /**
     * 更新优惠券状态
     * @param order 订单对象
     * @return 更新成功的结果
     */
    Result updateCouponStatus(TradeOrder order);
}
