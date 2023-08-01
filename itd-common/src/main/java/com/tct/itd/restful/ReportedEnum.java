package com.tct.itd.restful;

/**
 * @Description: 上报id 协议中infoid
 * @Author: YHF
 * @date 2020/5/20
 */
public enum ReportedEnum {
    TRACE("TRACE", 10001, "tias", "列车追踪数据"),
    STATION_SYNC("STATION_SYNC", 10002 , "tias", "站场同步数据"),
    COMMAND_RESULT("COMMAND_RESULT", 10003 , "tias", "命令应答数据"),
    HOLD_TRACE_NOTIFY("HOLD_TRACE_NOTIFY", 10004 , "tias", "扣车/抬车通知"),
    TRAIN_GRAPH("TRAIN_GRAPH", 10005 , "tias", "实际运行图"),
    TRAIN_DOOR_INFO("TRAIN_DOOR_INFO", 10006 , "ats", "车门故障隔离站台门信息"),
    VOBC_ALARM_DATA("TRAIN_GRAPH", 10007 , "ats", "VOBC报警数据帧"),
    //10201 起始 电子调度命令
    DISPATCH_COMMAND("DISPATCH_COMMAND", 10201 , "adm服务", "电子调度命令"),
    //推送运行图调整方案列表
    WEB_NOTICE_GRAPH_CASE("WEB_NOTICE_GRAPH_CASE", 10202, "adm服务", "推送运行图调整方案列表")
    ;

    //名称
    private String infoIdName;
    //上报id
    private int infoId;
    //所属微服务
    private String Microservices;
    //描述
    private String describe;

    ReportedEnum(String infoIdName, int infoId, String microservices, String describe) {
        this.infoIdName = infoIdName;
        this.infoId = infoId;
        this.Microservices = microservices;
        this.describe = describe;
    }

    public String getinfoIdName() {
        return infoIdName;
    }

    public void setinfoIdName(String infoIdName) {
        this.infoIdName = infoIdName;
    }

    public int getinfoId() {
        return infoId;
    }

    public void setinfoId(int infoId) {
        this.infoId = infoId;
    }
}