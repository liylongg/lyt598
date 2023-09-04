package com.tct.itd.adm.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;
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
@Accessors(chain = true)
public class AdmDispatchCmdDTO implements Serializable {
    private static final long serialVersionUID = 839140483311035057L;
    /**
     * 主键id
     */
    private Long id;

    /**
     * 命令日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private Date commandDate;

    /**
     * 命令时间
     */
    private String commandTime;

    /**
     * 命令类型
     */
    private Long commandType;

    /**
     * 命令号码
     */
    private String commandCode;

    /**
     * 命令类型描述
     */
    private String commandTypeDesc;

    /**
     * 命令内容
     */
    private String commandContext;

    /**
     * 转交
     */
    private String transmit;

    /**
     * 受令处所
     */
    private String receiveStation;

    /**
     * 发令人
     */
    private String sendUser;

    /**
     * 调度员代码
     */
    private String dispatcherCode;

    /**
     * 状态
     */
    private String state;

    /**
     * 已签收车站
     */
    private String sign;

    /**
     * 未签收车站
     */
    private String notSign;

    /**
     * 电子调度命令备注 zhangyinglong added on 2021/06/15
     */
    private String commandRemark;

    /**
     *  签收按钮是否显示 签收:显示 空:不显示
     */
    private String showBtnFlag;
}
