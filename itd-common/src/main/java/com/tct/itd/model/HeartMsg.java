package com.tct.itd.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @classname: HeartMsg
 * @description: 心跳信息
 * @author: liyunlong
 * @date: 2022/11/24 15:35
 */
@Getter
@Setter
public class HeartMsg {

    private static HeartMsg instance = new HeartMsg();

    public static HeartMsg getInstance() {
        return instance;
    }

    /**
     *  消息id
     */
    private String msgId;

    /**
     *  消息业务类型 type: "6"->as与gateway心跳。“7”->alg与gateway心跳。“8”gateway与gateway心跳。
     */
    private String type;

    /**
     *  消息目标 as/gateway/alg
     */
    private String target;

    /**
     *  消息类型 心跳信息: -1
     */
    private int msgType;

    /**
     *  服务ip
     */
    private String stationId;

    /**
     *  主从状态信息
     */
    private List<MasterSlaveInfo> masterSlaveInfoList;
}
