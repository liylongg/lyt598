package com.tct.itd.adm.msgRouter.executeHandle.common.step;


import com.google.common.collect.Lists;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmStationService;
import com.tct.itd.adm.util.DisCmdSendUtils;
import com.tct.itd.basedata.dfsread.service.handle.*;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * @classname: SendDisCmdByStationHandler
 * @description: 根据给定的站点id发送调度命令
 * @author: liyunlong
 * @date: 2022/3/8 16:16
 */
@Service(value = "sendDisCmdByStationHandler")
@Slf4j
public class SendDisCmdByStationHandler implements AidDecSubStepHandler {

    @Resource
    private DisCmdSendUtils disCmdSendUtils;

    @Resource
    private AdmStationService admStationService;

    @Resource
    private StopRegionDataService stopRegionDataService;

    @Resource
    private AppPushService appPushService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        String switchNo = alarmInfo.getSwitchNo();
        if (BasicCommonCacheUtils.existHash(Cache.TRAIN_NUMBER_LIST, switchNo) && BasicCommonCacheUtils.existHash(Cache.NEXT_STOP_REGION_ID,
                switchNo)) {
            List<String> trainNumberList = (List<String>) BasicCommonCacheUtils.hGet(Cache.TRAIN_NUMBER_LIST, switchNo);
            String trainNumber = String.join("、", trainNumberList);
            Integer nextStopRegionId = (Integer) BasicCommonCacheUtils.hGet(Cache.NEXT_STOP_REGION_ID, switchNo);
            Integer stationId = stopRegionDataService.getStationIdByStopAreaId(nextStopRegionId);
            String stationName = admStationService.selectByStationId(stationId).getStationName();
            String disCmdContent = String.format(dto.getDisCmdContent(), alarmInfo.getSwitchName(), trainNumber, stationName);
            disCmdSendUtils.sendDisCmd(alarmInfo, disCmdContent, String.valueOf(stationId));
            appPushService.sendWebNoticeMessageToAny(
                    new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
            // 电子调度命令发送成功后,移除缓存
            BasicCommonCacheUtils.delKey(Cache.TRAIN_NUMBER_LIST);
            BasicCommonCacheUtils.delKey(Cache.NEXT_STOP_REGION_ID);
        }

        if (BasicCommonCacheUtils.existHash(Cache.SLOWLY_TRAIN_NUMBER, switchNo) && BasicCommonCacheUtils.existHash(Cache.SLOWLY_NEXT_STOP_REGIONID, switchNo) && Boolean.TRUE.equals(BasicCommonCacheUtils.existHash(Cache.SLOWLY_NEXT_STATION_NAME, switchNo))) {
            String slowlyTrainNumber = (String) BasicCommonCacheUtils.hGet(Cache.SLOWLY_TRAIN_NUMBER, switchNo);
            String slowyNextStopReginId = (String) BasicCommonCacheUtils.hGet(Cache.SLOWLY_NEXT_STOP_REGIONID, switchNo);
            String stationName = (String) BasicCommonCacheUtils.hGet(Cache.SLOWLY_NEXT_STATION_NAME, switchNo);
            String stationId;
            String[] stopReginIdArr = slowyNextStopReginId.split(",");
            if (stopReginIdArr.length == NumConstant.ONE) {
                stationId = String.valueOf(stopRegionDataService.getStationIdByStopAreaId(Integer.parseInt(slowyNextStopReginId)));
            } else {
                List<String> stationIdList = Lists.newArrayList();
                Arrays.asList(stopReginIdArr).forEach(s -> stationIdList.add(String.valueOf(stopRegionDataService.getStationIdByStopAreaId(Integer.valueOf(s)))));
                stationId = StringUtils.join(stationIdList, ",");
            }
            String disCmdContent = String.format(dto.getDisCmdContent(), alarmInfo.getSwitchName(), slowlyTrainNumber
                    , stationName);
            disCmdSendUtils.sendDisCmd(alarmInfo, disCmdContent, String.valueOf(stationId));
            appPushService.sendWebNoticeMessageToAny(
                    new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
            // 电子调度命令发送成功后,移除缓存
            BasicCommonCacheUtils.delKey(Cache.SLOWLY_TRAIN_NUMBER);
            BasicCommonCacheUtils.delKey(Cache.SLOWLY_NEXT_STOP_REGIONID);
            BasicCommonCacheUtils.delKey(Cache.SLOWLY_NEXT_STATION_NAME);
        }
    }
}
