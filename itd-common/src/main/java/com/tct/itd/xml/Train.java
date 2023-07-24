package com.tct.itd.xml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @Description
 * @Author zhaoke
 * @Date 2020/5/26 15:51
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Train")
@XmlAccessorType(XmlAccessType.FIELD)
public class Train {

    /**
     * 时间 HH:mm:ss   (1.HH:mm:ss表示第二天)
     */
    @XmlAttribute(name = "Time")
    private String time;

    /**
     * 停车区域ID
     */
    @XmlAttribute(name = "StopAreaID")
    private Integer stopAreaID;

    /**
     * 列车信息类型，0：到达，1：出发
     */
    @XmlAttribute(name = "TrainInfoType")
    private Integer trainInfoType;

    /**
     * 是否折返
     */
    @XmlAttribute(name = "IsReturn")
    private String isReturn;

    /**
     * 预留
     */
    @XmlAttribute(name = "Reserve")
    private String reserve = "";

    /**
     * 是否有自定义颜色
     */
    @XmlAttribute(name = "HasCustomColor")
    private String hasCustomColor = "False";

    /**
     * 自定义颜色
     */
    @XmlAttribute(name = "CustomColorInfo")
    private String customColorInfo = "";

    /**
     * 运营图字段
     */
    @XmlAttribute(name = "RunLevel")
    private String runLevel = "";


    /**
     * 运营图字段
     */
    @XmlAttribute(name = "IsCarry")
    private String isCarry = "False";

}
