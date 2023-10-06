package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import org.springframework.stereotype.Service;

/**
 * @classname: ApdcnoOpenDoorRemoteHandler
 * @description: 整侧站台门无法打开，远程开门handler
 * @author: liyunlong
 * @date: 2021/10/8 15:30
 */
@Service("apdcnoOpenDoorRemote")
public class ApdcnoOpenDoorRemoteHandler implements AidDecSubStepHandler {

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {

    }
}
