package com.tct.itd.constant;

/**
 * @Description 列车相关常量
 * @Author zhangyinglong
 * @Date 2021/7/6 15:53
 */
public interface IidsSysParamPool {
    /**
     * 统一列车清客时间
     */
    String TRAIN_CLEAR_TIME = "minDebarkationTime";

    /**
     * 车门故障方案确认框倒计时
     */
    String STATION_CONFIRM_WID_TIME = "stationConfirmWidTIme";

    /**
     * 终点站缓行
     */
    String DOOR_FAILURE_CHANGE_GRAPH_TYPE = "doorFailureChangeGraphType";

    /*********************大客流*******************/
    String LARGE_PASS_FLOW_CLASS = "largePassFlow";//大客流类别
    String LARGE_PASS_FLOW_LEVEL1 = "1";//cctv客流量等级及阈值
    String LARGE_PASS_FLOW_LEVEL2 = "2";//cctv客流量等级及阈值
    String LARGE_PASS_FLOW_LEVEL3 = "3";//cctv客流量等级及阈值
    String FULL_LOAD_RATIO_TIME_THRESHOLD_MAX = "fullLoadRatioTimeThresholdMax";//列车满载率计算的数据时间范围(单位:ms)，20min
    String FULL_LOAD_RATIO_TIME_THRESHOLD_MIN = "fullLoadRatioTimeThresholdMin";//列车满载率计算的数据时间范围(单位:ms)，5min
    String FULL_LOAD_RATIO_THRESHOLD = "fullLoadRatioThreshold";//列车满载率触发大客流阈值
    String FULL_LOAD_RATIO_IN_OUT_DIFF = "fullLoadRatioInOutDiff";//列车进站和出站的触发大客流差值
    String FULL_LOAD_RATIO_COUNT_THRESHOLD = "fullLoadRatioCountThreshold";//触发大客流的次数阈值
    /*********************列车信息*******************/
    String TRAIN_INFO_CLASS = "trainInfo";//列车信息类别
    String TRAIN_LINE_ID = "lineId";//线路id
    String TRAIN_FIRST_WAIT_TIME = "firstWaitTime";//首发车延迟时长(秒)
    String UPDATE_GRAPH_TIME = "updateGraphTime";//每日运行图更新时间, 运营停运时间
    /*********************和利时日志推送开关（变量名）*******************/
    String FIRE_INFO_PUSH_SWITCH = "fireInfoPushSwitch";//和利时日志推送开关 0 关闭  1开启
    /*********************redis监控功能开关*******************/
    String REDIS_HEALTH_MONITOR_SWITCH = "redisHealthMonitorSwitch";//redis监控功能开关 0 关闭  1开启
    /*********************AS服务版本号*******************/
    String AS_VERSION = "asVersion";//AS服务版本号
    /*********************GATEWAY服务版本号*******************/
    String GATEWAY_VERSION = "gatewayVersion";//GATEWAY服务版本号

    /*********************站台门执行推荐指令后查看结果时长（分类名）*******************/
    //系统服务名称 分类名
    String PLATFORM_DOOR_WAIT_INFO = "platformDoorWaitInfo";
    /**
     * 整侧站台门无法打开执行第一次推荐指令后，检测远程打开站台门否成功
     */
    String ALL_PLATFORM_DOOR_WAIT_TIME = "allPlatformDoorWaitTime";
    /**
     * 站台门数量
     */
    String PLATFORM_DOOR_NUMBER = "PlatformDoorNumber";
    //通风机故障判定数量
    String VENTILATOR_ALARM_NUMBER = "ventilatorAlarmNumber";
    //列车故障恢复晚点时间参数
    String TRAIN_LATE_TIME = "trainLateTime";
    /*********************和利时综合监控主备状态*******************/
    String HLS_MONITOR_STATUS = "hlsMonitorStatus";
    /*********************道岔故障时间参数*******************/
    String SWITCH_FAILURE_TIME = "switchFailureTime";
    /*********************道岔第一次推荐指令执行至第二次推荐指令弹出时间间隔*******************/
    String SWITCH_FAILURE_NEXT_AID_TIME = "switchFailureNextAidTime";
    /*********************道岔故障(具备站前折返)站前折返图时间*******************/
    String SWITCH_FAILURE_HAS_BACK_TIME = "switchFailureHasBackTime";
    /*********************道岔故障30min运行图预览时间参数*******************/
    String SWITCH_FAILURE_PRRVIEW_TIME = "switchFailurePreviewTime";
    /*********************道岔故障60min运行图预览时间参数*******************/
    String SWITCH_FAILURE_THIRTY_PRRVIEW_TIME = "switchFailureThirtyPreviewTime";
    /****列车晚点调图时间范围****/
    String OTP_TIME = "otpTime";

    /***录入车次号规则校验***/
    String TRAIN_NUM_CHECK = "trainNumCheck";
    /*********************道岔故障20分钟检测道岔故障是否恢复时间*******************/
    String SWITCH_FAILURE_REPEAT_TEN_TIME = "switchFailureRepeatTenTime";
    /*********************道岔故障50分钟检测道岔故障是否恢复时间*******************/
    String SWITCH_FAILURE_REPEAT_TWENTY_TIME = "switchFailureRepeatTwentyTime";
    /**
     * 车门故障15分钟定时推送调图方案
     **/
    String DOOR_FAILURE_THIRTY_PREVIEW_TIME = "doorFailureThirtyPreviewTime";

    /**
     * 运行图预览弹窗倒计时
     **/
    String PREVIEW_RUN_GRAPH_TIME = "previewRunGraphTime";

    /**
     * 推荐指令弹窗倒计时
     **/
    String COUNT_DOWN = "countdown";

    /**
     * 车门故障10分钟触发15分钟调图方案
     **/
    String DOOR_FAILURE_REPEAT_TIME = "doorFailureRepeatTime";

    /**
     * 调图冗余参数; 默认一分钟
     **/
    String SET_REDUNDANCY_TIME = "setRedundancyTime";
    /**
     * 校验参数配置
     */
    // String TRAIN_NUM_LENGTH = "trainNumLength";
    // String SERVER_NUM_LENGTH = "serverNumLength";
    /**
     * 接触网失电参数
     */
    String TRACTION_POWER = "tractionPower";
    /**
     * 循环时间间隔
     */
    String TRACTION_POWER_PREVIEW_TIME = "tractionPowerPreviewTime";
    /**
     * 循环时间间隔后检查时间间隔
     */
    String TRACTION_POWER_CHECK_TIME = "tractionPowerCheckTime";

    /**
     * 联锁双机参数
     */
    String INTER_LOCK = "interLock";
    /**
     * 循环时间间隔
     */
    String INTER_LOCK_PREVIEW_TIME = "interLockPreviewTime";
    /**
     * 循环时间间隔后检查时间间隔
     */
    String INTER_LOCK_CHECK_TIME = "interLockCheckTime";
    /**
     * 循环时间间隔
     */
    String SIGNAL_ELECTRIC_PREVIEW_TIME = "signalElectricPreviewTime";
    /**
     * 循环时间间隔后检查时间间隔
     */
    String SIGNAL_ELECTRIC_CHECK_TIME = "signalElectricCheckTime";

    /**
     * 设置计划车延时时间
     */
    String SET_PLAN_DELAY_TIME = "setPlanDelayTime";
    /**
     * 导出应急事件列表，excel最大条数
     */
    String ALARM_INFO_EXPORT_MAX = "alarmInfoExportMax";
    /**
     * 后台应用和算法应用是否与网关应用建立长连接
     */
    String GATEWAY_CONNECT = "gatewayConnect";
    /**
     * 计轴故障调缓行图时间范围
     */
    String AXLE_COUNTER_SLOWLY_TIME = "axleCounterSlowlyTime";
    /**
     * 计轴故障推送缓行图预览间隔
     */
    String AXLE_COUNTER_SLOWLY_CIRCLE = "axleCounterSlowlyCircle";
    /**
     * 计轴故障自动上报延时时间
     **/
    String AXLE_COUNTER_DELAY = "axleCounterDelay";
    /**
     * 和利时用户名参数
     **/
    String HLS_USER_NAME = "hlsUserName";
    /**
     * 和利时密码参数
     **/
    String HLS_PASSWORD = "hlsPassword";

    /**
     * 道岔故障恢复预估时间
     */
    String SWITCH_FAILURE_RECOVERY_TIME = "switchFailureRecoveryTime";

    /**
     * 道岔故障恢复定反位次数
     */
    String SWITCH_RECOVERY_COUNT = "switchRecoveryCount";

    /**
     * 道岔故障未恢复定反位次数
     */
    String SWITCH_NOT_RECOVERY_COUNT = "switchNotRecoveryCount";

    /**
     * 进出站站台门故障-上报校验次数
     */
    String IN_OUT_PLATFORM_ALARM_CHECK_NUM = "inOutPlatFormAlarmCheckNum";

}
