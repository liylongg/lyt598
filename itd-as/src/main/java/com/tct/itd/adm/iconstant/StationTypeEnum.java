package com.tct.itd.adm.iconstant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description 车站类型
 * @Author yuelei
 * @Date 2022/3/17 16:34
 */
@Getter
@AllArgsConstructor
public enum StationTypeEnum {

    //车站类型(-1:维护0:中心1:站 2:段 3:调 4:区间 5:派班 6:停车场)
    MAINTAIN(-1, "维护"),
    CENTER(0, "中心"),
    STATION(1, "站"),
    DEPOT(2, "段"),
    MANAGE(3, "调"),
    SECTION(4, "区间"),
    DISPATCH(5, "派班"),
    PARK(6, "停车场");

    /**
     * 车站类型
     */
    private Integer type;

    /**
     * 类型描述
     */
    private String desc;
}
