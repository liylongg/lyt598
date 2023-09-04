package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.tct.itd.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 
 * </p>
 *
 * @author LYH
 * @since 2020-11-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("adm.t_adm_alert_info")
public class AdmAlertInfo extends BaseEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 故障类型
     */
    private String type;

    /**
     * 故障子类型
     */
    private String typeDetail;

    /**
     * 故障发生的时间，格式为yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date time;

    /**
     * 故障信息描述
     */
    private String message;

    /**
     * 故障位置
     */
    private String location;

    /**
     * 报警源
     */
    private String source;

    /**
     * 当前状态
     */
    private String status;

    /**
     * 0:不允许故障恢复,1:允许故障恢复,2:允许故障恢复，但是为禁用状态
     */
    @TableField(value = "allow_failover")
    private int allowFailover;

    /**
     * 应急事件续报按钮 0:不显示,1:显示,2:显示，但是为禁用状态
     */
    @TableField(value = "report")
    private int report;

    /**
     * 流程是否结束
     */
    @TableField(value = "end_life")
    private Boolean endLife;

    /**
     * 具备通车条件按钮(0:不显示 1:显示可以执行 2:显示,设为禁用)
     */
    @TableField(value = "ready")
    private int ready;

}
