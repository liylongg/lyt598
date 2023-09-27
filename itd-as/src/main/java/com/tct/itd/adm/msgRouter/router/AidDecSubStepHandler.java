package com.tct.itd.adm.msgRouter.router;

import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;

/**
 * @Description 执行单元统一父类
 * @Author yuelei
 * @Date 2021/9/15 14:27
 */
public interface AidDecSubStepHandler {
    void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto);
}
