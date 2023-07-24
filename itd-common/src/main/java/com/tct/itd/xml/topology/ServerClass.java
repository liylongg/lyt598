package com.tct.itd.xml.topology;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

/**
 * @classname: ServerClass
 * @description:
 * @author: liyunlong
 * @date: 2023/2/17 10:47
 */
@Data
public class ServerClass extends ServerClassBase{


    @JacksonXmlElementWrapper(localName = "SubServerList")
    @JacksonXmlProperty(localName = "SubServerClass")
    private List<SubServerClass> subServerClassList;

}
