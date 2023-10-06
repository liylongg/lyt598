package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.util.DisCmdSendUtils;
import com.tct.itd.basedata.dfsread.service.handle.ConStationInfoService;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author yuzhenxin
 * @date 2022-10-11 16:38:04
 * @desc 发送空调电子调度命令
 */
@Service(value = "sendDisCmdAirCondition")
@Slf4j
public class SendDisCmdAirConditionHandler implements AidDecSubStepHandler {
    @Resource
    private DisCmdSendUtils disCmdSendUtils;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private ConStationInfoService conStationInfoService;
    @Resource
    private AppPushService appPushService;

    /**
     * @param alarmInfo
     * @param dto
     * @return void
     * @Description 发送车站类型：
     * 1、故障车站
     * 2、所有车站
     * 3、所有车辆段
     * 4、所有车辆调
     * 5、所有车辆段与车辆调ID
     * 6、终点站受领处所
     * 7、下一站站受领处所
     * 8、折返站
     * @Author yuelei
     * @Date 2021/9/30 12:20
     */
    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        if (Objects.equals(AlarmTypeConstant.AIR_CONDITIONING_FAILURE, String.valueOf(alarmInfo.getAlarmTypeDetail()))&&
                Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())){
            return;
        }

        String station = disCmdSendUtils.getStationByAcceptType(alarmInfo, dto.getAcceptStation());
        log.info("开始发送调度命令，受令处所为：{}", station);
        String disCmdContent = dto.getDisCmdContent();
        disCmdSendUtils.sendDisCmd(alarmInfo, disCmdContent, station);
        log.info("发送刷新电子调度命");
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
        log.info("调度命令发送成功");
    }
}
