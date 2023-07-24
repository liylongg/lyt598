package com.tct.itd.xml.topology;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

/**
 * @classname: DeviceTopologyClass
 * @description: 设备拓扑图
 * @author: liyunlong
 * @date: 2023/2/17 9:51
 */
@Data
@JacksonXmlRootElement(localName = "DeviceTopologyClass")
public class DeviceTopologyClass {

    @JacksonXmlProperty(localName = "LineId")
    private int lienId;

    @JacksonXmlElementWrapper(localName = "StationList")
    @JacksonXmlProperty(localName = "StationClass")
    private List<StationClass> stationLists;
}
