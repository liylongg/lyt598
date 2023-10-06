package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.iconstant.ReplaceNameConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.adm.util.DisCmdSendUtils;
import com.tct.itd.basedata.dfsread.service.handle.*;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.dto.ConStationDto;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description 发送电子调度命令
 * @Author yuelei
 * @Date 2021/9/15 14:25
 */
@Service(value = "sendDisCmd")
@Slf4j
public class SendDisCmdHandler implements AidDecSubStepHandler {

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
        String station = disCmdSendUtils.getStationByAcceptType(alarmInfo, dto.getAcceptStation());
        log.info("开始发送调度命令，受令处所为：{}", station);
        String disCmdContent = dto.getDisCmdContent();
        //替换车组号
        if(disCmdContent.contains(ReplaceNameConstant.TRAIN_NUM)){
            disCmdContent = disCmdContent.replaceAll(ReplaceNameConstant.TRAIN_NUM, alarmInfo.getTrainId());
        }
        if (disCmdContent.contains(ReplaceNameConstant.ALARM_STATION)) {
            String alarmStation = alarmInfo.getAlarmSite().contains("_") ? alarmInfo.getAlarmSite().substring(alarmInfo.getAlarmSite().indexOf("_") + 1) : alarmInfo.getAlarmSite();
            disCmdContent = disCmdContent.replaceAll(ReplaceNameConstant.ALARM_STATION, alarmStation);
        }
        if (disCmdContent.contains(ReplaceNameConstant.ALARM_NAME)) {
            String alarmDescribe = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
            disCmdContent = disCmdContent.replaceAll(ReplaceNameConstant.ALARM_NAME, alarmDescribe);
        }
        if (disCmdContent.contains(ReplaceNameConstant.ORDER_NUM)) {
            AlarmUtil alarmUtil = SpringContextUtil.getBean(AlarmUtil.class);
            //获取表号
            String serverNum = alarmUtil.getServerNumByTrainId(alarmInfo.getTrainId());
            disCmdContent = disCmdContent.replaceAll(ReplaceNameConstant.ORDER_NUM, serverNum + alarmInfo.getOrderNum());
        }
        // 道岔电子调度命令
        if (disCmdContent.contains("%s站")) {
            ConStationDto conStationDto =
                    conStationInfoService.getStationByConStationId(Integer.valueOf(alarmInfo.getAlarmConStation()));
            disCmdContent = disCmdContent.replaceAll("%s站", conStationDto.getStationName());
        }
        if (disCmdContent.contains("%s号")) {
            disCmdContent = disCmdContent.replaceAll("%s号", alarmInfo.getSwitchName() + "号");
        }
        //接触网失电电子调度命令
        if (disCmdContent.contains(ReplaceNameConstant.TRACTION_SECTION_NAME)) {
            disCmdContent = disCmdContent.replaceAll(ReplaceNameConstant.TRACTION_SECTION_NAME, alarmInfo.getAlarmSite());
        }

        disCmdSendUtils.sendDisCmd(alarmInfo, disCmdContent, station);
        log.info("发送刷新电子调度命");
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
        log.info("调度命令发送成功");
    }
}
