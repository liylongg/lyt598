package com.tct.itd.common.cache;

public class Cache {

    public static Long alarmInfoId = 0L;

    /**************TAIS协议相关***************/

    /**
     * 下发命令发送序号 最大四个字节
     */
    public static final String SEQUENCE_NUM = "SEQUENCE_NUM";

    /**
     * 命令序号 最大两个字节
     */
    public static final String ORDER_NUM = "ORDER_NUM";

    /**************电子地图相关***************/


    /**
     * 站台所在站台编号id 与站台名称映射
     */
    public static final String PLATFORM_ID_TO_PLATFORM_NAME = "PLATFORM_ID_TO_PLATFORM_NAME";
    /**
     * 上行站台stationId 映射 platformId
     */
    public static final String UP_STATIONID_TO_PLATFORMID = "UP_STATIONID_TO_PLATFORMID";
    /**
     * 下行stationId 映射 platformId
     */
    public static final String DOWN_STATIONID_TO_PLATFORMID = "DOWN_STATIONID_TO_PLATFORMID";

    /**************业务逻辑相关***************/
    public static final String RESULTPREFIX = "TIASCOMMANDRESULT:";

    /**
     * 扣车业务 需要扣车的站台
     */
    public static final String HOLD_TRAIN_STATION = "HOLD_TRAIN_STATION";

    /**
     * 扣车指令
     */
    public static final String ON_HOLD_TRAIN = "ON_HOLD_TRAIN";


    /**
     * 抬车指令
     */
    public static final String OFF_HOLD_TRAIN = "OFF_HOLD_TRAIN";


    /**
     * 运行图指令命令回执 push
     */
    public static final String ADJUST_PLAN_GRAPH_COMMAND_RESULT = RESULTPREFIX + "ADJUST_PLAN_GRAPH_COMMAND_RESULT:";

    /**
     * 运行图调整加开/替开 key:车次号 val:PlanGraphInfoAndTrainAdjust 对象
     */
    public static final String TRAIN_NUMBER_ADJUST = "TRAIN_NUMBER_ADJUST";


    /**
     * 运行图调整加开/替开 key:上一车次号 val:PlanGraphInfoAndTrainAdjust 对象 与实际运行图数据匹配
     */
    public static final String TRAIN_GRAPH_TRAIN = "TRAIN_GRAPH_TRAIN";


    /**
     * 命令应答处理数据更新
     */
    public static final String COMMAND_TRAIN_CTRL = "COMMAND_TRAIN_CTRL";

    /**
     * 命令应答处理数据更新
     */
    public static final String COMMAND_RECEIVED = "COMMAND_RECEIVED";

    /**
     * 运行图调整回执map
     */
    public static final String CHANGE_TRAIN_GRAPH = "CHANGE_TRAIN_GRAPH";

    /**
     * 设置计划车回执map
     */
    public static final String SET_PLAN_TRAIN = "SET_PLAN_TRAIN";

    /**
     * 列车自动清客指令回执
     */
    public static final String TRAIN_CLEAR_COMMAND = "TRAIN_CLEAR_COMMAND";

    /**
     * 车次FAO信息
     */
    public static final String TRAIN_FAO_INFO = "TRAIN_FAO_INFO";

    /**
     * 推荐指令 等待回复超时 3分钟
     */
    public static final String ALARM_CLOSE_DOOR_TIME_OUT = "ALARM_CLOSE_DOOR_TIME_OUT";

    /**
     * 推荐指令已经执行 等待恢复
     */
    public static final String ADM_IDEA_EXECUTE = "ADM_IDEA_EXECUTE";

    /**
     * 算法-受影响的车次列表
     */
    public static final String ADM_AFFECTED_TRAIN = "ADM_AFFECTED_TRAIN";
    /**
     * 算法-扣车与抬车时间缓存
     */
    public static final String HOLD_AND_OFF_TRAIN_TIME = "HOLD_AND_OFF_TRAIN_TIME";

    /**
     * 大客流推荐指令 等待执行 60秒超时
     */
    public static final String ALL_DOOR_CONNOT_OPEN_CLEAR_GUEST = "ALL_DOOR_CONNOT_OPEN_CLEAR_GUEST";

    /**
     * 车站客流数据，触发大客流间隔缓存
     */
    public static final String STATION_PASSENGER_STAT_CACHE = "STATION_PASSENGER_STAT_CACHE:";
    /**
     * 满载率，TRAIN_FULL_LOAD_RATIO_CACHE:车站id:上下行
     */
    public static final String TRAIN_FULL_LOAD_RATIO_CACHE = "TRAIN_FULL_LOAD_RATIO_CACHE:%s:%s";

    /**
     * 大客流场景-加车回库时间  :车次号
     */
    public static final String LARGE_PASS_FLOW_BACK_TIME_CACHE = "LARGE_PASS_FLOW_BACK_TIME_CACHE:%s";

    /**
     * ATS命令号重发次数
     */
    public static final String RESEND_COMMAND_COUNT = "RESEND_COMMAND_COUNT";

    /**
     * 存入车次停车区段信息
     */
    public static final String TRAIN_TRACE_SECTION = "TRAIN_TRACE_SECTION";

    /**
     * 调度命令与故障信息关联
     */
    public static final String CMD_ASSOCIATE_ALARM_INFO = "CMD_ASSOCIATE_ALARM_INFO";

    /**************业务逻辑相关--故障自动上报***************/

    /**
     * 命令号缓存key
     */
    public static final String DISPATCH_COMMAND_CODE = "Dispatch_Command_Code";

    /**
     * 定时任务故障恢复调图逻辑正在执行
     */
    public static final String ADJUST_GRAPH_RECOVERY_TIMER_CHECK = "ADJUST_GRAPH_RECOVERY_TIMER_CHECK";
    /**
     * 站场同步数据扣车标识 0:无扣车 1:有扣车
     */
    public static final String SYNC_HOLD_TRAIN = "SYNC_HOLD_TRAIN";

    /**
     * 站场同步数据清客标识 0:未设置清客 1:设置清客
     */
    public static final String SYNC_CLEAR_STATUS = "SYNC_CLEAR_STATUS";

    /**
     * 站场同步数据立即发车标识 0:未设置立即发车 1:设置立即发车
     */
    public static final String SYNC_DRIVE_IMMEDIATELY = "SYNC_DRIVE_IMMEDIATELY";

    /**
     * 站场同步数据人工设置停站时间 -1：无人工设置停站时间 >0：设置的人工时间；
     */
    public static final String SYNC_STOP_TIME = "SYNC_STOP_TIME";

    /**
     * hub消息去重布隆过滤器
     */
    public static final String ATS_PACKET_REPORT_BLOOM_FILTER = "ats_packet_report_bloom_filter";
    /**
     * http请求序号
     */
    public static final String HTTP_REQUEST_UUID = "HTTP_REQUEST_UUID_SERVICE";

    /**
     * 智能调度黄紫网网段
     */
    public static final String IIDS_IP_SEGMENT = "IIDS_IP_SEGMENT";

    /**
     * 车门故障无法打开自动上报
     */
    public static final String TRAIN_DOOR_ALARM_NOT_OPEN = "TRAIN_DOOR_ALARM_NOT_OPEN";

    /**
     * 封入请求头序号参数key
     */
    public static final String HTTP_REQUEST_UUID_KEY = "requestuuid_service";

    /**
     * 记录牵引区间故障列车运行至站台的标志
     */
    public static final String TRAIN_TRACE_ARRIVE_PLATFORM = "TRAIN_TRACE_ARRIVE_PLATFORM";


    /**
     * 人工设置停站时间是否开始进行车门故障检测
     */
    public static final String SYNC_STOP_TIME_SIGN = "SYNC_STOP_TIME_SIGN";

    /**
     * ATS主备中心标志
     */
    public static final String ACTIVE_STANDBY = "ACTIVE_STANDBY";

    /**
     * 和利时综合监控前端主备状态 "0":主 "1":备
     */
    public static final String HLS_MONITOR_STATUS = "HLS_MONITOR_STATUS";

    /**
     * 道岔故障,缓行图故障未恢复继续调图标识
     */
    public static final String SWITCH_TWENTY_PREVIEW_SIGN = "SWITCH_TWENTY_PREVIEW_SIGN";

    /**
     * 道岔故障,中断图故障未恢复继续调图标识
     */
    public static final String SWITCH_TEN_PREVIEW_SIGN = "SWITCH_TEN_PREVIEW_SIGN";

    /**
     * 道岔故障,中断图循环调图开始时间
     */
    public static final String SWITCH_FAILURE_TEN_TIME = "SWITCH_FAILURE_TEN_TIME";

    /**
     * 道岔故障,缓行图循环调图开始时间
     */
    public static final String SWITCH_FAILURE_TWENTY_TIME = "SWITCH_FAILURE_TWENTY_TIME";

    /**
     * 接触网失电推图时间记录
     */
    public static final String TRAIN_POWER_PREVIEW_TIME_RECORD = "TRAIN_POWER_PREVIEW_TIME_RECORD";
    /**
     * 联锁推图时间记录
     */
    public static final String INNER_LOCK_PREVIEW_TIME_RECORD = "INNER_LOCK_PREVIEW_TIME_RECORD";
    /**
     * 信号电源推图时间记录
     */
    public static final String SINGAL_ELECTRIC_PREVIEW_TIME_RECORD = "SINGAL_ELECTRIC_PREVIEW_TIME_RECORD";


    /**
     * 运行图
     */
    public static final String PLAN_GRAPH = "PLAN_GRAPH";

    /**
     * 道岔故障推送第三次推荐指令标记
     */
    // public static final String SF_HAS_PRE_THREE_AID = "SF_HAS_PRE_THREE_AID_SIGN";

    /**
     * 终端道岔故障具备本站折返,第一个折返车次号
     */
    public static final String BACK_TRAIN_NUMBER = "BACK_TRAIN_NUMBER";

    /**
     * 终端道岔故障具备本站折返,恢复正常折返车次
     */
    public static final String RECOVERY_TRAIN_NUMBER = "RECOVERY_TRAIN_NUMBER";

    /**
     * 道岔故障运行图方案执行,运行图预览执行标识
     */
    public static final String SWITCH_FAILURE_PREVIEW_SIGN = "SWITCH_FAILURE_PREVIEW_SIGN";

    /**
     * 终端站折返道岔故障-具备本站折返故障时
     * 停在区间的车需要运行至下一站的车次号
     */
    public static final String TRAIN_NUMBER_LIST = "TRAIN_NUMBER_LIST";

    /**
     * 终端站折返道岔故障-具备本站折返故障时
     * 停在区间的车需要运行至下一站的停车区域id
     */
    public static final String NEXT_STOP_REGION_ID = "NEXT_STOP_REGION_ID";

    /**
     * 接触网失电循环已预览次数
     */
    public static final String TRACTION_POWER_NUM = "TRACTION_POWER_NUM_%s";
    /**
     * 联锁双机循环已预览次数
     */
    public static final String INTER_LOCK_NUM = "INTER_LOCK_NUM_%s";
    /**
     * 信号电源循环已预览次数
     */
    public static final String SIGNAL_ELECTRIC_NUM = "SIGNAL_ELECTRIC_NUM_%s";

    /**
     * 转换控制权命令
     */
    public static final String CTRL_SWITCH_COMMAND = "CTRL_SWITCH_COMMAND";
    /**
     * 计轴故障放弃推荐指令缓存计轴命令
     */
    public static final String WAIVE_REPORT_AXLE_COUNTER = "WAIVE_AXLE";
    /**
     * 已经上报过的计轴故障
     */
    public static final String ALREADY_REPORT_AXLE_COUNTER = "ALREADY_REPORT_AXLE_COUNTER";

    /**
     * 计轴扣车标志
     */
    public static final String AXLE_COUNTER_HOLD_TRAIN = "AXLE_COUNTER_HOLD_TRAIN";
    /**
     * 计轴弹窗标志
     */
    public static final String AXLE_COUNTER_CONFIRM = "AXLE_COUNTER_CONFIRM";
    /**
     * 已经上报过站台门故障的站台门id缓存key
     */
    public static final String ALREADY_REPORT_PLATFORM_DOOR = "ALREADY_REPORT_PLATFORM_DOOR";
    /**
     * 已经上报过进出站站台门故障的站台门id缓存key
     */
    public static final String ALREADY_REPORT_PLATFORM_DOOR_IN_OUT = "ALREADY_REPORT_PLATFORM_DOOR_IN_OUT";

    /**
     * 已经上报过联锁故障
     */
    public static final String ALREADY_REPORT_INNER_LOCK = "ALREADY_REPORT_INNER_LOCK";

    /**
     * 已经上报过接触网失电故障
     */
    public static final String ALREADY_REPORT_TRACTION_POWER = "ALREADY_REPORT_TRACTION_POWER";


    /**
     * 新增的弹窗确认缓存
     */
    public static final String POP_CONFIRM = "POP_CONFIRM_%s";

    /**
     * 缓行标识
     */
    public static final String SLOWLY_SIGN = "SLOWLY_SIGN";

    /**
     * 已经上报道岔故障Key
     */
    public static final String ALREADY_REPORT_SWITCH_FAILURE = "ALREADY_REPORT_SWITCH_FAILURE";

    /**
     * 道岔故障故障恢复Key
     */
    public static final String RECOVERY_SWITCH_FAILURE = "RECOVERY_SWITCH_FAILURE";

    /**
     * 计轴调图标志
     */
    public static final String AXLE_COUNTER_ADJUST_GRAPH = "AXLE_COUNTER_ADJUST_GRAPH";
    /**
     * 计轴设置计划车标志
     */
    public static final String AXLE_COUNTER_SET_PLAN = "AXLE_COUNTER_SET_PLAN";
    /**
     * 计轴调图标志
     */
    public static final String AXLE_COUNTER_SLOWLY_GRAPH_SIGN = "AXLE_COUNTER_SLOWLY_GRAPH_SIGN";

    /**
     * 计轴缓行开始时间
     */
    public static final String AXLE_COUNTER_SLOWLY_START = "AXLE_COUNTER_SLOWLY_START";
    /**
     * hub数据源所有数据库对应key
     */
    public static final String HUB_DATASOURCE_KEY = "HUB_DATASOURCE_KEY";
    /**
     * 算法返回计轴故障类型
     */
    public static final String AXLE_FAILURE_ALG = "AXLE_FAILURE_ALG";

    /**
     * 故障生命周期
     */
    public static final String END_LIFE = "END_LIFE";

    /**
     * 联锁故障发生时区段所有列车
     */
    public static final String INTER_LOCK_TRAIN_IN_SECTION = "INTER_LOCK_TRAIN_%s";

    /**
     * 默认key
     */
    public static final String DEFAULT = "default";

    /**
     * 列车时刻表(根据运行图计算列车时刻表)
     */
    public static final String STATION_TIME_CACHE = "station_time_cache";

    /**
     * 信号电源故障发生时区段所有列车
     */
    public static final String SIGNAL_ELECTRIC_TRAIN_IN_SECTION = "SIGNAL_ELECTRIC_TRAIN_%s";
    /**
     * 扣车与抬车时间缓存
     */
    public static final String HOLD_AND_OFF_TRAIN = "HOLD_AND_OFF_TRAIN";

    /**
     * 车门已经上报故障常量
     */
    public static final String ALREADY_REPORT_TRAIN_DOOR = "ALREADY_REPORT_TRAIN_DOOR";

    /**
     * 车门故障上报VOBC报警状态
     */
    public static final String TRAIN_DOOR_VOBC_STATE = "TRAIN_DOOR_VOBC_STATE";

    /**
     * 车门故障自动检测恢复标志
     */
    public static final String TRAIN_DOOR_AUTO_CHECK_RECOVERY = "TRAIN_DOOR_AUTO_CHECK_RECOVERY";

    /**
     * 空调故障上报标志
     */
    public static final String ALREADY_REPORT_AIR_CONDITION = "ALREADY_REPORT_AIR_CONDITION";

    /**
     * 存储故障恢复时间
     */
    public static final String DOOR_TIME_ADJUST_CASE_FLAG = "DOOR_TIME_ADJUST_CASE_FLAG";

    /**
     * 调表调令自动下发定时任务
     */
    public static final String ADJUST_SERVER_SEND_CMD_TIMER = "adjustServerSendCmdTimer";

    /**
     * 根据故障停车区域-返回扣车站台信息
     */
    public static final String HOLD_TRAIN_STOP_AREA = "HOLD_TRAIN_STOP_AREA";

    /**
     * 清客结束标志
     */
    public static final String CLEAR_PEOPLE_IS_FINISH = "CLEAR_PEOPLE_IS_FINISH";

    /**
     * 开始缓行车次号
     */
    public static final String SLOWLY_TRAIN_NUMBER = "SLOWLY_TRAIN_NUMBER";

    /**
     * 缓行车次缓行至站台ID
     */
    public static final String SLOWLY_NEXT_STOP_REGIONID = "SLOWLY_NEXT_STOP_REGIONID";

    /**
     * 缓行车次缓行至站台名称
     */
    public static final String SLOWLY_NEXT_STATION_NAME = "SLOWLY_NEXT_STATION_NAME";

    /**
     * 缓行/中断到恢复车次号
     */
    public static final String SLOWLY_RECOVERY_TRAIN_NUMBER = "SLOWLY_RECOVERY_TRAIN_NUMBER";
    /**
     * 实际运行图数据
     */
    public static final String ACTUAL_GRAPH_DATA = "ACTUAL_GRAPH_DATA";

    /**
     * 客户端长连接应答标识
     */
    public static final String CLIENT_ANSWER = "CLIENT_ANSWER";

    /**
     * 车门大小交路对应站台集合
     */
    public static final String PLATFORM_STOP_AREA_LIST = "PLATFORM_STOP_AREA_LIST";

    /**
     * 故障升级标志
     */
    public static final String ALARM_UPGRADE = "ALARM_UPGRADE";
    /**
     * 系统参数
     */
    public static final String SYSTEM_PARAM = "SYSTEM_PARAM";
    /**
     * 电子地图数据
     */
    public static final String FS_DATA = "FS_DATA:";
    /**
     * 折返点扣车标志，这个时候不抬车
     */
    public static final String CHANGE_GRAPH_CANCEL_HOLD_TRAIN_FLAG = "CHANGE_GRAPH_CANCEL_HOLD_TRAIN_FLAG";

    /**
     * 扣车标识
     */
    public static final String HOLD_TRAIN_FLAG = "HOLD_TRAIN_FLAG";

    /**
     * ATS命令未回执重发
     */
    public static final String RESEND_ATS_COMMAND = "RESEND_ATS_COMMAND";

    /**
     * 计轴预复位次数
     */
    public static final String AXIS_RESET_COUNT = "AXIS_RESET_COUNT";
    /**
     * 计轴预复位成功取消扣车
     */
    public static final String AXIS_RESET_SUCCESS = "AXIS_RESET_SUCCESS";
    /**
     * 站台门已经推出互锁解除信息
     */
    public static final String ALREADY_PUSH_PLATFORM_DOOR = "ALREADY_PUSH_PLATFORM_DOOR";
    /**
     * 正在清客状态标志
     */
    public static final String CLEAR_STATUS_ING = "CLEAR_STATUS_ING";

    /**
     * 数据库切换提示
     */
    public static final String DATASOURCE_ACTUATOR = "DATASOURCE_ACTUATOR";

    /**
     * 道岔调中断图时,返回跑小交路的停车区域id
     */
    public static final String SWITCH_FAILURE_STOP_AREA_LIST = "SWITCH_FAILURE_STOP_AREA_LIST";

    /**
     * 是否添加统计指标
     */
    public static final String AFTER_CHANGE_GRAPH_STATISTICS = "AFTER_CHANGE_GRAPH_STATISTICS";

    /**
     * 道岔故障第一次推荐指令执行后故障恢复,是否需要调图
     */
    public static final String SWITCH_FAILURE_RECOVERY_FIRST_CHANGE_GRAPH = "SWITCH_FAILURE_RECOVERY_FIRST_CHANGE_GRAPH";

    /**
     * 第一次推荐指令执行后，判断列车是否晚点2分钟
     */
    public static final String TWO_MINUTES_LATE_RECOVERY_CHANGE_GRAPH = "TWO_MINUTES_LATE_RECOVERY_CHANGE_GRAPH";

    /**
     * 道岔故障循环推图标识
     */
    public static final String SWITCH_FAILURE_ADJUST_LIST_SING = "SWITCH_FAILURE_ADJUST_LIST_SING";


    /**
     * 应急事件流程图数据
     */
    public static final String FLOWCHART_DATA = "FLOWCHART_DATA";

    /**
     * AS_GATEWAY心跳
     */
    public static final String AS_GATEWAY_HEART = "AS_GATEWAY_HEART:";

    /**
     * AS_GATEWAY心跳Timer标识
     */
    public static final String AS_GATEWAY_TIMER_FLAG = "AS_GATEWAY_TIMER_FLAG";

    /**
     * GATEWAY心跳
     */
    public static final String GATEWAY_HEART = "GATEWAY_HEART:";

    /**
     * AS_GATEWAY心跳Timer标识
     */
    public static final String GATEWAY_HEART_FLAG = "GATEWAY_HEART_FLAG";

    /**
     * 计轴故障延时标志
     */
    public static final String DELAY_AXLE_COUNTER = "DELAY_AXLE_COUNTER";
    /**
     * 车次号位数
     */
    public static final String ORDER_NUMBER_DIGIT = "ORDER_NUMBER_DIGIT";
    /**
     * 表号位数
     */
    public static final String SERVER_NUMBER_DIGIT = "SERVER_NUMBER_DIGIT";

    /**
     * 算法调图时间差,预览图和调图故障结束时间一致
     */
    public static final String CHANGE_GRAPH_TIME_DIFFERENCE = "CHANGE_GRAPH_TIME_DIFFERENCE";

    /**
     * 调度命令模板内容
     */
    public static final String CMD_TEMPLATE_XML = "CMD_TEMPLATE_XML";

    /**
     * 故障类型模板内容
     */
    public static final String ALARM_TYPE_XML = "ALARM_TYPE_XML";

    /**
     * 流程图状态
     */
    public static final String FLOWCHART_FLAG = "FLOWCHART_FLAG";

    /**
     *
     */
    public static final String AID_DES_STEP = "AID_DES_STEP";
    /**
     * 调图标志-流程图
     */
    public static final String CHANGE_GRAPH_FLAG = "CHANGE_GRAPH_FLAG";
    /**
     * 车站确认标志-流程图
     */
    public static final String STATION_CONFIRM_FLAG = "STATION_CONFIRM_FLAG";
    /**
     * 计轴预复位成功-流程图
     */
    public static final String PRE_SUCCESS_FLAG = "PRE_SUCCESS_FLAG";
    /**
     * 计轴预复位失败-流程图
     */
    public static final String PRE_FAIL_FLAG = "PRE_FAIL_FLAG";

    /**
     * 是否推送试送电标志
     */
    public static final String TRIAL_LINE_CHARGING_FLAG = "TRIAL_LINE_CHARGING_FLAG";

    /**
     * 道岔故障执行第一次推荐指令时间
     */
    public static final String SWITCH_FAILURE_EXECUTE_ONE_TIME = "SWITCH_FAILURE_EXECUTE_ONE_TIME";

    /**
     * 应用服务与网关长连接建联失败ip
     */
    public static final String AS_CONNECT_FAILED = "AS_CONNECT_FAILED";

    /**
     * 网关服务与网关长连接建联失败ip
     */
    public static final String GATEWAY_CONNECT_FAILED = "GATEWAY_CONNECT_FAILED";

    /**
     * 系统主从状态常量
     */
    public static final String MASTER_SLAVE_STATUS = "MASTER_SLAVE_STATUS";

    /**
     * 系统主从状态信息
     */
    public static final String MASTER_SLAVE_INFO = "MASTER_SLAVE_INFO";

    /**
     * AS-GATEWAY连接
     */
    public static final String GATEWAY_AS_CONNECTION = "GATEWAY_AS_CONNECTION";

    /**
     * AS-GATEWAY连接标识
     */
    public static final String GATEWAY_AS_CONNECTION_FLAG = "GATEWAY_AS_CONNECTION_FLAG";

    /**
     * ALG-GATEWAY连接
     */
    public static final String GATEWAY_ALG_CONNECTION = "GATEWAY_ALG_CONNECTION";

    /**
     * ALG-GATEWAY连接标识
     */
    public static final String GATEWAY_ALG_CONNECTION_FALG = "GATEWAY_ALG_CONNECTION_FALG";

    /**
     * 存放从服务黄紫网ip
     */
    public static final String SLAVE_IP = "SLAVE_IP";

    /**
     * 存放本服务黄紫网ip
     */
    public static final String LOCAL_IP = "LOCAL_IP";

    /**
     * 存另一台服务黄紫网ip
     */
    public static final String OTHER_IP = "OTHER_IP";

    /**
     * 是否自动切换主备
     */
    public static final String AUTO_SWITCH_MASTER = "AUTO_SWITCH_MASTER";

    /**
     * 手动切换到A还是B
     */
    public static final String MANUAL_SWITCH_MASTER = "MANUAL_SWITCH_MASTER";

    /**
     * 同步主服务缓存标识
     */
    public static final String SYNC_CACHE_FLAG = "SYNC_CACHE_FLAG";

    /**
     *  接触网失电弹框标志
     */
    public static final String TRACTION_POWER_POP_FLAG = "TRACTION_POWER_POP_FLAG";

    /**
     *  离线客户端未收到的推送消息
     */
    public static final String OFFLINE_CLIENT_MSG = "OFFLINE_CLIENT_MSG";

    /**
     *  tcmsGatwewayCache缓存key
     */
    public static final String TCMS_GATEWAY_CACHE = "TCMS_GATEWAY_CACHE";

    /**
     * 封入请求头序号参数key
     */
    public static final String HTTP_REQUEST_TYPE = "HTTP_REQUEST_TYPE";
    /**
     * header放token
     */
    public static final String HTTP_REQUEST_HEADER_TOKEN = "HTTP_REQUEST_HEADER_TOKEN";

    /**
     * 设备拓扑关系
     */
    public static final String TOPOLOGY_RELATION = "TOPOLOGY_RELATION";
    /**
     * 车门屏蔽门故障报警
     */
    public static final String TRAIN_PLATFORM_DOOR_ALARM = "TRAIN_PLATFORM_DOOR_ALARM";
    /**
     * 车站弹窗缓存
     */
    public static final String STATION_CONFIRM_IDS= "STATION_CONFIRM_IDS";

    /**
     * 道岔现地单操_定位
     */
    public static final String SWITCH_FIXED = "SWITCH_FIXED";

    /**
     * 道岔现地单操_反位
     */
    public static final String SWITCH_INVERSE = "SWITCH_INVERSE";

    /**
     * 道岔车站单操超过次数标记
     */
    public static final String SWITCH_OPERATE_SIGN = "SWITCH_OPERATE_SIGN";

    /**
     * 监测道岔操作故障恢复标识
     */
    public static final String SWITCH_OPERATE_RECOVERY_FLAG = "SWITCH_OPERATE_RECOVERY_FLAG";

    /**
     * 道岔单操故障恢复
     */
    public static final String SWITCH_OPERATE_RECOVERY = "SWITCH_OPERATE_RECOVERY";

    /**
     * 道岔放弃应急事件
     */
    public static final String SWITCH_GIVE_UP_SIGN = "SWITCH_GIVE_UP_SIGN";

    /**
     * 存储列车紧急制动状态
     */
    public static final String TRAIN_EBI_STATUS_CACHE = "TRAIN_EBI_STATUS_CACHE";


    /**
     * 监测道岔现地单操_定位
     */
    public static final String MONITOR_SWITCH_FIXED = "MONITOR_SWITCH_FIXED";

    /**
     * 监测道岔现地单操_反位
     */
    public static final String MONITOR_SWITCH_INVERSE = "MONITOR_SWITCH_INVERSE";

    /**
     * 网关重连失败
     */
    public static final String GATEWAY_RECONNECT_TIME = "GATEWAY_RECONNECT_TIME";

    /**
     * 网关长连接发送心跳失败次数
     */
    public static final String GATEWAY_HEART_BEAT_COUNT = "GATEWAY_HEART_BEAT_COUNT";

    /**
     * 应用服务(AS)长连接发送心跳失败次数
     */
    public static final String AS_HEART_BEAT_FAILED_COUNT = "AS_HEART_BEAT_FAILED_COUNT";

}
