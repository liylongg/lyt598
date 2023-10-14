package com.tct.itd.adm.msgRouter.handle;

import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
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
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.dto.Info;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 站台门故障-列车进站过程中站台门打开
 * @Author yl
 * @Date 2021/9/1 10:20
 **/
@Slf4j
@Component
public class AllPlatformDoorOpenIntoStationHandler implements AlarmInfoMessageHandler {

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
    private AlarmUtil alarmUtil;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource
    private StopRegionDataService stopRegionDataService;

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        log.info("站台门故障-列车进站过程中站台门打开,产生故障:{}", alarmInfo);
        // 设置站台,停车区域编号信息
        Integer platformId = alarmUtil.getPlatformIdByTrainTraceInSection(alarmInfo.getTrainId());
        alarmInfo.setPlatformId(String.valueOf(platformId));
        Integer stopAreaId = stopRegionDataService.getStopAreaByPlatformId(platformId);
        alarmInfo.setStopAreaNumber(stopAreaId);
        // 设置推荐指令步骤
        alarmInfo.setExecuteStep(1);
        alarmInfo.setTableBoxId2(-1L);
        long detailId = aidDecisionExecService.doExecAidDecision(alarmInfo);
        //查询故障决策指令-列车进站过程中站台门打开
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), 1, 0);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        //设置推荐指令标题
        admIdea.setTitle(aidDesSubStepUtils.getTitle(alarmInfo, 1, 0));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        //插入推荐指令数据
        long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        //告警信息存入子表
        admAlertInfoSubService.insert(alarmInfo);
        //缓存站台门已经上报过站台门故障
        BasicCommonCacheUtils.hPut(Cache.ALREADY_REPORT_PLATFORM_DOOR_IN_OUT, alarmInfo.getPlatformDoorId(),1);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(1);
            add(2);
        }});
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_INTO_STATION;
    }
}
