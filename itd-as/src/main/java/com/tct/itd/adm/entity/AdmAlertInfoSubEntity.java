package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tct.itd.base.BaseEntity;
import lombok.Data;

/**
 * (AdmCommandType)实体类
 * @author yl
 * @version 1.0
 * @date 2021-11-1 15:04:37
 */
@Data
@TableName("adm.t_adm_alert_info_sub")
public class AdmAlertInfoSubEntity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     *t_adm_alert_info主键ID
     */
    @TableField(value = "table_info_id")
    private long tableInfoId;

    /**
     *物理区段编号
     */
    @TableField(value = "physics_section_type")
    private Integer physicsSectionType;

    /**
     *停车区域编号
     */
    @TableField(value = "stop_area_number")
    private Integer stopAreaNumber;

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
     *行驶方向 85-上行；170-下行
     */
    @TableField(value = "up_down")
    private Integer upDown;

    /**
     * 车组号,加开/备开需要,非加开/备开没有车组号
     */
    @TableField(value = "train_id")
    private String trainId;

    /**
     * 车次号
     */
    @TableField(value = "order_num")
    private String orderNum;

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

    /**
     *故障状态 1:产生故障（马上掉线） 2:解除故障（晚点调整）3:故障运行至下一站（掉线）
     * 4:故障运行至终点站（掉线,故障站不扣车）5:故障运行至终点站（掉线,故障站要扣车）
     */
    @TableField(value = "alarm_state")
    private Integer alarmState;

    /**
     * 推荐指令执行步骤 0:放弃推荐指令 1:故障开始扣车推荐指令 2:故障告警超时,执行第二次调图推荐指令 3:故障恢复,抬车推荐指令(车未晚点,不需调图)
     * 4:故障恢复,抬车推荐指令(车有晚点,需调图) 5:全列车门第三次推荐指令方案步骤
     */
    @TableField(value = "execute_step")
    private String executeStep;

    /**
     * 故障类型AlarmTypeEnum 1:车门故障 2:空调故障 3:广播故障 4:牵引故障 5: 大客流  6：站台门故障
     */
    @TableField(value = "alarm_type")
    private String alarmType;

    /**
     * 故障子类型 AlarmTypeDetailEnum 配合故障类型使用
     */
    @TableField(value = "alarm_type_detail")
    private String alarmTypeDetail;

    /**
     * 告警信息
     */
    @TableField(value = "alarm_data")
    private String alarmData;

    /**
     * 故障地点
     */
    @TableField(value = "alarm_site")
    private String alarmSite;

    /**
     * t_adm_alert_detail_box主键ID
     */
    @TableField(value = "table_box_id")
    private long tableBoxId;

    /**
     * t_adm_alert_detail_box主键ID
     */
    @TableField(value = "table_box_id_two")
    private long tableBoxId2;

    /**
     * 调整运行图的方案：最优情况和最坏情况
     */
    @TableField(value = "case_code")
    private Integer caseCode;

    /**
     * 拥挤密度级别,与密度一一对应
     */
    @TableField(value = "large_pass_flow_crowd_level")
    private Integer largePassFlowCrowdLevel;

    /**
     * 是否是选择的断面客流 0:否，1:是
     */
    @TableField(value = "large_pass_flow_section_flow")
    private Integer largePassFlowSectionFlow;

    /**
     * 是否是选择的区间
     */
    @TableField(value = "section_flag")
    private Integer sectionFlag;

    /**
     * 上报故障信息的客户端编号 0：中心，非0：某个车站
     */
    @TableField(value = "report_client_index")
    private Integer reportClientIndex;

    /**
     * 车站上报故障消息是否被中心确认  0：未确认，1：确认
     */
    @TableField(value = "if_confirmed")
    private Integer ifConfirmed;

    /**
     * 故障恢复步骤数, 牵引区间存在两次故障恢复
     */
    @TableField(value = "failure_recovery_step")
    private Integer failureRecoveryStep;

    /**
     * 是否已执行（0 未执行 1已执行 2执行放弃）
     */
    @TableField(value = "execute_end")
    private Integer executeEnd;

    /**
     * 道岔名称
     */
    @TableField(value = "switch_name")
    private String switchName;

    /**
     * 道岔编号
     */
    @TableField(value = "switch_no")
    private String switchNo;
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
     * 牵引供电区段编号
     */
    @TableField(value = "traction_section_id")
    private Integer tractionSectionId;

    /**
     * 是否自动上报 true 是
     */
    @TableField(value = "auto_report")
    private Boolean autoReport;

    /**
     * 故障录入集中区(可能多个集中区id)
     */
    @TableField(value = "alarm_con_station")
    private String alarmConStation;

    /**
     * 故障升级表id
     */
    @TableField(value = "upgrade_id")
    private long upgradeId;

    /**
     * 防护道岔编号
     */
    @TableField(value = "protect_switch_no")
    private String protectSwitchNo;
}