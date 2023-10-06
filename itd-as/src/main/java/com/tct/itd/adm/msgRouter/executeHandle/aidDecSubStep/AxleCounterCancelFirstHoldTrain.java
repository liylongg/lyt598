package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.client.AlgSwitchClient;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.*;
import com.tct.itd.common.enums.CommandTypeEnum;
import com.tct.itd.common.enums.TrainStateEnum;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.enums.PlanRunGraphEnum;
import com.tct.itd.hub.service.PlanRunGraphService;
import com.tct.itd.tias.service.AtsByteCommandFlatService;
import com.tct.itd.tias.service.SendNotifyService;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author kangyi
 * @description 计轴取消第一辆车的扣车
 * @date 2022年 10月26日 09:41:57
 */
@Service("axleCounterCancelFirstHoldTrain")
@Slf4j
public class AxleCounterCancelFirstHoldTrain implements AidDecSubStepHandler {

    @Resource
    private AlgSwitchClient algSwitchClient;

    @Resource
    private PlanRunGraphService planRunGraphService;

    @Resource
    private AlarmUtil alarmUtil;

    @Resource
    private SendNotifyService sendNotifyService;

    @Resource
    private AtsByteCommandFlatService atsByteCommandFlatClient;

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        log.info("执行计轴取消来车方向第一辆车的扣车推荐指令,alarmInfo:{},AidDesSubStepOutDto:{}",
                JsonUtils.toJSONString(alarmInfo), JsonUtils.toJSONString(dto));
        if (CollectionUtils.isEmpty(trainTraceCache.asMap())) {
            return;
        }
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        //获取计轴算法参数
        AlgorithmData algorithmData = getAxleCounterAlgorithmData(alarmInfo1);
        algorithmData.setNowTime(DateUtil.getTimeStamp());
        //调用算法，获取离故障计轴最近的一辆车车组号
        String trainId = algSwitchClient.getAxleFirstTrain(algorithmData).getData();
        if (StringUtils.isEmpty(trainId)) {
            log.info("计轴预复位成功后，算法返回离计轴最近的车为空");
            return;
        }
        TiasTraceInfo traceInfo = trainTraceCache.getIfPresent(trainId);
        if (Objects.isNull(traceInfo)) {
            return;
        }
        //设置扣车标志不再继续扣车
        BasicCommonCacheUtils.set(Cache.AXIS_RESET_SUCCESS, trainId);
        //列车非停稳在站台
        if (!IidsConstPool.STAND_STILL.equals(traceInfo.getTrainArrFlg())) {
            return;
        }
        //抬车
        offHoldTrain(trainId);
    }

    //获取算法参数
    private AxleCounterAlgorithmData getAxleCounterAlgorithmData(AlarmInfo alarmInfo) {
        //获取运行图压缩包
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        //获取运行图
        String planGraph = (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH);
        //获取车次追踪数据
        List<TiasTraceInfo> tiasTraceInfos = new ArrayList<>(trainTraceCache.asMap().values());
        //故障转移至非道岔区段计轴故障
        if (BasicCommonCacheUtils.exist(Cache.ALARM_UPGRADE)) {
            AlarmUpgrade alarmUpgrade = (AlarmUpgrade) BasicCommonCacheUtils.hGet(Cache.ALARM_UPGRADE, AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH, AlarmUpgrade.class);
            alarmInfo.setAxleCounterId(alarmUpgrade.getAxleCounterId());
        }
        return new AxleCounterAlgorithmData(zipPlanRunGraph, alarmInfo, new ArrayList<>(), tiasTraceInfos, planGraph, 0, DateUtil.getTimeStamp(), DateUtil.getTimeStamp(), 0);
    }

    //抬车
    private void offHoldTrain(String trainId) {
        String platformId = null;
        for (Map.Entry<Object, Object> entry : BasicCommonCacheUtils.hmget(Cache.HOLD_AND_OFF_TRAIN,HoldOffTrainTimeDto.class).entrySet()) {
            HoldOffTrainTimeDto dto = (HoldOffTrainTimeDto) entry.getValue();
            //该车已经扣住在站台上
            if (!Objects.isNull(dto) && dto.getTrainId().equals(trainId) && dto.getIsHold().equals(1)) {
                platformId = (String) entry.getKey();
                log.info("扣车信息：{}", JsonUtils.toJSONString(dto));
                TrainCtrlPrv trainCtrl = alarmUtil.getTrainCtrlPrvForTrainDoor(Integer.parseInt(platformId), 0);
                trainCtrl.setAlarmInfoId(String.valueOf(dto.getTableInfoId()));
                if (trainCtrl.getPlatformId() == -1) {
                    BasicCommonCacheUtils.delMapKey(Cache.HOLD_AND_OFF_TRAIN, platformId);
                    log.info("获取的站台未-1，取消抬车操作:{}", entry.getKey());
                    continue;
                }
                PlatformInfo platformInfo = alarmUtil.getPlatformInfo(trainCtrl.getPlatformId());
                //推送前端通知
                NotifyParam notifyParam = new NotifyParam();
                notifyParam.setMsgPushEnum(MsgPushEnum.START_OFF_HOLD_TRAIN_MSG);
                notifyParam.setInfoId(dto.getTableInfoId());
                notifyParam.setPlatformId(trainCtrl.getPlatformId());
                sendNotifyService.sendNotify(notifyParam);
                //下发抬车指令
                AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
                atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.HOLD_TRAIN);
                atsByteCmdData.setParameter(TrainStateEnum.TRAIN_DOOR_CLOSE.getValue());
                atsByteCmdData.setT(JsonUtils.toJSONString(platformInfo));
                atsByteCmdData.setTrainCtrl(trainCtrl);
                log.info("下发抬车指令至ats");
                atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
                break;
            }
        }
        if (!StringUtils.isEmpty(platformId)) {
            BasicCommonCacheUtils.delMapKey(Cache.HOLD_AND_OFF_TRAIN, platformId);
        }
    }

}
