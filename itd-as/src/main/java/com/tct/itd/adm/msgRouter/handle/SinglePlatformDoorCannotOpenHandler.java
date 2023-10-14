package com.tct.itd.adm.msgRouter.handle;


import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.msgRouter.service.*;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.PlatformDoorUpdateFlowchartsService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.basedata.dfsread.service.handle.StopRegionDataService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.Info;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
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
 * @author kangyi
 * @description 单档站台门无法打开应急事件录入处理类
 * @date 2021/10/9
 **/
@Slf4j
@Component
public class SinglePlatformDoorCannotOpenHandler implements AlarmInfoMessageHandler {

    @Resource
    private AdmCommonMethodService admCommonMethodService;

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
    private StopRegionDataService stopRegionDataService;

    @Resource
    private PlatformDoorService platformDoorService;

    @Resource
    private PlatformDoorUpdateFlowchartsService platformDoorUpdateFlowchartsService;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        // 推送中心或车站请求确认或通知车站录入故障的消息
        if (0 == admCommonMethodService.pushConfirmToCenter(alarmInfo)) {
            return;
        }
        //故障类型
        String alarmType = AdmAlertDetailTypeService.getDescribeByCode(String.valueOf(alarmInfo.getAlarmTypeDetail()));
        log.info("产生站台门故障，故障类型:{}，故障内容:{}", alarmType, JsonUtils.toJSONString(alarmInfo));
        alarmInfo.setStopAreaNumber(stopRegionDataService.getStopAreaByPlatformId(Integer.parseInt(alarmInfo.getPlatformId())));
        // 设置推荐指令步骤
        alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_1);
        alarmInfo.setTableBoxId2(IidsConstPool.INIT_AUXILIARY_DECISION_TABLE_ID);
        alarmInfo.setFailureRecoveryStep(IidsConstPool.NO_RECOVERY_PUSH);
        //插入故障信息进数据库
        long detailId = aidDecisionExecService.doExecAidDecision(alarmInfo);
        //查询推荐指令配置的步骤  单档站台门无法打开
        List<AidDesSubStepEntity> stepEntities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.DISPOSE_STEP_ONE, IidsConstPool.DISPOSE_STEP_TYPE_ZERO);
        //替换站名
        List<AidDesSubStepEntity> entities = platformDoorService.replaceEntities(stepEntities, alarmInfo);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(alarmType);
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        //插入推荐指令数据
        long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        log.info("推送第一次推荐指令信息内容:{}", JsonUtils.toJSONString(admInfo));
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        //告警信息存入子表
        admAlertInfoSubService.insert(alarmInfo);
        //缓存站台门已经上报过站台门故障
        BasicCommonCacheUtils.hPut(Cache.ALREADY_REPORT_PLATFORM_DOOR, alarmInfo.getPlatformDoorId(), IidsConstPool.PLATFORM_DOOR_CLOSE);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        //更新流程图
        platformDoorUpdateFlowchartsService.pushFirstAux(alarmInfo);
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.SINGLE_PLATFORM_DOOR_CANNOT_OPEN;
    }
}
