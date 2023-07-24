package com.tct.itd.xml.topology;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

/**
 * @classname: ControlRoomClass
 * @description:
 * @author: liyunlong
 * @date: 2023/2/17 10:12
 */
@Data
public class ControlRoomClass {

    @JacksonXmlProperty(localName = "ControlRoomName")
    private String controlRoomName;

    @JacksonXmlElementWrapper(localName = "ServerList")
    @JacksonXmlProperty(localName = "ServerClass")
    private List<ServerClass> serverList;
}
