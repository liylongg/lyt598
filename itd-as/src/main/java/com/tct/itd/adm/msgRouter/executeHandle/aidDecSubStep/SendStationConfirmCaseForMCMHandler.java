package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.dto.StationConfirmDto;
import com.tct.itd.common.dto.StationConfirmWinDto;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Description 推送车站对话框
 * @Author zhoukun
 * @Date 2022/7/20 15:25
 */
@Service("sendStationConfirmCaseForMCM")
@Slf4j
public class SendStationConfirmCaseForMCMHandler implements AidDecSubStepHandler {
    @Resource
    private AppPushService appPushService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        //给车站推送对话框-方案
        List<StationConfirmWinDto> list = new ArrayList<>();
        String subStepContent = dto.getSubStepContent();
        String[] split = subStepContent.split(";");
        int code = 1;
        //牵引在折返区间特殊处理
        if (Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
            StationConfirmWinDto winDto = new StationConfirmWinDto(2, split[1].replace("2","1"));
            list.add(winDto);
        }else {
            for(String content : split){
                StationConfirmWinDto winDto = new StationConfirmWinDto(code, content);
                list.add(winDto);
                code++;
            }
        }
        //没有倒计时
        appPushService.sendMessage(MsgTypeEnum.STATION_CONFIRM_CASE,
                new WebNoticeDto(CommandEnum.STATION_CONFIRM_CASE.getMsgCode(), "0",
                        new StationConfirmDto(-1, TitleUtil.getTitle(alarmInfo), list)));
        //推送给车站的弹窗，缓存用于故障放弃时关闭车站弹窗
        BasicCommonCacheUtils.set(Cache.STATION_CONFIRM_IDS, "0");
    }
}
