package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.google.common.collect.Lists;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.HoldTrainService;
import com.tct.itd.basedata.dfsread.service.handle.*;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.enums.HoldTrainTypeEnum;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description : 车门中断扣车逻辑
 * @Author : yuelei
 * @Date : Created in 2022/6/19
 */
@Slf4j
@Service("trainDoorRetraceHoldTrain")
public class TrainDoorRetraceHoldTrainHandler implements AidDecSubStepHandler {
    @Resource
    private HoldTrainService holdTrainService;
    @Resource
    
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private PlatformInfoService platformInfoService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        //获取故障结束时间
        AlarmInfo alarmInfoAll = admAlertInfoSubService.queryOnlyByInfoId(alarmInfo.getTableInfoId());
        alarmInfo.setEndAlarmTime(alarmInfoAll.getEndAlarmTime());
        //获取折返点站台信息
        List<Integer> list = aidDecisionExecService.sendHoldTrainForDoorTrain(alarmInfo);
        //根据站台获取停车区域信息
        List<Integer> platformIdList = platformInfoService.getPlatformIdList(list);
        log.info("获取站台信息集合：{}", platformIdList);
        // platformIdList.parallelStream().forEach(
        //         id -> holdTrainService.holdTrain(id, HoldTrainTypeEnum.CURRENT_PLATFORM.getType()));
        platformIdList.forEach(p -> {
            List<Integer> idList = Lists.newArrayList();
            idList.add(p);
            holdTrainService.holdTrain(idList, HoldTrainTypeEnum.CURRENT_PLATFORM.getType());
        });
        BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_CANCEL_HOLD_TRAIN_FLAG, alarmInfo.getTableInfoId());
    }
}