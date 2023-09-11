package com.tct.itd.adm.iconstant;

import lombok.AllArgsConstructor;

/**
 * @Description 调用算法服务调图类别
 * @Author zyl
 * @Date 2021/8/12 16:23
 **/
@AllArgsConstructor
public enum AlarmStateEnum {

    STATION_DROP_LINE(1,"本站掉线"),

    LATE_ADJUSTMENT(2,"晚点调整"),

    NEXT_STATION_DROP_LINE(3,"下一站掉线"),

    TERMINAL_POINT_DROP_LINE(4,"终点掉线,本站不扣车"),

    TERMINAL_POINT_DROP_LINE_HOLD(5, "终点掉线,本站扣车"),

    TRAIN_DOOR_STATION_DROP_LINE(6, "车门场景本站掉线"),

    TRAIN_DOOR_END_DROP_LINE_HOLD(7, "车门场景终点站掉线-本站扣车"),

    REVERSE_RAIL(8, "折返轨特殊处理");

    private final Integer code;

    private final String desc;

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
