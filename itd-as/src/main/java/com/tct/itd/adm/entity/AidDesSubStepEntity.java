package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tct.itd.base.BaseEntity;
import lombok.*;

/***
 * @Description 推荐指令执行单元实体表
 * @Author yuelei
 * @Date 2021/9/13 16:08
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
@TableName("adm.t_aid_des_sub_step")
public class AidDesSubStepEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 推荐指令ID
     */
    @TableField(value = "aid_step_id")
    private long aidStepId;

    /**
     * 执行单元步骤
     */
    @TableField(value = "sub_step")
    private Integer subStep;

    /**
     * 执行单元类型，0:只显示/1:显示并执行/2:不显示,要执行
     */
    @TableField(value = "sub_step_type")
    private Integer subStepType;

    /**
     * 执行单元内容
     */
    @TableField(value = "sub_step_content")
    private String subStepContent;

    /**
     * 对应bean名称
     */
    @TableField(value = "bean_name")
    private String beanName;

    /**
     * 电子调度命令发送内容
     */
    @TableField(value = "dis_cmd_content")
    private String disCmdContent;

    /**
     * 发送车站
     */
    @TableField(value = "accept_station")
    private Integer acceptStation;

    /**
     * 步骤参数
     */
    @TableField(value = "param")
    private String param;

    /**
     * 操作类型 0:手动 1：自动
     */
    @TableField(value = "opt_type")
    private Integer optType;

    /**
     * 1:启用/0:禁用
     */
    @TableField(value = "enable")
    private Integer enable;
}
