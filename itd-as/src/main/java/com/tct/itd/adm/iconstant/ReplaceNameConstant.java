package com.tct.itd.adm.iconstant;

/**
 * @Description 数据库配置变量名替换
 * @Author yuelei
 * @Date 2022/5/1 16:45
 */
public class ReplaceNameConstant {

    /**
     * 故障站
     */
    public static final String ALARM_STATION = "alarmStation";

    /**
     * 故障名称
     */
    public static final String ALARM_NAME = "alarmName";

    /**
     * 几车几门描述
     */
    public static final String DOOR_DESC = "doorDesc";

    /**
     * 折返车次
     */
    public static final String RETRACE_NUM = "RetraceNum";
    /**
     * 故障车次
     */
    public static final String ORDER_NUM = "orderNum";

    /**
     * 接触网失电故障区段
     */
    public static final String TRACTION_SECTION_NAME = "tractionSectionName";

    /**
     * 故障车车组号
     */
    public static final String TRAIN_NUM = "trainNum";

    /**** 自动下发调度命令模板 ***/

    /**
     * 回库场段名称
     */
    public static final String END_DEPOT_NAME = "endDepotName";

    /**
     * 掉线车站名称
     */
    public static final String DROP_STATION_NAME = "dropStationName";

    /**
     * 上下行
     */
    public static final String IS_UP = "isUp";

    /**
     * 终点车站名称
     */
    public static final String END_STATION_NAME = "endStationName";

    /**
     * 起始车站名称
     */
    public static final String START_STATION_NAME = "startStationName";

    /**
     * 下一个车次
     */
    public static final String NEXT_ORDER_NUM = "nextOrderNum";

    /**
     * 前车车次
     */
    public static final String PRE_ORDER_NUM = "preOrderNum";

    /**
     * 加车起点
     */
    public static final String ADD_TRAIN_START_STATION_NAME = "addTrainStartStationName";

    /**
     * 加车终点
     */
    public static final String ADD_TRAIN_END_STATION_NAME = "addTrainEndStationName";

    /**
     * 载客车站 addTrainStationNameList
     */
    public static final String START_PASSENGER_STATION_NAME= "startPassengerStationName";

    /**
     * 载客列车动车时分
     */
    public static final String ADD_TRAIN_STATION_LAUNCH_TIME= "addTrainStationLaunchTime";

    /**
     * 第一个加开放空车次
     */
    public static final String FIRST_ADD_VENT_ORDER_NUM = "firstAddVentOrderNum";

    /**
     * 第一个折返站台名称
     */
    public static final String FIRST_ADD_VENT_RETURN_STATION_NAME = "firstAddVentReturnStationName";

}
