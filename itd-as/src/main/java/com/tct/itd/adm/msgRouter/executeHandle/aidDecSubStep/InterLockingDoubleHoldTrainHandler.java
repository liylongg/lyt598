package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.runGraph.stategy.InterLockingDoubleStrategy;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description : 联锁双机扣车逻辑
 * @Author : zhangjiarui
 * @Date : Created in 2022/4/8
 */
@Slf4j
@Service("interLockingDoubleHoldTrain")
public class InterLockingDoubleHoldTrainHandler implements AidDecSubStepHandler {
    @Resource
    private InterLockingDoubleStrategy interLockingDoubleStrategy;
    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        log.info("联锁双机执行扣车推荐指令,alarmInfo-" + alarmInfo);
        alarmInfo.setEndAlarmTime(interLockingDoubleStrategy.getEndAlarmTime(alarmInfo));
        log.info("开始扣车...");
        aidDecisionExecService.sendHoldTrainForLock(alarmInfo, null);
        log.info("扣车结束");
    }
}