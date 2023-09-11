package com.tct.itd.adm.iconstant;

import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @classname: AlarmTypeConstant
 * @description: 应急事件常量
 * @author: liyunlong
 * @date: 2023/3/23 17:29
 */
@Component
public class AlarmTypeConstant {

    /**
     * 车门故障
     */
    public static String TRAIN_DOOR_FAILURE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("TRAIN_DOOR_FAILURE");

    /**
     * 车门故障 - 单车门无法关闭
     */
    public static String ONLY_DOOR_CANNOT_CLOSE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("ONLY_DOOR_CANNOT_CLOSE");

    /**
     * 车门故障 - 单车门无法关闭 -缓行算法
     */
    public static  String ONLY_DOOR_CANNOT_CLOSE_SLOW_DOWN = AdmAlertDetailTypeService.getCodeByAlarmTypeName("ONLY_DOOR_CANNOT_CLOSE_SLOW_DOWN");

    /**
     * 车门故障-单车门无法打开
     */
    public static String ONLY_DOOR_CANNOT_OPEN = AdmAlertDetailTypeService.getCodeByAlarmTypeName("ONLY_DOOR_CANNOT_OPEN");

    /**
     * 车门故障-全列车门无法打开
     */
    public static String ALL_TRAIN_DOOR_CANNOT_OPEN = AdmAlertDetailTypeService.getCodeByAlarmTypeName("ALL_TRAIN_DOOR_CANNOT_OPEN");

    /**
     * 车门故障 - 全列车门无法关闭
     */
    public static String ALL_TRAIN_DOOR_CANNOT_CLOSE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("ALL_TRAIN_DOOR_CANNOT_CLOSE");

    /**
     * 车门故障 - 全列车门无法关闭-缓行
     */
    public static String ALL_TRAIN_DOOR_CANNOT_CLOSE_SLOW_DOWN = AdmAlertDetailTypeService.getCodeByAlarmTypeName("ALL_TRAIN_DOOR_CANNOT_CLOSE_SLOW_DOWN");

    /**
     * 空调故障
     */
    public static String CONDITIONING_FAILURE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("CONDITIONING_FAILURE");

    /**
     * 列车空调故障
     */
    public static String AIR_CONDITIONING_FAILURE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("AIR_CONDITIONING_FAILURE");

    /**
     * 通风功能故障
     */
    public static String AIR_CONDITIONING_VENTILATE_FAILURE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("AIR_CONDITIONING_VENTILATE_FAILURE");

    /**
     * 广播故障
     */
    public static String BROADCAST_FAILURE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("BROADCAST_FAILURE");

    /**
     * 广播故障 - 人工广播可用
     */
    public static String BROADCAST_FAILURE_CAN_MANUAL = AdmAlertDetailTypeService.getCodeByAlarmTypeName("BROADCAST_FAILURE_CAN_MANUAL");
    /**
     * 广播故障 - 人工广播不可用
     */
    public static String BROADCAST_FAILURE_CANNOT_MANUAL = AdmAlertDetailTypeService.getCodeByAlarmTypeName("BROADCAST_FAILURE_CANNOT_MANUAL");

    /**
     * 牵引故障大类
     */
    public static String MCM_FAILURE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("MCM_FAILURE");

    /**
     * 多牵引故障
     */
    public static String MORE_MCM_FAILURE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("MORE_MCM_FAILURE");

    /**
     * 大客流场景
     */
    public static String LARGE_PASSENGER = AdmAlertDetailTypeService.getCodeByAlarmTypeName("LARGE_PASSENGER");

    /**
     * 大客流场景
     */
    public static String LARGE_PASSENGER_FLOW = AdmAlertDetailTypeService.getCodeByAlarmTypeName("LARGE_PASSENGER_FLOW");

    /**
     * 站台门场景
     */
    public static String PLATFORM_DOOR = AdmAlertDetailTypeService.getCodeByAlarmTypeName("PLATFORM_DOOR");

    /**
     * 站台门故障-列车进站过程中站台门打开
     */
    public static String ALL_PLATFORM_DOOR_OPEN_INTO_STATION = AdmAlertDetailTypeService.getCodeByAlarmTypeName("ALL_PLATFORM_DOOR_OPEN_INTO_STATION");

    /**
     * 站台门故障-列车出站过程中站台门打开
     */
    public static String ALL_PLATFORM_DOOR_OPEN_OUT_STATION = AdmAlertDetailTypeService.getCodeByAlarmTypeName("ALL_PLATFORM_DOOR_OPEN_OUT_STATION");

    /**
     * 站台门故障-单档站台门无法打开
     */
    public static String SINGLE_PLATFORM_DOOR_CANNOT_OPEN = AdmAlertDetailTypeService.getCodeByAlarmTypeName("SINGLE_PLATFORM_DOOR_CANNOT_OPEN");

    /**
     * 站台门故障-整侧站台门无法打开
     */
    public static String ALL_PLATFORM_DOOR_CANNOT_OPEN = AdmAlertDetailTypeService.getCodeByAlarmTypeName("ALL_PLATFORM_DOOR_CANNOT_OPEN");

    /**
     * 站台门故障-单档站台门无法关闭
     */
    public static String SINGLE_PLATFORM_DOOR_CANNOT_CLOSE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("SINGLE_PLATFORM_DOOR_CANNOT_CLOSE");

    /**
     * 站台门故障-整侧站台门无法关闭
     */
    public static String ALL_PLATFORM_DOOR_CANNOT_CLOSE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("ALL_PLATFORM_DOOR_CANNOT_CLOSE");

    /**
     * 道岔故障
     */
    public static String SWITCH_FAILURE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("SWITCH_FAILURE");

    /**
     * 道岔故障-终端站折返道岔故障-具备本站折返
     */
    public static String SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE =
            AdmAlertDetailTypeService.getCodeByAlarmTypeName("SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE");

    /**
     * 道岔故障-线路终点站常态化站后折返道岔故障-不具备站前折返
     */
    public static String BEHIND_FAILURE_NOT_HAS_FRONT_TURN = AdmAlertDetailTypeService.getCodeByAlarmTypeName(
            "BEHIND_FAILURE_NOT_HAS_FRONT_TURN");

    /**
     * 线路终点站常态化站前折返道岔故障-不具备站后折返
     */
    public static String FRONT_FAILURE_NOT_HAS_BEHIND_TURN = AdmAlertDetailTypeService.getCodeByAlarmTypeName(
            "FRONT_FAILURE_NOT_HAS_BEHIND_TURN");

    /**
     * 道岔故障-中间道岔故障-影响单行
     */
    public static String SWITCH_FAILURE_MIDDLE_SINGLE = AdmAlertDetailTypeService.getCodeByAlarmTypeName(
            "SWITCH_FAILURE_MIDDLE_SINGLE");

    /**
     * 计轴故障
     */
    public static String AXLE_COUNTER = AdmAlertDetailTypeService.getCodeByAlarmTypeName("AXLE_COUNTER");
    /**
     * 计轴故障-ARB计轴故障
     */
    public static String AXLE_COUNTER_ARB = AdmAlertDetailTypeService.getCodeByAlarmTypeName("AXLE_COUNTER_ARB");
    /**
     * 计轴故障-非道岔区段计轴故障
     */
    public static String AXLE_COUNTER_NOT_SWITCH = AdmAlertDetailTypeService.getCodeByAlarmTypeName("AXLE_COUNTER_NOT_SWITCH");
    /**
     * 计轴故障-道岔区段计轴故障
     */
    public static String AXLE_COUNTER_SWITCH = AdmAlertDetailTypeService.getCodeByAlarmTypeName("AXLE_COUNTER_SWITCH");
    /**
     * 失电故障-接触网失电故障
     */
    public static String TRACTION_POWER_PARENT = AdmAlertDetailTypeService.getCodeByAlarmTypeName("TRACTION_POWER_PARENT");
    /**
     * 接触网失电故障
     */
    public static String TRACTION_POWER = AdmAlertDetailTypeService.getCodeByAlarmTypeName("TRACTION_POWER");
    /**
     * 联锁故障
     */
    public static String INTERLOCKING_DOUBLE_PARENT = AdmAlertDetailTypeService.getCodeByAlarmTypeName("INTERLOCKING_DOUBLE_PARENT") ;
    /**
     * 联锁双机故障
     */
    public static String INTERLOCKING_DOUBLE = AdmAlertDetailTypeService.getCodeByAlarmTypeName("INTERLOCKING_DOUBLE");

    /**
     * 信号故障
     */
    public static String SIGNAL_ELECTRIC_PARENT = AdmAlertDetailTypeService.getCodeByAlarmTypeName("SIGNAL_ELECTRIC_PARENT");
    /**
     * 信号电源故障
     */
    public static String SIGNAL_ELECTRIC = AdmAlertDetailTypeService.getCodeByAlarmTypeName("SIGNAL_ELECTRIC");
    /**
     * 计轴故障-ARB计轴故障-复位
     */
    public static String AXLE_COUNTER_ARB_RESET = AdmAlertDetailTypeService.getCodeByAlarmTypeName("AXLE_COUNTER_ARB_RESET");
    /**
     * 计轴故障-非道岔区段计轴故障-复位
     */
    public static String AXLE_COUNTER_NOT_SWITCH_RESET = AdmAlertDetailTypeService.getCodeByAlarmTypeName("AXLE_COUNTER_NOT_SWITCH_RESET");
    /**
     * 计轴故障-道岔区段计轴故障-复位
     */
    public static String AXLE_COUNTER_SWITCH_RESET = AdmAlertDetailTypeService.getCodeByAlarmTypeName("AXLE_COUNTER_SWITCH_RESET");
    /**
     * 虚拟编组故障
     */
    public static String VIRTUAL_GROUP = AdmAlertDetailTypeService.getCodeByAlarmTypeName("VIRTUAL_GROUP");

    /**
     * 线路中间站道岔故障(非小交路折返站-影响单行)
     */
    public static String MIDDLE_FAILURE_SINGLE = AdmAlertDetailTypeService.getCodeByAlarmTypeName(
            "MIDDLE_FAILURE_SINGLE");

    /**
     * 线路中间站道岔故障(非小交路折返站-影响上下行)
     */
    public static String MIDDLE_FAILURE_UP_DOWN = AdmAlertDetailTypeService.getCodeByAlarmTypeName(
            "MIDDLE_FAILURE_UP_DOWN");

    /**
     * 道岔故障-终端站后折返道岔故障可换轨
     */
    public static String BEHIND_FAILURE_CHANGE = AdmAlertDetailTypeService.getCodeByAlarmTypeName(
            "BEHIND_FAILURE_CHANGE");

}

