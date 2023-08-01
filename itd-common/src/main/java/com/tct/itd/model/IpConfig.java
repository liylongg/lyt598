package com.tct.itd.model;

import lombok.Data;
import lombok.ToString;

/**
 * @author kangyi
 * @description hubIp配置实体类
 * @date 2021/11/4
 **/
@Data
@ToString
public class IpConfig {
    //id
    private String ip;
    //端口
    private int port;
    //服务器名称
    private String hostName;
    //连接资源
    private int source;
    //设备类型：0：智能调度适配器1：应用服务器
    private int type;
    //黄紫网：0:黄网1：紫网
    private int color;
    //是否可用：0：否1：是
    private int enable;
}
