package com.itheima.api;


import com.itheima.entity.Result;
import com.itheima.shop.pojo.TradeUser;
import com.itheima.shop.pojo.TradeUserMoneyLog;

/**
 * @Classname UserService
 * @Description TODO
 * @Date 2020/9/23 21:54
 * @Author Danrbo
 */
public interface UserService {
    /**
     * 通过用户 ID 找到用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    TradeUser findOne(Long userId);

    /**
     * 扣减用户账户余额
     *
     * @param userMoneyLog 用户扣减余额日志
     * @return 更新结果
     */
    Result updateUserMoneyPaid(TradeUserMoneyLog userMoneyLog);

    void insert(TradeUserMoneyLog tradeUserMoneyLog);


}
