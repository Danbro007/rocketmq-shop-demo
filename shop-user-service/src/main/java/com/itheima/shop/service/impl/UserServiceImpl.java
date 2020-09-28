package com.itheima.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.itheima.api.UserService;
import com.itheima.constant.ShopCode;
import com.itheima.entity.Result;
import com.itheima.exception.CastException;
import com.itheima.shop.mapper.TradeUserMapper;
import com.itheima.shop.mapper.TradeUserMoneyLogMapper;
import com.itheima.shop.pojo.TradeUser;
import com.itheima.shop.pojo.TradeUserMoneyLog;
import com.itheima.shop.pojo.TradeUserMoneyLogExample;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Classname UserServiceImpl
 * @Description TODO
 * @Date 2020/9/23 21:53
 * @Author Danrbo
 */
@Slf4j
@Component
@Service(interfaceClass = UserService.class)
public class UserServiceImpl implements UserService {
    @Autowired
    TradeUserMapper tradeUserMapper;

    @Autowired
    TradeUserMoneyLogMapper tradeUserMoneyLogMapper;

    @Override
    public TradeUser findOne(Long userId) {
        if (userId == null) {
            CastException.cast(ShopCode.SHOP_USER_IS_NULL);
        }
        return tradeUserMapper.selectByPrimaryKey(userId);
    }

    @Override
    public Result updateUserMoneyPaid(TradeUserMoneyLog userMoneyLog) {
        Result result = null;
        try {
            if (userMoneyLog == null
                    || userMoneyLog.getUseMoney() == null
                    || userMoneyLog.getUseMoney().compareTo(BigDecimal.ZERO) < 0
                    || userMoneyLog.getUserId() == null
                    || userMoneyLog.getOrderId() == null) {
                CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
            }
            TradeUser user = tradeUserMapper.selectByPrimaryKey(userMoneyLog.getUserId());
            if (user == null) {
                CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
            }
            // 如果类型是付款类型
            if (userMoneyLog.getMoneyLogType().compareTo(ShopCode.SHOP_USER_MONEY_PAID.getCode()) == 0) {
                // 先查询用户账户金额记录里有没有相关记录，如果有说明已经用余额付过
                TradeUserMoneyLogExample userMoneyLogExample = new TradeUserMoneyLogExample();
                userMoneyLogExample.createCriteria()
                        .andUserIdEqualTo(userMoneyLog.getUserId())
                        .andOrderIdEqualTo(userMoneyLog.getOrderId())
                        .andMoneyLogTypeEqualTo(ShopCode.SHOP_USER_MONEY_PAID.getCode());
                int paidResult = tradeUserMoneyLogMapper.countByExample(userMoneyLogExample);
                // 说明该订单已经使用过余额
                if (paidResult > 0) {
                    CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY);
                }
                // 扣减余额
                result = reduceUserMoneyPaid(userMoneyLog, user);
                if (!result.getSuccess()) {
                    CastException.cast(ShopCode.SHOP_USER_MONEY_REDUCE_FAIL);
                } }
            // 退款
            else if (userMoneyLog.getMoneyLogType().compareTo(ShopCode.SHOP_USER_MONEY_REFUND.getCode()) == 0) {
                TradeUserMoneyLogExample userMoneyLogExample2 = new TradeUserMoneyLogExample();
                userMoneyLogExample2.createCriteria()
                        .andUserIdEqualTo(userMoneyLog.getUserId())
                        .andOrderIdEqualTo(userMoneyLog.getOrderId())
                        .andMoneyLogTypeEqualTo(ShopCode.SHOP_USER_MONEY_REFUND.getCode());
                int refundResult = tradeUserMoneyLogMapper.countByExample(userMoneyLogExample2);
                // 说明已经退过款了
                if (refundResult > 0) {
                    CastException.cast(ShopCode.SHOP_USER_MONEY_REFUND_ALREADY);
                }
                // 执行退款
                result = refundUserMoneyPaid(userMoneyLog, user);
                if (!result.getSuccess()) {
                    CastException.cast(ShopCode.SHOP_USER_MONEY_REFUND_FAIL);
                };
            }
            // 插入新的用户金额日志记录
            userMoneyLog.setCreateTime(new Date());
            tradeUserMoneyLogMapper.insert(userMoneyLog);
        } catch (Exception e) {
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
        return result;
    }

    @Override
    public void insert(TradeUserMoneyLog tradeUserMoneyLog) {
        tradeUserMoneyLogMapper.insert(tradeUserMoneyLog);
    }

    /**
     * 扣减用户余额
     * @param userMoneyLog 用户金额日志
     * @param user 用户对象
     * @return 扣减结果
     */
    private Result reduceUserMoneyPaid(TradeUserMoneyLog userMoneyLog, TradeUser user) {
        BigDecimal userMoney = user.getUserMoney();
        user.setUserMoney(userMoney.subtract(userMoneyLog.getUseMoney()));
        int result = tradeUserMapper.updateByPrimaryKey(user);
        if (result == ShopCode.SHOP_FAIL.getCode()) {
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }

    /**
     * 退款到用户账户
     * @param userMoneyLog 用户用户金额日志
     * @param user 用户对象
     * @return 退款结果
     */
    private Result refundUserMoneyPaid(TradeUserMoneyLog userMoneyLog, TradeUser user) {
        BigDecimal userMoney = user.getUserMoney();
        user.setUserMoney(userMoney.add(userMoneyLog.getUseMoney()));
        int result = tradeUserMapper.updateByPrimaryKey(user);
        if (result == ShopCode.SHOP_FAIL.getCode()) {
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }

}
