package com.tct.itd.model;

import lombok.Data;

/**
 * @classname: MasterSlaveInfo
 * @description: 主从状态信息
 * @author: liyunlong
 * @date: 2022/11/29 15:09
 */
@Data
public class MasterSlaveInfo {

    /**
     *  黄网ip
     */
    private String yellowIp;

    /**
     *  紫网ip
     */
    private String purpleIp;

    /**
     *  主从状态 1:主 0:从 -1:未知
     */
    private String status;
}
