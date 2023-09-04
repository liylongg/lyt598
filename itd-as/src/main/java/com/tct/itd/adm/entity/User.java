package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tct.itd.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author zhaoke
 * @Date 2020-12-01 15:15
 */
@Setter
@Getter
@TableName("adm.t_adm_user")
public class User extends BaseEntity {

    private static final long serialVersionUID = 5128498212690558429L;

    @TableField("name")
    private String name;

    @TableField("age")
    private Integer age;

    @TableField("sex")
    private Integer sex;

}
