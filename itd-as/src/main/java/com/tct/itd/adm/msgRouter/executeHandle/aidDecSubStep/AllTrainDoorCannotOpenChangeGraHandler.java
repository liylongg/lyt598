package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description 全列车门无法打开立刻清人掉线-调整运行图
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("allTrainDoorCannotOpenChangeGra")
@Slf4j
public class AllTrainDoorCannotOpenChangeGraHandler implements AidDecSubStepHandler {
    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        //立刻掉线
        alarmInfo.setAlarmState(AlarmStateEnum.STATION_DROP_LINE.getCode());
        //调掉线图
        aidDecisionExecService.changeGraph(alarmInfo);
    }
}
