package com.itheima.test;

import com.itheima.api.OrderService;
import com.itheima.entity.Result;
import com.itheima.shop.OrderServiceApplication;
import com.itheima.shop.pojo.TradeOrder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.math.BigDecimal;
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrderServiceApplication.class)
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Test
    public void confirmOrder() throws IOException {

        Long coupouId = 345988230098857984L;
        Long goodsId = 345959443973935104L;
        Long userId = 345963634385633280L;

        TradeOrder order = new TradeOrder();
        order.setGoodsId(goodsId);
        order.setUserId(userId);
        order.setCouponId(coupouId);
        order.setAddress("北京");
        order.setGoodsNumber(1);
        order.setGoodsPrice(new BigDecimal(1000));
        order.setShippingFee(new BigDecimal(10));
        order.setOrderAmount(new BigDecimal(1000));
        order.setMoneyPaid(new BigDecimal(100));
        order.setGoodsAmount(new BigDecimal(order.getGoodsNumber()).multiply(order.getGoodsPrice()));
        Result result = orderService.confirmOrder(order);
        if (!result.getSuccess()){
            log.info(String.format("订单【%s】确认失败", order.getOrderId()));
        }else {
            log.info(String.format("订单【%s】确认成功", order.getOrderId()));
        }
        System.in.read();

    }
    

}
