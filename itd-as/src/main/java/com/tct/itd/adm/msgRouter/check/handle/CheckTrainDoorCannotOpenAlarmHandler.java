package com.tct.itd.adm.msgRouter.check.handle;

import com.tct.itd.adm.msgRouter.check.router.CheckConfigHandler;
import com.tct.itd.basedata.dfsread.service.handle.DoorInfoService;
import com.tct.itd.basedata.dfsread.service.handle.PlatformInfoService;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.constant.NumStrConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author yuelei
 * @Desc 校验是否存在vobc-超时未打开报警帧
 * @Date 11:14 2023/2/20
 */
@Slf4j
@Service("checkTrainDoorCannotOpenAlarm")
public class CheckTrainDoorCannotOpenAlarmHandler implements CheckConfigHandler {

    @Resource(name = "atoInfoCache")
    private com.github.benmanes.caffeine.cache.Cache<String, List<String>> atoInfoCache;

    @Resource(name = "trainPlatformDoorStateCache")
    private com.github.benmanes.caffeine.cache.Cache<String, Integer> trainPlatformDoorStateCache;

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    @Resource(name = "ciPlatformDoorStateCache")
    private com.github.benmanes.caffeine.cache.Cache<String, String> ciPlatformDoorStateCache;

    @Resource
    private PlatformInfoService platformInfoService;

    @Resource
    private DoorInfoService doorInfoService;

    private final static int NUM_85 = 85;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        ConcurrentMap<String, List<String>> atoMap = atoInfoCache.asMap();
        if (CollectionUtils.isEmpty(atoMap)) {
            throw new BizException(CodeEnum.NO_GET_TRAIN_ATO_MSG);
        }
        if (CollectionUtils.isEmpty(atoMap.get(alarmInfo.getTrainId()))) {
            throw new BizException(CodeEnum.NO_GET_CURRENT_TRAIN_ATO_MSG);
        }
        //获取到VOBC报警帧数据
        List<String> atoList = atoMap.get(alarmInfo.getTrainId());
        log.info("当前列车报警帧信息：{}", atoList.toString());
        // 车门超时未打开
        String trainDoorNotOpenVobc = atoList.get(NumConstant.FIVE);
        if (NumStrConstant.ONE.equals(trainDoorNotOpenVobc)) {
            log.info("整侧车门无法打开故障");
            return;
        }
        this.checkPlatformTrainDoorAlarm(alarmInfo.getTrainId());
    }

    public void checkPlatformTrainDoorAlarm(String tranId){
        ConcurrentMap<String, Integer> trainPlatformDoorState = trainPlatformDoorStateCache.asMap();
        Integer state = trainPlatformDoorState.get(tranId);
        log.info("车门屏蔽门故障报警取值:{}", state);
        if(Objects.isNull(state) || state != NUM_85){
            throw new BizException(CodeEnum.NOT_GET_PLATFORM_TRAIN_DOOR_ALARM);
        }
        TiasTraceInfo traceInfo = trainTraceCache.getIfPresent(tranId);
        if(Objects.isNull(traceInfo.getTrainDoorState()) || traceInfo.getTrainDoorState() != NUM_85){
            throw new BizException(CodeEnum.TRAIN_DOOR_STATE_NOT_OPEN);
        }
        //获取屏蔽门打开状态
        Integer stopAreaNumber = traceInfo.getStopAreaNumber();
        log.info("停车区域ID：{}", stopAreaNumber);
        //获取站台id
        Integer platformId = platformInfoService.getPlatformIdByStopArea(stopAreaNumber);
        if (Objects.isNull(platformId)) {
            log.error("根据车次追踪数据中的停车区域编号{},获取站台id失败！", stopAreaNumber);
            throw new BizException(CodeEnum.NO_GET_PLATFORM_ID_ERROR_BY_TRAIN_TRANCE);
        }
        log.debug("根据车次追踪数据中的停车区域编号{},获取到站台{}", stopAreaNumber, platformId);
        //获取站台门编号
        Integer doorIndex = doorInfoService.getDoorIndexByPlatformId(platformId);
        if (Objects.isNull(doorIndex)) {
            log.error("根据站台id{},获取站台门编号失败！", platformId);
            throw new BizException(CodeEnum.GET_DOOR_INDEX_BY_PLATFORM_ID_FAIL);
        }
        log.info("获取到的屏蔽门编号为：{}", doorIndex);
        String openState = ciPlatformDoorStateCache.getIfPresent(String.valueOf(doorIndex));
        log.info("屏蔽门打开状态：{}", openState);
        if(!Objects.equals(openState, IidsConstPool.OPEN_DOOR_STATUS)){
            throw new BizException(CodeEnum.PLATFORM_STATE_NOT_OPEN);
        }
    }

}
