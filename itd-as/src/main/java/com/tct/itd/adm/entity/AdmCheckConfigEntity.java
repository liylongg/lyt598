package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tct.itd.base.BaseEntity;
import lombok.Data;

/**
 * (t_adm_check_config)实体类
 * @author yl
 * @version 1.0
 * @date 2021-12-14 15:04:37
 */
@Data
@TableName("adm.t_adm_check_config")
public class AdmCheckConfigEntity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * bean名称
     */
    @TableField(value = "bean_name")
    private String beanName;

    /**
     * 故障类型
     */
    @TableField(value = "alarm_type")
    private String alarmType;

    /**
     * 0：针对所有  1：针对特定类型
     */
    @TableField(value = "type")
    private Integer type;

    /**
     * 序号
     */
    @TableField(value = "num")
    private Integer num;

}