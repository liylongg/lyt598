package com.tct.itd.adm.iconstant;

import java.util.Arrays;

/*
 * @Description:命令id 协议中cmdid
 * @Author: YHF
 * @date 2020/5/20
 */
public enum CommandEnum {
    ON_HOLD_TRAIN("ON_HOLD_TRAIN", 10001, 202001,"tias", "一键扣车/抬车"),
    CHANGE_TRAIN_GRAPH("CHANGE_TRAIN_GRAPH", 10005 , 204001,"tias", "下发运行图调整命令"),

    //10101 起始 推荐指令命令
    ADM_IDEA("ADM_IDEA", 10101 , 206003,"ats", "推荐指令方案"),
    /**
     * 下发运行获取运行图版本号
     */
    GET_TRAIN_GRAPH_VERSION("GET_TRAIN_GRAPH_VERSION", 10004 , 204002,"tias", "下发运行获取运行图版本号"),
//    CONTINGENCY_ANALYSIS_REPORT("CHANGE_TRAIN_GRAPH", 10004 , 204002,"tias", "运行图调整"),
    CREATE_ANALYSIS_REPORT("CREATE_ANALYSIS_REPORT", 20001 , 203001,"tias", "下发生成应急分析报告命令"),
    ALARM_LOG("ALARM_LOG", 20002 , 203001,"tias", "获取报警日志"),
    OPERATE_LOG("OPERATE_LOG", 20003 , 203001,"tias", "获取操作日志"),
    DISPATCH_LIST_LOG("DISPATCH_LIST_LOG", 20004 , 203001,"tias", "获取调度命令列表"),
    DISPATCH_LOG("DISPATCH_LOG", 20005 , 203001,"tias", "获取调度命令"),

    /*
     * 列车自动指令
     */
    TRAIN_AUTO_OFFLINE("TRAIN_AUTO_OFFLINE", 10012 , 205001,"tias", "列车自动掉线"),
    TRAIN_AUTO_OUTAREA("TRAIN_AUTO_OUTAREA", 10013 , 205002,"tias", "备车自动出段"),
    TRAIN_AUTO_STOPSERVICE("TRAIN_AUTO_STOPSERVICE", 10014 , 205003,"tias", "列车自动停运"),

    /**
     * 车站车门处理方案结果
     */
    STATION_CONFIRM_CASE("STATION_CONFIRM_CASE", 10015 , 205004,"adm", "车站车门处理方案结果"),
    /**
     * 车站故障恢复提示框
     */
    STATION_RECOVER_ALERT_WID("STATION_RECOVER_ALERT_WID", 10016, 205005, "adm", "车站故障恢复提示框");

    //命令名称
    private String cmdIdName;
    //命令id
    private int cmdId;
    //消息编号
    private int msgCode;
    //所属微服务
    private String microServices;
    //描述
    private String describe;

    CommandEnum(String cmdIdName, int cmdId, int msgCode, String microServices, String describe) {
        this.cmdIdName = cmdIdName;
        this.cmdId = cmdId;
        this.msgCode=msgCode;
        this.microServices = microServices;
        this.describe = describe;
    }

    public String getcmdIdName() {
        return cmdIdName;
    }

    public void setcmdIdName(String cmdIdName) {
        this.cmdIdName = cmdIdName;
    }

    public int getcmdId() {
        return cmdId;
    }

    public void setcmdId(int cmdId) {
        this.cmdId = cmdId;
    }

    public int getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(int msgCode) {
        this.msgCode = msgCode;
    }
    public static CommandEnum getEnum(int msgCode) {
        return Arrays.stream(CommandEnum.values()).filter(e->e.getMsgCode()==msgCode).findAny().orElse(null);
    }
    public static CommandEnum getEnumByCmdId(int cmdId) {
        return Arrays.stream(CommandEnum.values()).filter(e->e.getcmdId()==cmdId).findAny().orElse(null);
    }
}
