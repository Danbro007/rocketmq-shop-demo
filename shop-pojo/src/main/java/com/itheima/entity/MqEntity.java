package com.itheima.entity;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class MqEntity implements Serializable {
    private Long orderId;
    private Long couponId;
    private Long userId;
    @Alias("moneyPaid")
    private BigDecimal userMoney;
    private Long goodsId;
    private Integer goodsNumber;

}
