package com.tct.itd.adm.entity;

import lombok.Data;

/**
 * @author kangyi
 * @description 报警源
 * @date 2022年 06月11日 11:56:00
 */
@Data
public class AlarmSourceDto {

    private Long id;
    //报警源类型1:人工录入2:TCMS3:VOBC4:ATS
    private String alarmType;
    //报警源名称1:人工录入2:TCMS3:VOBC4:ATS
    private String alarmName;
}
