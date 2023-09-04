package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tct.itd.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @Description 运行图调整记录
 * @author yuelei
 * @Date 2022/2/23 14:12
 */
@Getter
@Setter
@TableName("adm.t_adm_graph_record")
@AllArgsConstructor
@NoArgsConstructor
public class AdmGraphRecordEntity extends BaseEntity {

    private static final long serialVersionUID = 11111L;

    /**
     * 应急事件ID
     */
    @TableField(value = "detail_id")
    private long detailId;

    /**
     * 调整之前运行图
     */
    @TableField(value = "change_before")
    private byte[]  changeBefore;

    /**
     * 调整之后运行图
     */
    @TableField(value = "change_after")
    private byte[] changeAfter;

    /**
     * 调整方案
     */
    @TableField(value = "adjust_case")
    private String adjustCase;
}
