package com.tct.itd.adm.msgRouter.handle;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.iconstant.DesSubStepBeanConstant;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AdmStationService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.basedata.dfsread.service.handle.StopRegionDataService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.Info;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @Description 列车空调故障
 * @Author YHF
 * @Date 2020/6/3 9:20
 **/
@Slf4j
@Component
public class AirConditioningNoVentilateHandler implements AlarmInfoMessageHandler {

    @Resource
    private AlarmUtil alarmUtil;

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
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private StopRegionDataService stopRegionDataService;

    private static final String THIS_STATION = "本站";
    private static final String NEXT_STATION = "下一站";

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AdmStationService admStationService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        //推送中心或车站请求确认或通知车站录入故障的消息
        if (0 == admCommonMethodService.pushConfirmToCenter(alarmInfo)) {
            return;
        }
        log.info("列车空调无通风" + alarmInfo);
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(1));
        // 空调故障-通风机故障只有自动上报
        if (alarmInfo.getAutoReport().equals(Boolean.TRUE)) {
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
            //区间上报,下一站掉线
            alarmInfo.setPlatformId(String.valueOf(platformId));
        }
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(1);
        alarmInfo.setTableBoxId2(-1L);

        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.EXECUTE_STEP_1, 0);
        if (Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
            entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.EXECUTE_STEP_1, 1);
        }
        //修改推荐指令替换下一站
        for (AidDesSubStepEntity entity : entities) {
            if (DesSubStepBeanConstant.SEND_DIS_CMD_CLEAR_PEOPLE.equals(entity.getBeanName())) {
                //下一站名称
                Integer stationId = alarmUtil.getStationIdByTrainTraceInSection(alarmInfo.getTrainId());
                String stationName = admStationService.selectByStationId(stationId).getStationName();
                //上下行
                String upDown = alarmInfo.getUpDown() == IidsConstPool.TRAIN_UP ? "上行" : "下行";
                entity.setSubStepContent(String.format(entity.getSubStepContent(), alarmInfo.getOrderNum(), stationName, upDown));
                break;
            }
        }
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        //无通风时区间与站台推荐指令掉线车站语句区别
        if (alarmInfo.getSectionFlag() == 0) {
            log.info("空调无通风站台故障，添加本站扣车推荐指令！");
            DisposeDto dto = stepList.get(0);
            dto.setStep(String.format(stepList.get(0).getStep(), THIS_STATION));
            stepList.set(0, dto);
        } else if (alarmInfo.getSectionFlag() == 1) {
            log.info("空调无通风区间故障，添加站扣车推荐指令！");
            DisposeDto dto = stepList.get(0);
            dto.setStep(String.format(stepList.get(0).getStep(), NEXT_STATION));
            stepList.set(0, dto);
        }
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
        if (!"-1".equals(alarmInfo.getTrainId())) {
            //存入已经上报的缓存
            TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent(alarmInfo.getTrainId());
            if (!Objects.isNull(tiasTraceInfo)) {
                BasicCommonCacheUtils.hPut(Cache.ALREADY_REPORT_AIR_CONDITION, tiasTraceInfo.getTrainId(), 1);
            }
        }
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("推送推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
    }


    @Override
    public String channel() {
        return AlarmTypeConstant.AIR_CONDITIONING_VENTILATE_FAILURE;
    }
}
