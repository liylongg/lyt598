package com.tct.itd.xml.topology;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

/**
 * @classname: StationClass
 * @description:
 * @author: liyunlong
 * @date: 2023/2/17 9:57
 */
@Data
public class StationClass {

    @JacksonXmlProperty(localName = "StationId")
    private int stationId;

    @JacksonXmlProperty(localName = "StationName")
    private String stationName;

    @JacksonXmlElementWrapper(localName = "ControlRoomList")
    @JacksonXmlProperty(localName = "ControlRoomClass")
    private List<ControlRoomClass> controlRoomLists;
}
