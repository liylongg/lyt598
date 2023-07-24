package com.tct.itd.xml.topology;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

/**
 * @classname: ServerClassBase
 * @description:
 * @author: liyunlong
 * @date: 2023/2/17 10:31
 */
@Data
public class ServerClassBase {

    @JacksonXmlProperty(localName = "ID")
    private int id;

    @JacksonXmlProperty(localName = "Name")
    private String name;

    @JacksonXmlProperty(localName = "Status")
    private int status;

    @JacksonXmlElementWrapper(localName = "SubServerList")
    @JacksonXmlProperty(localName = "SubServerClass")
    private List<SubServerClass> subServerClassList;

}
