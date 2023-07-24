package com.tct.itd.xml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @Description
 * @Author zhaoke
 * @Date 2020/5/26 15:08
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "TrainGraphRoot")
@XmlAccessorType(XmlAccessType.FIELD)
public class TrainGraphRoot {

    @XmlElement(name = "TrainGraph")
    private TrainGraph trainGraph;
}
