package com.tct.itd.adm.msgRouter.check.router;


import com.tct.itd.dto.AlarmInfo;

/**
 * @Description 录入校验统一父类
 * @Author yuelei
 * @Date 2021/9/15 14:27
 */
public interface CheckConfigHandler {
    void handle(AlarmInfo alarmInfo);
}
