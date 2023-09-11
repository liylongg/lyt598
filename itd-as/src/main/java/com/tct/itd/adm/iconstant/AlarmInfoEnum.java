package com.tct.itd.adm.iconstant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description: 消息类型 协议中msgtype
 * @Author: YHF
 * @date 2020/5/20
 */
@Getter
@AllArgsConstructor
public enum AlarmInfoEnum {

    /**
     * 是否已执行
     */
    EXECUTE_END_0(0, "未执行"),
    EXECUTE_END_1(1, "已执行"),
    EXECUTE_END_2(2, "执行放弃")
    ;
    /**
     * 编码
     */
    private Integer code;

    /**
     * 描述
     */
    private String name;
}