package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description 车门缓行-调整运行图
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("trainDoorSlowChangeGra")
@Slf4j
public class TrainDoorSlowChangeGraHandler implements AidDecSubStepHandler {

    @Resource
    
    private AidDecisionExecService aidDecisionExecService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        //当前时间为故障结束时间
        alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp(System.currentTimeMillis()));
        alarmInfo.setAlarmState(AlarmStateEnum.TRAIN_DOOR_STATION_DROP_LINE.getCode());
        log.info("准备调整运行图");
        aidDecisionExecService.changeGraphForDoorSlowFailure(alarmInfo);
    }
}
