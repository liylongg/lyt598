package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.basedata.dfsread.service.handle.ConStationInfoService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.ConStationDto;
import com.tct.itd.common.dto.StationConfirmDto;
import com.tct.itd.common.dto.StationConfirmWinDto;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 推送车站对话框
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("sendStationConfirmCase")
@Slf4j
public class SendStationConfirmCaseHandler implements AidDecSubStepHandler {
    @Resource
    private AppPushService appPushService;

    @Resource
    private ConStationInfoService conStationInfoService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        //给车站推送对话框-方案
        List<StationConfirmWinDto> list = new ArrayList<>();
        String subStepContent = dto.getSubStepContent();
        String param = dto.getParam();
        String[] split = subStepContent.split(";");
        int code = 1;
        //aidDesStep.xml中"param"配置起始选项code，配置了使用param，未配置默认从1开始
        if (!StringUtils.isEmpty(param)) {
            code = Integer.parseInt(param);
        }
        for (String content : split) {
            StationConfirmWinDto winDto = new StationConfirmWinDto(code, content);
            list.add(winDto);
            code++;
        }
        //获取车站对话框弹窗倒计时
        String time = SysParamKit.getByCode(IidsSysParamPool.STATION_CONFIRM_WID_TIME);
        String stationId = getStationId(alarmInfo);
        appPushService.sendMessage(MsgTypeEnum.STATION_CONFIRM_CASE,
                new WebNoticeDto(CommandEnum.STATION_CONFIRM_CASE.getMsgCode(), stationId,
                        new StationConfirmDto(Integer.valueOf(time), TitleUtil.getTitle(alarmInfo), list)));
        //推送给车站的弹窗，缓存用于故障放弃时关闭车站弹窗
        BasicCommonCacheUtils.set(Cache.STATION_CONFIRM_IDS, stationId);
    }

    private String getStationId(AlarmInfo alarmInfo) {
        String alarmTypeDetail = String.valueOf(alarmInfo.getAlarmTypeDetail());
        if(alarmTypeDetail.equals(AlarmTypeConstant.AXLE_COUNTER_SWITCH)
            || alarmTypeDetail.equals(AlarmTypeConstant.AXLE_COUNTER_ARB_RESET)
            || alarmTypeDetail.equals(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH_RESET)
            || alarmTypeDetail.equals(AlarmTypeConstant.AXLE_COUNTER_SWITCH_RESET)
        ){
            //弹窗期间不允许故障恢复
            BasicCommonCacheUtils.set(Cache.AXLE_COUNTER_CONFIRM, "1");
            //获取集中站名
            ConStationDto conStationDto = conStationInfoService.getStationByConStationId(Integer.parseInt(alarmInfo.getAlarmConStation()));
            return String.valueOf(conStationDto.getStationId());
        }else {
            return String.valueOf(alarmInfo.getStationId());
        }
    }
}
