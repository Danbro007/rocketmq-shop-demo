package com.itheima.shop.pojo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
@Data
@Builder
public class TradeMqProducerTemp implements Serializable {
    private String id;

    private String groupName;

    private String msgTopic;

    private String msgTag;

    private String msgKey;

    private String msgBody;

    private Integer msgStatus;

    private Date createTime;

}