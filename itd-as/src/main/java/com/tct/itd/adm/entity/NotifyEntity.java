package com.tct.itd.adm.entity;

import com.tct.itd.base.BaseEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author LYH
 * @Description 通知实体类
 * @Date 11:10 2021/2/20
 * @Param
 * @return
 **/
@Data
public class NotifyEntity extends BaseEntity implements Serializable {
    /**
     * 通知编码
     */
    private int code;
    /**
     * 通知名字
     */
    private String name;
    /**
     * 通知类别
     */
    private String type;
    /**
     * 通知内容
     */
    private String content;
    /**
     * 时间戳
     */
    private String timestamp;
    /**
     * 拓展字段1
     */
    private Long tableInfoId;
    /**
     * 拓展字段3
     */
    private Integer platformId = 0;

}
