package com.tct.itd.adm.msgRouter.handle;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.basedata.dfsread.service.handle.StopRegionDataService;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.Info;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Description 列车空调故障
 * @Author YHF
 * @Date 2020/6/3 9:20
 **/
@Slf4j
@Component
public class AirConditionerFailureHandler implements AlarmInfoMessageHandler {

    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AdmCommonMethodService admCommonMethodService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AlarmUtil alarmUtil;
    @Resource
    private StopRegionDataService stopRegionDataService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    private static final String SEND_DIS_CMD_AIR_CONDITION = "sendDisCmdAirCondition";

    @Override
    public void handle(AlarmInfo alarmInfo) {
        //推送中心或车站请求确认或通知车站录入故障的消息
        if (0 == admCommonMethodService.pushConfirmToCenter(alarmInfo)) {
            return;
        }
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(1));
        log.info("列车空调故障:{}", alarmInfo);
        // 空调只有手动上报
        Integer platformId = 0;
        if (alarmInfo.getSectionFlag() != IidsConstPool.IN_SECTION) {
            if (Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
                platformId = Integer.parseInt(alarmInfo.getPlatformId());
            } else {
                platformId = alarmUtil.getPlatformIdByTrainTrace(alarmInfo.getTrainId());
                alarmInfo.setStopAreaNumber(stopRegionDataService.getStopAreaByPlatformId(platformId));
            }
        }
        if (alarmInfo.getSectionFlag() == IidsConstPool.IN_SECTION) {
            if (Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
                platformId = Integer.parseInt(alarmInfo.getPlatformId());
            } else {
                platformId = alarmUtil.getPlatformIdByTrainTraceInSection(alarmInfo.getTrainId());
                alarmInfo.setStopAreaNumber(stopRegionDataService.getStopAreaByPlatformId(platformId));
            }
        }
        if (platformId == 0) {
            throw new BizException("未从车次追踪里获取到站台!");
        }
        alarmInfo.setPlatformId(String.valueOf(platformId));

        //设置推荐指令步骤
        alarmInfo.setExecuteStep(1);
        alarmInfo.setTableBoxId2(-1L);
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(),
                IidsConstPool.EXECUTE_STEP_1, IidsConstPool.DISPOSE_STEP_TYPE_ZERO);
        if (Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
            entities = entities.stream().filter(e -> !Objects.equals(e.getBeanName(), SEND_DIS_CMD_AIR_CONDITION)).collect(Collectors.toList());
        }

        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        long detailId = aidDecisionExecService.doExecAidDecision(alarmInfo);
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = new AdmIdea(alarmInfo.getTableInfoId(), alarmInfo.getTrainId(), alarmInfo.getOrderNum(), alarmInfo.getStartAlarmTime(), alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(), alarmInfo.getAlarmTypeDetail(), stepList, alarmInfo.getStationId(), alarmInfo.getAlarmState(), alarmInfo.getExecuteStep());
        //设置故障类型文本信息
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescribeByCode(String.valueOf(admIdea.getAlarmType())));
        //设置故障子类型文本信息
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        //插入推荐指令数据
        long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        //告警信息存入子表
        admAlertInfoSubService.insert(alarmInfo);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("推送推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.AIR_CONDITIONING_FAILURE;
    }
}
