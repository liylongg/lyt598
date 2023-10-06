package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description 故障恢复推荐指令-车门取消扣车
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("doorCancelHoldTrain")
@Slf4j
public class DoorCancelHoldTrainHandler implements AidDecSubStepHandler {
    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Override
    @Async("taskExecutor")
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        log.info("删除扣车缓存");
        BasicCommonCacheUtils.delKey(Cache.HOLD_TRAIN_STATION);
        BasicCommonCacheUtils.delKey(Cache.HOLD_TRAIN_FLAG);
        //抬车
        log.info("准备取消扣车");
        aidDecisionExecService.offHoldTrainTrainDoor(alarmInfo.getTableInfoId());
        log.info("取消扣车结束");
    }
}
