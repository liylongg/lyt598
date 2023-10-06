package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.runGraph.stategy.TractionPowerStrategy;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description : 接触网失电扣车逻辑
 * @Author : zhangjiarui
 * @Date : Created in 2022/3/7
 */
@Slf4j
@Service("tractionPowerHoldTrain")
public class TractionPowerHoldTrainHandler implements AidDecSubStepHandler {
    @Resource
    private TractionPowerStrategy tractionPowerStrategy;
    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        log.info("接触网失电执行扣车推荐指令,alarmInfo-" + alarmInfo);
        alarmInfo.setEndAlarmTime(tractionPowerStrategy.getEndAlarmTime(alarmInfo));
        // 算法不需要
        alarmInfo.setAlarmState(tractionPowerStrategy.getAlarmState(alarmInfo));
        log.info("开始扣车...");
        aidDecisionExecService.sendHoldTrainForPower(alarmInfo, null);
        log.info("扣车结束");
    }
}