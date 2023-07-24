package com.tct.itd.xml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @Description
 * @Author zhaoke
 * @Date 2020/5/26 15:45
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "TrainNumber")
@XmlAccessorType(XmlAccessType.FIELD)
public class TrainNumber {
    /**
     * 车次号
     */
    @XmlAttribute(name = "OrderNumber")
    private String orderNumber;

    @XmlAttribute(name = "IsLock")
    private Boolean isLock;

    @XmlAttribute(name = "LastOrderNumber")
    private String lastOrderNumber;

    /**
     * 列车运行方向 1 下行 0 上行
     */
    @XmlAttribute(name = "RunDirect")
    private String runDirect;

    /**
     * 是否有自定义颜色
     */
    @XmlAttribute(name = "HasCustomColor")
    private String hasCustomColor;

    /**
     * 自定义颜色
     */
    @XmlAttribute(name = "CustomColorInfo")
    private String customColorInfo;

    /**
     * 是否为跨线车次
     */
    @XmlAttribute(name = "IsCrossLine")
    private String  isCrossLine;

    /**
     * 跨线车次表号
     */
    @XmlAttribute(name = "CrossLineServerNum")
    private String crossLineServerNum;

    /**
     * 源线路ID，全局调度下发时赋值
     */
    @XmlAttribute(name = "SrcLineId")
    private String srcLineId;

    /**
     * 目的线路ID，全局调度下发时赋值
     */
    @XmlAttribute(name = "DstLineId")
    private String dstLineId;

    /**
     * 源停车区域，全局调度下发时赋值
     */
    @XmlAttribute(name = "CrossSrcStopAreaId")
    private String  crossSrcStopAreaId;

    /**
     * 目的停车区域，全局调度下发时赋值
     */
    @XmlAttribute(name = "CrossDstStopAreaId")
    private String  crossDstStopAreaId;

    /**
     * 源跨线车次号，全局调度下发时赋值
     */
    @XmlAttribute(name = "CrossSrcOrderNumber")
    private String crossSrcOrderNumber = "";

    /**
     *目的跨线车次号，全局调度下发时赋值
     */
    @XmlAttribute(name = "CrossDstOrderNumebr")
    private String crossDstOrderNumebr = "";

    /**
     * 运营图字段
     */
    @XmlAttribute(name = "TrainOrganize")
    private String trainOrganize = "";

    /**
     * 预留
     */
    @XmlAttribute(name = "Reserve")
    private String  reserve;

    /**
     * 列车信息
     */
    @XmlElement(name = "Train")
    private List<Train> trains;
}
