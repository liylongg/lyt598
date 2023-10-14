package com.tct.itd.adm.msgRouter.handle;


import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.dto.AlarmInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @classname: SwitchFailureHandler
 * @description: 道岔故障handler
 * @author: liyunlong
 * @date: 2021/12/29 14:35
 */
@Slf4j
@Component
public class SwitchFailureHandler implements AlarmInfoMessageHandler {

    /**
     * 数字常量 0
     */
    public static final Integer ZERO_NUM = 0;

    /**
     * 数字常量 1
     */
    public static final Integer ONE_NUM = 1;


    @Override
    public void handle(AlarmInfo alarmInfo) {

    }

    @Override
    public String channel() {
        return AlarmTypeConstant.SWITCH_FAILURE;
    }
}
