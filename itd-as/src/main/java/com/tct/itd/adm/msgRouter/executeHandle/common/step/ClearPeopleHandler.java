package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.runGraph.stategy.IPrePlanRunGraphStrategy;
import com.tct.itd.basedata.dfsread.service.handle.PlatformInfoService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.dto.AtsByteCmdData;
import com.tct.itd.common.dto.NotifyParam;
import com.tct.itd.common.dto.TrainCtrlPrv;
import com.tct.itd.common.dto.TrainNumberAdjust;
import com.tct.itd.common.enums.CommandTypeEnum;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.tias.service.AtsByteCommandFlatService;
import com.tct.itd.tias.service.SendNotifyService;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Description 执行推荐指令-清人掉线
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("clearPeople")
@Slf4j
public class ClearPeopleHandler implements AidDecSubStepHandler {
    @Resource
    private List<IPrePlanRunGraphStrategy> graphDataStrategyList;
    private Map<String, IPrePlanRunGraphStrategy> graphDataStrategyHashMap = new HashMap<>();
    @Resource
    private AtsByteCommandFlatService atsByteCommandClient;
    @Resource
    private PlatformInfoService platformInfoService;
    @Resource
    private SendNotifyService sendNotifyService;

    /**
     * @Description 建立映射关系
     * @Author yuelei
     * @date 2021/9/30 15:26
     */
    @PostConstruct
    public void init() {
        graphDataStrategyList.forEach(
                graphDataStrategy -> graphDataStrategyHashMap.put(graphDataStrategy.strategy(), graphDataStrategy)
        );
    }

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        if (BasicCommonCacheUtils.exist(Cache.TRAIN_NUMBER_ADJUST)) {
            for (Map.Entry<Object, Object> m : BasicCommonCacheUtils.hmget(Cache.TRAIN_NUMBER_ADJUST,TrainNumberAdjust.class,List.class).entrySet()) {
                //如果不是当前应急事件对应调整信息则返回
                if (!m.getKey().equals(String.valueOf(alarmInfo.getTableInfoId()))) {
                    continue;
                }
                List<TrainNumberAdjust> trainNumberAdjustList = (List<TrainNumberAdjust>) m.getValue();
                Iterator<TrainNumberAdjust> iterator = trainNumberAdjustList.iterator();
                while (iterator.hasNext()) {
                    TrainNumberAdjust trainNumberAdjust = iterator.next();
                    log.info("车次调整信息为trainNumberAdjust：{}", JsonUtils.toJSONString(trainNumberAdjust));
                    if (trainNumberAdjust.getCleanPassengerStopAreaId() == null
                            || trainNumberAdjust.getCleanPassengerStopAreaId() == 0
                            || StringUtils.isEmpty(trainNumberAdjust.getTrainId())) {
                        continue;
                    }
                    log.info("车次调整-车组号与故障车匹配,准备清客,车组号:{}", alarmInfo.getTrainId());
                    //发送清客指令
                    String platFormId = Integer.toString(platformInfoService.getPlatformIdByStopArea(trainNumberAdjust.getCleanPassengerStopAreaId()));
                    if ("-1".equals(platFormId)) {
                        log.error("未查找到platFormId,trainNumberAdjust:{}", JsonUtils.toJSONString(trainNumberAdjust));
                        continue;
                    }
                    NotifyParam notifyParam = new NotifyParam();
                    notifyParam.setPlatformId(Integer.parseInt(platFormId));
                    notifyParam.setInfoId(alarmInfo.getTableInfoId());
                    notifyParam.setMsgPushEnum(MsgPushEnum.TRAIN_CLEAR_PASSENGER_CMD_LOG_MSG);
                    notifyParam.setOrderNo(trainNumberAdjust.getTrainOrder());
                    //通知开始抬车
                    sendNotifyService.sendNotify(notifyParam);
                    TrainCtrlPrv trainCtrlPrv = new TrainCtrlPrv();
                    trainCtrlPrv.setAlarmInfoId(String.valueOf(alarmInfo.getTableInfoId()));
                    trainCtrlPrv.setOrderNo(trainNumberAdjust.getTrainOrder());
                    trainCtrlPrv.setPlatformId(Integer.parseInt(platFormId));
                    log.info("发送清客指令至ATS");
                    AtsByteCmdData atsByteCmdData = new AtsByteCmdData(CommandTypeEnum.CLEAR_TRAIN_RETURN, JsonUtils.toJSONString(trainNumberAdjust), trainCtrlPrv, Integer.parseInt(platFormId));
                    //下发清客指令
                    atsByteCommandClient.sendCommandTrain(atsByteCmdData);
                    //删除该条调整信息
                    iterator.remove();
                    BasicCommonCacheUtils.hPut(Cache.TRAIN_NUMBER_ADJUST, (String) m.getKey(), trainNumberAdjustList);
                }
            }
        }
    }
}
