package com.tct.itd.adm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
 *  t_adm_upload_file 实体
 * </p>
 *
 * @author LYH
 * @since 2020-11-16
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("adm.t_adm_upload_file")
public class UploadFileEntity extends BaseEntity {

    /**
     * 应急事件ID
     */
    @TableField(value = "info_id")
    private Long infoId;

    /**
     * 文件名称
     */
    @TableField(value = "file_name")
    private String fileName;
    /**

     * 文件保存地址
     */
    @TableField(value = "path")
    private String path;

    /**
     * 文件保存时间
     */
    @TableField(value = "save_time")
    private Date saveTime;

}
