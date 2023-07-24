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
 * @Description 表号信息
 * @Author zhaoke
 * @Date 2020/5/26 15:42
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "ServerNumber")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServerNumber {

    /**
     * 表号ID
     */
    @XmlAttribute(name = "ID")
    private String id;

    @XmlAttribute(name = "ISLOCK")
    private Boolean isLock;

    /**
     * 是否有自定义颜色
     */
    @XmlAttribute(name = "HasCustomColor")
    private boolean hasCustomColor = false;

    /**
     * 自定义颜色
     */
    @XmlAttribute(name = "CustomColorInfo")
    private String customColorInfo;

    /**
     * 是否为跨线表号
     */
    @XmlAttribute(name = "IsCrossLineServer")
    private String isCrossLineServer;

    /**
     * 预留 (车底号)
     */
    @XmlAttribute(name = "Reserve")
    private String reserve;

    /**
     * 车次信息
     */
    @XmlElement(name = "TrainNumber")
    private List<TrainNumber> trainNumbers;
}
