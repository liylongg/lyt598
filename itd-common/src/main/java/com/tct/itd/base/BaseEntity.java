package com.tct.itd.base;

import com.baomidou.mybatisplus.annotation.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description 基类entity
 * @Author zhaoke
 * @Date 2020/9/10 16:28
 */

@Accessors(chain = true)
public abstract class BaseEntity implements Serializable {


    @TableId(value = "id",type = IdType.ASSIGN_ID)
    protected Long id;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    protected Date createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    protected Date updateTime;

    /**
     * 逻辑删除(1删除 0未删除)
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    protected Integer dr;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    protected Date ts;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDr() {
        return dr;
    }

    public void setDr(Integer dr) {
        this.dr = dr;
    }

    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
    }
}
