package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.basedata.dfsread.service.handle.PlatformInfoService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.PlatformInfo;
import com.tct.itd.common.dto.TrainCtrlPrv;
import com.tct.itd.common.enums.HoldTrainTypeEnum;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 不调算法依次扣车
 * @Author kangyi
 * @Date 2022/6/9 15:27
 */
@Service("circleHoldTrain")
@Slf4j
public class CircleHoldTrainHandler implements AidDecSubStepHandler {

    @Resource
    private PlatformInfoService platformInfoService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        log.info("执行扣车推荐指令,alarmInfo:{},AidDesSubStepOutDto:{}",
                JsonUtils.toJSONString(alarmInfo), JsonUtils.toJSONString(dto));
        String holdParam = dto.getParam();
        if (StringUtils.isEmpty(holdParam)) {
            throw new BizException("扣车参数为空");
        }
        String platformId = alarmInfo.getPlatformId();
        if (StringUtils.isEmpty(platformId)) {
            throw new BizException("未设置扣车站台");
        }
        TrainCtrlPrv trainCtrlPrv = new TrainCtrlPrv();

        trainCtrlPrv.setAlarmInfoId(String.valueOf(alarmInfo.getTableInfoId()));
        trainCtrlPrv.setType(IidsConstPool.COMMAND_HOLD_TRAIN);
        setPlatformIdUpOrDown(trainCtrlPrv, holdParam, alarmInfo);
        log.info("开始存入扣车缓存TrainCtrlPrv:{}", JsonUtils.toJSONString(trainCtrlPrv));
        BasicCommonCacheUtils.set(Cache.HOLD_TRAIN_STATION, trainCtrlPrv);
    }

    private void setPlatformIdUpOrDown(TrainCtrlPrv trainCtrlPrv, String holdParam, AlarmInfo alarmInfo) {
        HoldTrainTypeEnum holdTrainTypeEnum = HoldTrainTypeEnum.getByType(holdParam);
        switch (holdTrainTypeEnum) {
            case CURRENT_PLATFORM:
                List<Integer> platforms = new ArrayList<>();
                platforms.add(Integer.parseInt(alarmInfo.getPlatformId()));
                trainCtrlPrv.setHoldTrainTimerPlatformIds(platforms);
                trainCtrlPrv.setUpDown(String.valueOf(alarmInfo.getUpDown()));
                return;
            case NO_CURRENT_PLATFORM:
                List<Integer> platformIds = platformInfoService.getLastPlatformIdByPId(Integer.parseInt(alarmInfo.getPlatformId()),
                        alarmInfo.getUpDown());
                log.info("获取电子地图扣车的下一个站台集合{},当前站台id【{}】",JsonUtils.toJSONString(platformIds),alarmInfo.getPlatformId());
                PlatformInfo prePlatform = platformInfoService.getPlatformByPId(platformIds.get(0));
                trainCtrlPrv.setUpDown(String.valueOf(prePlatform.getDirection()));
                trainCtrlPrv.setHoldTrainTimerPlatformIds(platformIds);
                return;
            default:
                throw new BizException("数据库扣车参数枚举类型不存在");
        }
    }

}
