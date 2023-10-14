package com.tct.itd.adm.msgRouter.handle;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.msgRouter.service.SwitchFailureService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.adm.util.StrategyMatchUtil;
import com.tct.itd.basedata.dfsread.service.handle.ConStationInfoService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.dto.ConStationDto;
import com.tct.itd.common.dto.Info;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.enums.PlanRunGraphEnum;
import com.tct.itd.hub.service.PlanRunGraphService;
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

/**
 * @classname: SFMidSingleHandler
 * @description: 道岔故障-中间道岔故障-影响单行
 * @author: liyunlong
 * @date: 2021/12/29 9:52
 */
@Component
@Slf4j
public class SFMidSingleHandler implements AlarmInfoMessageHandler {

    @Resource
    private AppPushService appPushService;

    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;

    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private ConStationInfoService conStationInfoService;

    @Resource
    private PlanRunGraphService planRunGraphService;

    @Resource
    private SwitchFailureService switchFailureService;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        // 车站确认标识
        Integer ifConfirmed = alarmInfo.getIfConfirmed();
        if (NumConstant.ONE.equals(ifConfirmed)) {
            switchFailureService.sendAuxiliaryDecisionThree(alarmInfo.getTableInfoId());
            return;
        }
        log.info("道岔故障-中间道岔故障-影响单行,录入参数:{}", alarmInfo);
        // 设置推荐指令步骤
        alarmInfo.setExecuteStep(NumConstant.ONE);
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
        alarmInfo.setTableBoxId2(NumConstant.NEGATIVE_ONE);
        long detailId = aidDecisionExecService.doExecAidDecision(alarmInfo);
        // 获取第一次推荐指令执行单元信息
        List<AidDesSubStepEntity> aidDesSubStepList =
                aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), NumConstant.ONE, NumConstant.ZERO);
        // 获取推荐指令各执行单元的内容
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(aidDesSubStepList, alarmInfo);
        ConStationDto conStationDto = conStationInfoService.getStationByConStationId(Integer.parseInt(alarmInfo.getAlarmConStation()));
        stepList = StrategyMatchUtil.formatStepList(stepList, conStationDto);
        // 执行单元对象，返回前台用于执行时回传
        List<AidDesSubStepOutDto> aidDesSubStepDtoList = aidDesSubStepConvert.entitiesToDtoList(aidDesSubStepList);
        // 组装推荐指令内容，用于发给客户端
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        String alarmTypeDetail = admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail());
        admIdea.setAlarmTypeDetailStr(alarmTypeDetail);
        admIdea.setAidDesSubStepDtoList(aidDesSubStepDtoList);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        // 推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        // 获取车次追踪数据，将当前车次停车区段信息存入缓存，用于车次追踪检验位置是否发生变化
        // 告警信息存入子表
        admAlertInfoSubService.insert(alarmInfo);
        // 获取运行图,此时运行图是原图,用于道岔故障中断恢复时使用
        String planGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        BasicCommonCacheUtils.set(Cache.PLAN_GRAPH,planGraph);
        // 上报道岔故障后,缓存标识,防止重复上报
        BasicCommonCacheUtils.hPut(Cache.ALREADY_REPORT_SWITCH_FAILURE, alarmInfo.getSwitchNo(), "1");
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(1,2));
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.SWITCH_FAILURE_MIDDLE_SINGLE;
    }
}
