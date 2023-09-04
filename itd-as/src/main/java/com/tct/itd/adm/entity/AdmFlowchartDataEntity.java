package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tct.itd.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author yuzhenxin
 * @date 2022-11-11 09:48:15
 * @desc 流程图数据对象
 */
@Getter
@Setter
@TableName("adm.t_adm_flowchart_data")
@AllArgsConstructor
@NoArgsConstructor
public class AdmFlowchartDataEntity extends BaseEntity {


    /**
     * 应急事件ID
     */
    @TableField(value = "table_info_id")
    private long tableInfoId;


    /**
     * 流程图信息
     */
    @TableField(value = "flowchart_data")
    private String flowchartData;


}
