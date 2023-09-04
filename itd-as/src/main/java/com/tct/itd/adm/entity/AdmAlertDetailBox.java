package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tct.itd.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author LYH
 * @since 2020-11-16
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("adm.t_adm_alert_detail_box")
public class AdmAlertDetailBox extends BaseEntity implements Serializable {

    private static final long serialVersionUID=1L;

    @JsonSerialize(using= ToStringSerializer.class)
    private Long detailId;

    /**
     * 处置方案框中的信息内容
     */
    private String message;

    /**
     * 当前方案的执行状态
     */
    private String status;

    public AdmAlertDetailBox(Long id, Long detailId, String message, String status) {
        super.id = id;
        this.detailId = detailId;
        this.message = message;
        this.status = status;
    }
}
