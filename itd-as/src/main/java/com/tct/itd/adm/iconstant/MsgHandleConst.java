package com.tct.itd.adm.iconstant;

/**
 * @Description 消息处理常量
 * @Author zhaoke
 * @Date 2020/6/2 17:05
 **/
public class MsgHandleConst {

    //-------------------------消息类型---------------------------------

    /**
     * 心跳
     */
    public static final Integer HEARTBEAT = 0;

    /**
     * 命令
     */
    public static final Integer COMMAND = 1;

    /**
     *上报信息
     */
    public static final Integer REPORT_INFORMATION = 2;

    /**
     *告警信息
     */
    public static final Integer ALARM_INFORMATION = 3;

    /**
     * 命令执行结果
     */
    public static final Integer COMMAND_EXECUTION_RESULT = 100;

    /**
     * 回执
     */
    public static final Integer RECEIPT = 101;


    //********************消息类别全局ID***********************************
    /**
     * 默认
     */
    public static final String DEFAULT = "default";


    //----------------------上报信息
    /**
     * iids-hub-tias上报列车相关消息消息
     */
    public static final String TIAS_TRAIN_REPORTED = "2";

    /**
     * iids-hub-tias上报车门故障隔离站台门信数据
     */
    public static final String TRAIN_DOOR_INFO = "2-10006";

    /**
     * iids-hub-tias上报VOBC报警数据帧
     */
    public static final String VOBC_ARALM_DATA = "2-10007";

    /**
     * 推荐指令执行指令
     */
    public static final String ADMEXECUTE = "1-10101";
    /**
     * 人工告警执行指令
     */
    public static final String MANPOWER_ALARM = "10102";

    //---------------------------命令全局ID--------------------------------



    /**
     * 一键扣车/抬车命令
     */
    public static final String HOLD_TRAIN_COMMAND = "1-10001";

    /**
     * 调整计划运行图命令
     */
    public static final String ADJUST_PLAN_GRAPH_COMMAND = "1-10002";

    /**
     * 运行图版本号命令
     */
    public static final String PLAN_GRAPH_VERSION_COMMAND = "1-10003";

    /**
     * 运行图下发命令
     */
    public static final String CHANGE_TRAIN_GRAPH_COMMAND = "1-10005";

    /**
     * 初始化列车排班计划命令
     */
    public static final String INIT_DISPATCH_COMMAND = "1-10006";

    /**
     * 调整列车排班计划命令
     */
    public static final String ADJUST_DISPATCH_COMMAND = "1-10007";

    /**
     * 更新列车运行图
     */
    public static final String UPDATE_PLAN_GRAPH_COMMAND = "1-10008";

    /**
     * 初始化计划运行图命令
     */
    public static final String INIT_PLAN_GRAPH_COMMAND = "1-10009";

    /**
     * 更新列车排班表命令
     */
    public static final String UPDATE_DISPATCH_COMMAND = "1-10010";

    /**
     * 调整计划运行图命令 to hub
     */
    public static final String ADJUST_PLAN_GRAPH_COMMAND_HUB = "1-10011";

    /**
     * 列车自动指令 - 列车自动掉线
     */
    public static final String TRAIN_AUTO_OFFLINE = "1-10012";
    /**
     * 列车自动指令 - 备车自动出段
     */
    public static final String TRAIN_AUTO_OUTAREA = "1-10013";
    /**
     * 列车自动指令 - 列车自动停运
     */
    public static final String TRAIN_AUTO_STOPSERVICE = "1-10014";

    //---------------------命令执行结果ID---------------------------------

    /**
     * 消息回执统一处理
     */
    public static final String ACK_DATA = "101";

    /**
     * 初始化计划运行图执行结果
     */
    public static final String INIT_PLAN_GRAPH_RECEIPT = "100-100001";

    /**
     * 调整计划运行图执行结果
     */
    public static final String ADJUST_PLAN_GRAPH_RECEIPT = "100-100002";

    /**
     * 运行图版本号执行结果
     */
    public static final String PLAN_GRAPH_VERSION_RECEIPT = "100-100003";

    /**
     * 初始化列车排班计划执行结果
     */
    public static final String INIT_DISPATCH_RECEIPT = "100-100004";

    /**
     * 调整列车排班计划执行结果
     */
    public static final String ADJUST_DISPATCH_RECEIPT = "100-100005";

    /**
     * 更新计划运行图执行结果
     */
    public static final String UPDATE_PLAN_GRAPH_RECEIPT = "100-100006";

    /**
     * 更新列车排班表执行结果
     */
    public static final String UPDATE_DISPATCH_RECEIPT = "100-100007";

    /**
     * 获取应急分析报告命令
     */
    public static final String ANALYSIS_REPORT_COMMAND = "1-20001";

    /**
     * 获取应急分析报告命令回执
     */
    public static final String ANALYSIS_REPORT_RECEIPT = "101-20001";

    /**
     * 获取报警日志命令
     */
    public static final String ALARM_LOG_COMMAND = "1-20002";

//    /**
//     * 获取报警日志命令回执
//     */
//    public static final String ALARM_LOG_RECEIPT = "101-20002";

    /**
     * 获取报警日志命令执行结果
     */
    public static final String ALARM_LOG_RESULT = "100-20002";

    /**
     * 获取操作日志命令
     */
    public static final String OPERATE_LOG_COMMAND = "1-20003";

//    /**
//     * 获取操作日志命令回执
//     */
//    public static final String OPERATE_LOG_RECEIPT = "101-20003";

    /**
     * 获取操作日志命令执行结果
     */
    public static final String OPERATE_LOG_RESULT = "100-20003";

    /**
     * 获取调度日志列表命令
     */
    public static final String DISPATCH_LIST_LOG_COMMAND = "1-20004";

//    /**
//     * 获取调度日志列表命令回执
//     */
//    public static final String DISPATCH_LIST_LOG_RECEIPT = "101-20004";

    /**
     * 获取调度日志列表命令执行结果
     */
    public static final String DISPATCH_LIST_LOG_RESULT = "100-20004";

    /**
     * 获取调度命令
     */
    public static final String DISPATCH_LOG_COMMAND = "1-20005";

//    /**
//     * 获取调度命令回执
//     */
//    public static final String DISPATCH_LOG_RECEIPT = "101-20005";

    /**
     * 获取调度命令执行结果
     */
    public static final String DISPATCH_LOG_RESULT = "100-20005";
}
