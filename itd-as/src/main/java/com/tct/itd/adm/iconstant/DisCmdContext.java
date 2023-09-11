package com.tct.itd.adm.iconstant;

/**
 * @ClassName DisCmdContext
 * @Description 自动发送电子调度命令常量
 * @Author YHF
 * @Date 2021/8/18 16:45
 */
public class DisCmdContext {

    /**
     * 单车门故障-第一次推荐指令
     * 推荐指令步骤 2.电子调度命令通知车辆段给备车派班
     * 空调故障推荐指令
     * 推荐指令步骤 2.电子调度命令通知车辆段给备车派班
     * 列车空调无通风推荐指令
     * 推荐指令步骤 2.电子调度命令通知车辆段/停车场DCC准备派班派备车上线;
     * 广播故障 人工广播可用
     * 推荐指令步骤 2.电子调度命令通知车辆段/停车场DCC准备派班派备车上线;
     * 广播故障 人工广播不可用
     * 推荐指令步骤 2.电子调度命令通知车辆段/停车场DCC准备派班派备车上线;
     */
    public static final String SPARE_TRAIN_SET_ONLINE = "准备派班派备车上线";
    /**
     * 牵引故障--生成第二次推荐指令
     * 推荐指令步骤 2.自动发送电子调度命令通知DCC更新派班计划安排备车上线
     * 第二次推荐指令指令-多车门故障产生（连续/非连续，无法打开/关闭）
     * 推荐指令步骤 2.自动发送电子调度命令通知DCC更新派班计划安排备车上线;
     * 大客流之断面客流
     * 推荐指令步骤 2.自动下发电子调度命令通知DCC更新派班计划安排备车上线;
     * 第二次推荐指令指令--全列车门无法关闭故障产生
     * 推荐指令步骤 3.系统自动下发电子调度命令，通知DCC更新派班计划安排备车上线;
     * 第三次推荐指令指令---全列车门无法关闭故障产生
     * 推荐指令步骤 3.系统自动下发电子调度命令至DCC，更新派班计划安排备车上线;
     * 第二次推荐指令指令---全列车门无法打开故障产生 一分钟清人方案
     * 推荐指令步骤 3.系统自动下发电子调度命令，通知DCC更新派班计划安排备车上线;
     * 第三次推荐指令指令---全列车门无法打开故障产生 四分钟清人方案
     * 推荐指令步骤 3.系统自动下发电子调度命令至DCC，更新派班计划安排备车上线;
     */
    public static final String UPDATE_SPARE_TRAIN_SET_ONLINE = "更新派班计划安排备车上线";
    /**
     * 单车门故障-第一次推荐指令
     * 推荐指令步骤 4.下达电子调度命令给车站值班员人工处理车门故障
     */
    public static final String CHECK_THE_ALARM_DOOR = "车站值班员人工处理车门故障";
    /**
     * 空调故障推荐指令
     * 推荐指令步骤 3.电子调度命令通知终点站列车掉线，并且给下线列车分回库头码;
     * 广播故障 人工广播可用
     * 推荐指令步骤 3.电子调度命令通知终点站列车掉线，并且给下线列车分回库头码;
     */
    public static final String TRAIN_OFF_LINE = "终点站列车掉线，并且给下线列车分回库头码";
    /**
     * 空调无通风推荐指令
     * 推荐指令步骤 3.以电子调度命令形式通知做好清客准备;
     * 广播故障 人工广播不可用
     * 推荐指令步骤 3.以电子调度命令形式通知做好清客准备;
     * 牵引故障--生成第二次推荐指令
     * 推荐指令步骤 3.以电子调度命令形式通知故障车站做好清客准备;
     * 第二次推荐指令指令--全列车门无法关闭故障产生
     * 推荐指令步骤 2.系统自动下发电子调度命令，通知车站配合清人作业;
     * 第三次推荐指令指令---全列车门无法关闭故障产生
     * 推荐指令步骤 2.系统自动下发电子调度命令至车站，进行列车清人作业;
     * 第二次推荐指令指令---全列车门无法打开故障产生 一分钟清人方案
     * 推荐指令步骤 2.系统自动下发电子调度命令，通知车站配合清人作业;
     */
    public static final String PREPARE_CLEAR_TRAIN = "做好清客准备";
    /**
     * 牵引故障---第一次辅助推荐指令
     * 推荐指令步骤 3.自动向车辆调发送电子调度命令进行故障远程复位;
     */
    public static final String ALARM_REMOTE_RESTORATION = "进行故障远程复位";
    /**
     * 第一次推荐指令指令-多车门无法关闭故障产生（连续/非连续）
     * 推荐指令步骤 3.电子调度命令通知站务人员进行故障处置
     * 第一次推荐指令指令-多车门无法打开故障产生（连续/非连续）
     * 推荐指令步骤 2.电子调度命令通知站务人员进行故障处置
     * 第一次推荐指令指令-全列车门无法关闭故障产生
     * 推荐指令步骤 3.自动下发电子调度命令通知站务人员前往现场进行故障处置;
     * 第一次推荐指令指令-全列车门无法打开故障产生
     * 推荐指令步骤 2.系统自动下发电子调度命令通知站务人员进行故障处置;
     */
    public static final String ALARM_DISPOSITION = "进行故障处置";
    /**
     * 第二次推荐指令指令-多车门故障产生（连续/非连续，无法打开/关闭）
     * 推荐指令步骤 3.自动向车站发送电子调度命令提示故障无法恢复需使用门旁路发车
     */
    public static final String ALARM_NOT_RESTORATION = "故障无法恢复需使用门旁路发车";
    /**
     * 大客流之断面客流
     * 推荐指令步骤 3.自动下发电子调度命令通知所有车站预备车加开路线;
     */
    public static final String ALL_STATION_SPARE_TRAIN_ADD_LINE = "所有车站预备车加开路线";

    /**
     * 大客流之一级管控电子调度命令
     * 发送电子调度命令通知XX车站执行客流量一级管控措施;
     */
    public static final String LARGE_PASSENGER_FIRST_LEVEL_NOTICY_CMD = "准alarmStation站执行客流量一级管控措施";

    /**
     * 大客流之二级管控电子调度命令
     * 发送电子调度命令通知XX车站执行客流量二级管控措施;
     */
    public static final String LARGE_PASSENGER_SECOND_LEVEL_NOTICY_CMD = "准alarmStation站执行客流量二级管控措施";

    /**
     * 大客流之三级管控电子调度命令
     * 发送电子调度命令通知XX车站执行客流量三级管控措施;
     */
    public static final String LARGE_PASSENGER_THIRD_LEVEL_NOTICY_CMD = "准alarmStation站执行客流量三级管控措施";

    /**
     * 大客流之一级管控电子调度命令
     * 发送电子调度命令通知XX车辆段做好预备车加开准备;
     */
    public static final String LARGE_PASSENGER_FIRST_LEVEL_ADD_TRAIN_CMD = "准车辆段/停车场DCC进行预备车加开准备工作";

    /**
     * 第一次推荐指令指令-全列车门无法打开故障产生
     * 推荐指令步骤 3.提示行车调度员通知应急人员（司机）列车故障现象及进行故障处置;
     */
    public static final String DRIVER_ALARM_DISPOSITION = "通知应急人员（司机）列车故障现象及进行故障处置";
    /**
     * 第三次推荐指令指令---全列车门无法打开故障产生 四分钟清人方案
     * 推荐指令步骤 2.系统自动下发电子调度命令至车站，使用人工紧急解锁车门;
     */
    public static final String LABOUR_LOCK_DOOR = "使用人工紧急解锁车门";
    /**
     * 列车进/出站过程中站台门打开--第一次推荐指令
     * 推荐指令步骤 2.通知站务人员前往现场查看，若故障无法恢复，将站台门置于旁路（手动）位（此状态下不影响列车进站及开关门作业），并在现场进行监护。
     */
    public static final String SCENE_CHECK_AND_IN_THE_BYPASS = "通知站务人员前往现场查看，若处理后故障仍无法恢复，将站台门置于旁路（手动）位（此状态下不影响列车进站及开关门作业），并在现场进行监护";
}