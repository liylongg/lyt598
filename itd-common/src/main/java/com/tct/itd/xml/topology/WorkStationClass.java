package com.tct.itd.xml.topology;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * @classname: WorkStationClass
 * @description:
 * @author: liyunlong
 * @date: 2023/2/17 10:45
 */
@Data
public class WorkStationClass {

    @JacksonXmlProperty(localName = "ID")
    private int id;

    @JacksonXmlProperty(localName = "Name")
    private String name;

    @JacksonXmlProperty(localName = "Status")
    private int status;

}
