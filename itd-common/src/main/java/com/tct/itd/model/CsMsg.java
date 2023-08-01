package com.tct.itd.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @classname: CsMsg
 * @description: 客户端、服务器主备切换信息
 * @author: liyunlong
 * @date: 2022/5/7 16:40
 */
@Data
@Builder
public class CsMsg {

    /**
     * 和利时主备状态
     */
    private String hlsMonitorStatus;

    /**
     * 应用服务器主备状态
     */
    private String atsServerStatus;

    /**
     * 服务主从状态信息
     */
    private String masterSlaveStatus;
}
