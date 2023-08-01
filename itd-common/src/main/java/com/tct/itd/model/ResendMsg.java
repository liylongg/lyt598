package com.tct.itd.model;

import lombok.Builder;
import lombok.Data;

/**
 * @classname: ResendMsg
 * @description: 长连接消息重发
 * @author: liyunlong
 * @date: 2022/9/21 15:29
 */
@Data
@Builder
public class ResendMsg {

    /**
     * 发送次数
     */
    private Integer sendCount;

    /**
     * 发送消息内容
     */
    private String msgJson;

    /**
     *  发送stationId
     */
    private String stationId;


}
