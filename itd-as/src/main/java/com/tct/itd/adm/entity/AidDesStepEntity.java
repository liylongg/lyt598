package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tct.itd.base.BaseEntity;
import lombok.*;
import lombok.experimental.Accessors;

/***
 * @Description 推荐指令实体表
 * @Author yuelei
 * @Date 2021/9/13 16:08
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
@TableName("adm.t_aid_des_step")
@ToString
@Accessors(chain = true)
public class AidDesStepEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 故障类型表id
     */
    @TableField(value = "alarm_type_id")
    private long alarmTypeId;

    /**
     * 推荐指令内容
     */
    @TableField(value = "aid_content")
    private String aidContent;

    /**
     * 推荐指令步骤
     */
    @TableField(value = "step")
    private Integer step;

    /**
     * 等待时间，单位毫秒ms
     */
    @TableField(value = "wait_time")
    private Integer waitTime;

    /**
     * 1:启用/0:禁用
     */
    @TableField(value = "enable")
    private Integer enable;

    /**
     * 辅助步骤，情况拆分
     */
    @TableField(value = "type")
    private Integer type;

}
