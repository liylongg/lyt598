package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.dto.*;
import com.tct.itd.enums.PlanRunGraphEnum;
import com.tct.itd.hub.service.PlanRunGraphService;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description 故障恢复推荐指令-晚点-取消扣车
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("trainDoorCancelHoldTrain")
@Slf4j
public class TrainDoorCancelHoldTrainHandler implements AidDecSubStepHandler {
    @Resource
    
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private PlanRunGraphService planRunGraphService;
    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    @Override
    @Async("taskExecutor")
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        //需要先判断故障站是否是终点站
        //获取车次追踪数据
        TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent(AlarmUtil.getTrainId(alarmInfo));
        String serverNumber = tiasTraceInfo.getServerNumber();
        String trainNumber = tiasTraceInfo.getOrderNumber();
        Integer stopAreaNumber = tiasTraceInfo.getStopAreaNumber();
        AtomicBoolean isEndStation = new AtomicBoolean(false);
        //获取运行图
        TrainGraphDto trainGraphDto = (TrainGraphDto) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.DTO);
        List<ServerNumberDto> serverNumbers = trainGraphDto.getServerNumbers();
        if (CollectionUtils.isNotEmpty(serverNumbers)) {
            for(ServerNumberDto s : serverNumbers){
                if (Integer.parseInt(s.getId()) == Integer.parseInt(serverNumber)) {
                    List<TrainNumberDto> tns = s.getTrainNumbers();
                    if (CollectionUtils.isNotEmpty(tns)) {
                        for (TrainNumberDto tn : tns){
                            if (trainNumber.equals(tn.getOrderNumber())) {
                                List<TrainDto> ts = tn.getTrains();
                                if(ts.size() <= 1){
                                    isEndStation.set(true);
                                }
                                if(stopAreaNumber.equals(ts.get(ts.size() -2).getStopAreaID())){
                                    isEndStation.set(true);
                                }
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        //终点站先调图，再抬车，非终点站先抬车再调图
        if(isEndStation.get()){
            log.info("当前站为终点站，暂不取消扣车，等待调图完成后取消");
            return;
        }
        log.info("删除扣车缓存");
        BasicCommonCacheUtils.delKey(Cache.HOLD_TRAIN_STATION);
        //抬车
        log.info("准备开始抬车");
        aidDecisionExecService.offHoldTrainCheckEnd(alarmInfo.getTableInfoId());
        log.info("抬车完毕");
    }
}
