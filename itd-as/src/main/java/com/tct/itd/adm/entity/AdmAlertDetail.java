package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tct.itd.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 *  t_adm_alert_detail 实体
 * </p>
 *
 * @author LYH
 * @since 2020-11-16
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("adm.t_adm_alert_detail")
public class AdmAlertDetail extends BaseEntity implements Serializable {

    private static final long serialVersionUID=1L;

    @JsonSerialize(using= ToStringSerializer.class)
    private Long infoId;

    /**
     * 每条信息的标题
     */
    private String title;

    /**
     * 信息生成的时间
     */
    private Date time;

    /**
     * 命令执行的详细内容信息
     */
    private String message;

    /**
     * 标识是否拥有详情框(0为没有，1方案详情，2运行图预览,3 电子调度)
     */
    private String tag;

    /**
     * 按钮的状态(0为可用但无执行按钮，1为可用有执行按钮，2为禁用该详情框)
     */
    private Integer buttonStatus = 2;

    public AdmAlertDetail(Long id,Long infoId, String title, Date time, String message, String tag, Integer buttonStatus,Long ts) {
        Date date = new Date(ts);
        super.id = id;
        super.ts = date;
        this.infoId = infoId;
        this.title = title;
        this.time = time;
        this.message = message;
        this.tag = tag;
        this.buttonStatus = buttonStatus;
    }
}
