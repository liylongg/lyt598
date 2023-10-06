package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.runGraph.stategy.SignalElectricSourceStrategy;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description : 信号电源扣车逻辑
 * @Author : zhoukun
 * @Date : Created in 2022/4/19
 */
@Slf4j
@Service("signalElectricSourceHoldTrain")
public class SignalElectricSourceHoldTrainHandler implements AidDecSubStepHandler {
    @Resource
    private SignalElectricSourceStrategy signalElectricSourceStrategy;
    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        log.info("信号电源执行扣车推荐指令,alarmInfo-" + alarmInfo);
        alarmInfo.setEndAlarmTime(signalElectricSourceStrategy.getEndAlarmTime(alarmInfo));
        log.info("开始扣车...");
        aidDecisionExecService.sendHoldTrainForSignalElec(alarmInfo, null);
        log.info("扣车结束");
    }
}