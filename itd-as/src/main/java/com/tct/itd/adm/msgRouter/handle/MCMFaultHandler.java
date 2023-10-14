package com.tct.itd.adm.msgRouter.handle;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.*;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.*;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.basedata.dfsread.service.handle.*;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.Info;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.UidGeneratorUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @ClassName 牵引故障处理类
 * @Description TODO
 * @Author zhoukun
 * @Date 2022/07/20 11:30
 */
@Slf4j
@Component
public class MCMFaultHandler implements AlarmInfoMessageHandler {

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
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AdmAlertDetailService admAlertDetailService;
    @Resource
    private StopRegionDataService stopRegionDataService;
    @Resource
    private AdmAlertDetailBoxService admAlertDetailBoxService;
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
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(1));
        log.info("列车牵引故障:{}", alarmInfo);
        //stationId转platformId
        Integer platformId = 0;
        if (alarmInfo.getSectionFlag() != IidsConstPool.IN_SECTION) {
            if (Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
                platformId = Integer.parseInt(alarmInfo.getPlatformId());
            } else {
                platformId = alarmUtil.getPlatformIdByTrainTrace(alarmInfo.getTrainId());
            }
        }
        if (alarmInfo.getSectionFlag() == IidsConstPool.IN_SECTION) {
            if (Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
                platformId = Integer.parseInt(alarmInfo.getPlatformId());
            } else {
                platformId = alarmUtil.getPlatformIdByTrainTraceInSection(alarmInfo.getTrainId());
            }
        }
        if (platformId == 0) {
            throw new BizException("未从车次追踪里获取到站台!");
        }
        alarmInfo.setPlatformId(String.valueOf(platformId));
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(1);
        alarmInfo.setTableBoxId2(-1L);
        //算出初期故障决策指令-牵引故障产生
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(),
                IidsConstPool.EXECUTE_STEP_1, IidsConstPool.DISPOSE_STEP_TYPE_ZERO);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        long detailId = aidDecisionExecService.doExecAidDecision(alarmInfo);
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = new AdmIdea(alarmInfo.getTableInfoId(), alarmInfo.getTrainId(), alarmInfo.getOrderNum(), alarmInfo.getStartAlarmTime(), alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(), alarmInfo.getAlarmTypeDetail(), stepList, alarmInfo.getStationId(), alarmInfo.getAlarmState(), alarmInfo.getExecuteStep());
        //设置故障类型文本信息,前端显示
        admIdea.setAlarmTypeStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        //设置故障子类型文本信息,前端显示
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        //插入推荐指令数据
        long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        //存入告警信息本站停车区域id 供算法使用
        if (!Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
            alarmInfo.setStopAreaNumber(stopRegionDataService.getStopAreaByPlatformId(platformId));
        }

        //告警信息存入子表
        admAlertInfoSubService.insert(alarmInfo);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);

    }

    /**
     * 推送接触网失电第二次推荐指令
     */
    public void pushSecondAdm(Integer choice) {
        //获取存活的alarmInfo
        AlarmInfo alarmInfo = admAlertInfoSubService.getInfoInLife();
        if (alarmInfo == null) {
            throw new BizException("未获取到处于生命周期的alarmInfo");
        }
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        log.info("牵引故障, 推送第二次推荐指令, alarmInfo参数:{}", alarmInfo);
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_2);
        long detailId = UidGeneratorUtils.getUID();
        AdmAlertDetail alertDetail = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令",
                new Date(), "系统产生第二次推荐指令", "1", 1, System.currentTimeMillis());
        admAlertDetailService.insert(alertDetail);
        List<AidDesSubStepEntity> entities = new ArrayList<>();
        //折返轨或折返区间,重新从车次追踪获取车次号
        if (Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
            String trainId = alarmInfo.getTrainId();
            TiasTraceInfo traceInfo = trainTraceCache.getIfPresent(trainId);
            if (Objects.isNull(traceInfo)) {
                log.error("根据车组号【{}】获取车次追踪数据失败！", trainId);
                throw new BizException(CodeEnum.NO_GET_TRAIN_TRACE);
            }
            alarmInfo.setOrderNum(traceInfo.getOrderNumber());
        }

        if (1 == choice) {
            // 终点站掉线
            entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.EXECUTE_STEP_2, 1);
            if (!Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
                alarmInfo.setAlarmState(AlarmStateEnum.TERMINAL_POINT_DROP_LINE_HOLD.getCode());
            }
            alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(7));
        }
        if (2 == choice) {
            // 立刻清人掉线
            entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.EXECUTE_STEP_2, 3);
            if (!Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
                entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.EXECUTE_STEP_2, 2);
                alarmInfo.setAlarmState(AlarmStateEnum.STATION_DROP_LINE.getCode());
            }
            alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(8));

        }
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        long boxId = UidGeneratorUtils.getUID();
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推荐指令入库
        AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
        admAlertDetailBoxService.insert(alertDetailBox);
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmInfo.setTableBoxId2(boxId);
        //设置状态未执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());

        admAlertInfoSubService.updateById(alarmInfo);
        log.info("牵引故障,生成第二次推荐指令提示成功,alarmInfo{}", alarmInfo);

    }

    @Override
    public String channel() {
        return AlarmTypeConstant.MORE_MCM_FAILURE;
    }
}
