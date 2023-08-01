package com.tct.itd.model;

import lombok.Builder;
import lombok.Data;

/**
 * @classname: PushMsg
 * @description: 向客户端推送消息类
 * @author: liyunlong
 * @date: 2022/5/9 15:13
 */
@Data
@Builder
public class PushMsg {

    /**
     * 推送的消息
     */
    private ClientMsg message;

    /**
     * 接收消息的客户端id
     */
    private String stationId;

}
