package com.tct.itd.adm.iconstant;

import lombok.Getter;

/**
 * @classname: RecoveryCodeEnum
 * @description: 道岔故障RecoveryCode枚举
 * @author: liyunlong
 * @date: 2022/3/9 11:39
 */
@Getter
public enum RecoveryCodeEnum {

    BREAK(1, "中断"),
    BREAK_RECOVERY(2, "中断-恢复"),
    BREAK_SLOWLY(3, "中断-缓行"),
    SLOWLY_RECOVERY(4, "缓行-恢复"),
    LATE(5, "晚点调整"),
    SLOWLY_ADJUST(6, "缓行调整"),
    SLOWLY(7, "区间缓行"),
    LATE_BREAK(8, "晚点-中断"),
    LATE_SWITCH(9, "道岔故障晚点"),
    LATE_SLOW_DOWN(10, "车门晚点到缓行"),
    TERMINAL_SLOWLY(11, "终端站小交路+缓行"),
    SINGLE_BREAK(12, "单边中断"),
    SLOWLY_CHANGE(13, "缓行加换轨")
    ;
    private Integer code;

    private String msg;

    RecoveryCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
