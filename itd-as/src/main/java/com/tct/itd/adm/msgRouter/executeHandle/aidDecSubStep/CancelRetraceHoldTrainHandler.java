package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.basedata.dfsread.service.handle.PlatformInfoService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.dto.*;
import com.tct.itd.common.enums.CommandTypeEnum;
import com.tct.itd.common.enums.TrainStateEnum;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.tias.service.AtsByteCommandFlatService;
import com.tct.itd.tias.service.SendNotifyService;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Description 车门无法关闭-取消折返站扣车
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("cancelRetraceHoldTrain")
@Slf4j
public class CancelRetraceHoldTrainHandler implements AidDecSubStepHandler {
    @Resource
    private AlarmUtil alarmUtil;
    @Resource
    private AtsByteCommandFlatService atsByteCommandFlatClient;
    @Resource
    private SendNotifyService sendNotifyService;
    @Resource
    private PlatformInfoService platformInfoService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto outDto) {
        //获取大小交路对应站台停车区域信息
        PlatformStopAreaListDto listDto = (PlatformStopAreaListDto) BasicCommonCacheUtils.get(Cache.PLATFORM_STOP_AREA_LIST, PlatformStopAreaListDto.class);
        if(Objects.isNull(listDto) || listDto.getPlatformStopAreaList().isEmpty()){
            log.info("未获取大小交路停车区域信息");
            return;
        }
        //获取对应站台编号
        List<Integer> platformIdList =  platformInfoService.getPlatformIdList(listDto.getPlatformStopAreaList());
        //将大小交路涉及站台，已扣车的取消扣车
        long tableInfoId = alarmInfo.getTableInfoId();
        Map<Object, Object> holdTrainMap = BasicCommonCacheUtils.hmget(Cache.HOLD_AND_OFF_TRAIN,HoldOffTrainTimeDto.class);
        for (Map.Entry<Object, Object> entry : holdTrainMap.entrySet()) {
            HoldOffTrainTimeDto holdOffTrainTimeDto = (HoldOffTrainTimeDto) entry.getValue();
            //如果该站台已扣车，并且属于小交路涉及的站台
            if(holdOffTrainTimeDto.getTableInfoId() == tableInfoId && holdOffTrainTimeDto.getIsHold().equals(NumConstant.ONE)){
                if ( platformIdList.contains(Integer.parseInt((String) entry.getKey()))) {
                    //到达抬车时间 开始抬车
                    TrainCtrlPrv trainCtrl = alarmUtil.getTrainCtrlPrv(Integer.parseInt((String) entry.getKey()) ,0);
                    trainCtrl.setAlarmInfoId(String.valueOf(tableInfoId));
                    if (trainCtrl.getPlatformId() == -1) {
                        BasicCommonCacheUtils.delMapKey(Cache.HOLD_AND_OFF_TRAIN, String.valueOf(entry.getKey()));
                        log.info("获取的站台未-1，取消抬车操作:{}", entry.getKey());
                        continue;
                    }
                    PlatformInfo platformInfo = alarmUtil.getPlatformInfo(trainCtrl.getPlatformId());
                    NotifyParam notifyParam = new NotifyParam();
                    notifyParam.setPlatformId(trainCtrl.getPlatformId());
                    notifyParam.setInfoId(tableInfoId);
                    notifyParam.setMsgPushEnum(MsgPushEnum.START_OFF_HOLD_TRAIN_MSG);
                    //通知开始抬车
                    sendNotifyService.sendNotify(notifyParam);
                    //下发抬车指令
                    AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
                    atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.HOLD_TRAIN);
                    atsByteCmdData.setParameter(TrainStateEnum.TRAIN_DOOR_CLOSE.getValue());
                    atsByteCmdData.setT(JsonUtils.toJSONString(platformInfo));
                    atsByteCmdData.setTrainCtrl(trainCtrl);
                    log.info("下发抬车指令至ats");
                    atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
                    BasicCommonCacheUtils.delMapKey(Cache.HOLD_AND_OFF_TRAIN, String.valueOf(entry.getKey()));
                }
            }
        }
        log.info("抬车结束");
        //循环扣车标志
        BasicCommonCacheUtils.delKey(Cache.HOLD_TRAIN_STATION);
        //取消折返点扣车标志
        BasicCommonCacheUtils.delKey(Cache.HOLD_TRAIN_FLAG);
        //取消折返扣车后，将折返站台集合删除
        BasicCommonCacheUtils.delKey(Cache.PLATFORM_STOP_AREA_LIST);
    }
}
