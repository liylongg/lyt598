package com.tct.itd.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author 上报信息
 * @Description
 * @Date 16:17 2020/5/20
 **/
@Data
public class Info implements Serializable {
    //状态码
    private int infoId;
    //上报数据
    private Object data;

    public Info() {
    }

    public Info(int infoId, Object data) {
        this.infoId = infoId;
        this.data = data;
    }

}
