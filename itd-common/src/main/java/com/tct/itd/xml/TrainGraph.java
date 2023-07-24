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
 * @Date 2020/5/26 15:04
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "TrainGraph")
@XmlAccessorType(XmlAccessType.FIELD)
public class TrainGraph {

    @XmlAttribute(name = "ID")
    private Integer id;

    @XmlAttribute(name = "Name")
    private String name;

    @XmlAttribute(name = "CheckedPass")
    private String checkedPass;

    @XmlAttribute(name = "Version")
    private String version;

    @XmlAttribute(name = "CreateUserID")
    private String createUserID = "";

    @XmlAttribute(name = "CreateTime")
    private String createTime = "2020/12/11 15:08:47";

    @XmlAttribute(name = "UpdateUserID")
    private String updateUserID;

    @XmlAttribute(name = "UpdateTime")
    private String updateTime;

    @XmlAttribute(name = "LineID")
    private String lineID;

    @XmlAttribute(name = "Type")
    private Integer type;

    @XmlAttribute(name = "ExamineUserID")
    private String examineUserID;

    @XmlAttribute(name = "LinkBasicGraph")
    private String linkBasicGraph;

    @XmlAttribute(name = "EditType")
    private String editType;

    @XmlAttribute(name = "IsIntegerity")
    private String isIntegerity;

    @XmlAttribute(name = "Status")
    private String status;

    @XmlAttribute(name = "LinkBasicGraphName")
    private String linkBasicGraphName;

    @XmlAttribute(name = "StatusUpdateUserID")
    private String statusUpdateUserID;

    @XmlAttribute(name = "StatusUpdateTime")
    private String statusUpdateTime = "0001/1/1 0:00:00";

    @XmlAttribute(name = "SignedUserName")
    private String signedUserName;

    @XmlAttribute(name = "CreationSource")
    private String creationSource;

    @XmlAttribute(name = "Reserve")
    private String reserve;

    @XmlElement(name = "ServerNumber")
    private List<ServerNumber> serverNumbers;
}
