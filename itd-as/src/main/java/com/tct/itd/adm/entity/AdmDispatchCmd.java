package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.tct.itd.base.BaseEntity;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * (AdmDispatchCommand)实体类
 * @author Liyuanpeng
 * @version 1.0
 * @date 2020-11-12 15:03:53
 */
@Data
@TableName("adm.t_adm_dispatch_command")
public class AdmDispatchCmd extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 839140483311035057L;

    /**
     * 命令日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @TableField(value = "command_date")
    private Date commandDate;

    /**
     * 命令时间
     */
    @TableField(value = "command_time")
    private String commandTime;

    /**
     * 命令类型
     */
    @TableField(value = "command_type")
    private Long commandType;

    /**
     * 命令类型描述
     */
    @TableField(value = "command_type_desc")
    private String commandTypeDesc;

    /**
     * 命令号码
     */
    @TableField(value = "command_code")
    private String commandCode;

    /**
     * 命令内容
     */
    @TableField(value = "command_context")
    private String commandContext;

    /**
     * 转交
     */
    @TableField(value = "transmit")
    private String transmit;

    /**
     * 受令处所
     */
    @TableField(value = "receive_station")
    private String receiveStation;

    /**
     * 发令人
     */
    @TableField(value = "send_user")
    private String sendUser;

    /**
     * 调度员代码
     */
    @TableField(value = "dispatcher_code")
    private String dispatcherCode;

    /**
     * 状态
     */
    @TableField(value = "state")
    private String state;

    /**
     * 已签收车站
     */
    @TableField(value = "sign")
    private String sign;

    /**
     * 未签收车站
     */
    @TableField(value = "not_sign")
    private String notSign;

    /**
     * 电子调度命令备注 zhangyinglong added on 2021/06/15
     */
    @TableField(value = "command_remark")
    private String commandRemark;

}