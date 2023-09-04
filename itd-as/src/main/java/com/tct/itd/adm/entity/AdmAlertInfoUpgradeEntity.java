package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tct.itd.base.BaseEntity;
import lombok.Data;

/**
 * @author kangyi
 * @description 故障升级实体类
 * @date 2022年 07月21日 11:56:20
 */
@Data
@TableName("adm.t_adm_alert_info_upgrade")
public class AdmAlertInfoUpgradeEntity extends BaseEntity {

    /**
     * 故障类型
     */
    @TableField(value = "alarm_type")
    private String alarmType;

    /**
     * 故障子类型
     */
    @TableField(value = "alarm_type_detail")
    private String alarmTypeDetail;

    /**
     * 故障录入集中区(可能多个集中区id)
     */
    @TableField(value = "alarm_con_station")
    private String alarmConStation;

    /**
     * 计轴编号
     */
    @TableField(value = "axle_counter_id")
    private Integer axleCounterId;

    /**
     * 计轴名称
     */
    @TableField(value = "axle_counter_name")
    private String axleCounterName;

    /**
     * 推荐指令执行步骤 0:放弃推荐指令 1:故障开始扣车推荐指令 2:故障告警超时,执行第二次调图推荐指令 3:故障恢复,抬车推荐指令(车未晚点,不需调图)
     * 4:故障恢复,抬车推荐指令(车有晚点,需调图) 5:全列车门第三次推荐指令方案步骤
     */
    @TableField(value = "execute_step")
    private String executeStep;

    /**
     * 是否已执行（0 未执行 1已执行 2执行放弃）
     */
    @TableField(value = "execute_end")
    private Integer executeEnd;

    /**
     *行驶方向 85-上行；170-下行
     */
    @TableField(value = "up_down")
    private Integer upDown;

    /**
     *站台编号
     */
    @TableField(value = "platform_id")
    private String platformId;

    /**
     *车站编号
     */
    @TableField(value = "station_id")
    private Integer stationId;

    /**
     * 故障开始时间
     */
    @TableField(value = "start_alarm_time")
    private String startAlarmTime;

    /**
     *故障结束时间
     */
    @TableField(value = "end_alarm_time")
    private String endAlarmTime;

}
