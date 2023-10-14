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
import com.tct.itd.dto.AidDesSubStepOutDto;
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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @classname: ApdcncHandler
 * @description: 整侧站台门无法关闭(Apdcnc : All platform door cannot close)
 * @author: liyunlong
 * @date: 2021/10/20 14:11
 */
@Slf4j
@Service
public class ApdcncHandler implements AlarmInfoMessageHandler {

    /**
     * 数字常量 0
     */
    public static final int ZERO_NUM = 0;

    /**
     * 数字常量 1
     */
    public static final int ONE_NUM = 1;

    /**
     * 数字常量 -1L
     */
    public static final long NEGATIVE_ONE_NUM = -1L;

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
        log.info("站台门故障-整侧站台门无法关闭,故障信息:{}", alarmInfo);
        alarmInfo.setStopAreaNumber(stopRegionDataService.getStopAreaByPlatformId(Integer.parseInt(alarmInfo.getPlatformId())));
        // 设置推荐指令步骤
        alarmInfo.setExecuteStep(ONE_NUM);
        alarmInfo.setTableBoxId2(NEGATIVE_ONE_NUM);
        long detailId = aidDecisionExecService.doExecAidDecision(alarmInfo);
        // 获取第一次推荐指令执行单元信息
        List<AidDesSubStepEntity> aidDesSubStepList =
                aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), ONE_NUM, ZERO_NUM);
        //替换站名
        List<AidDesSubStepEntity> entities = platformDoorService.replaceEntities(aidDesSubStepList, alarmInfo);
        // 获取推荐指令各执行单元的内容
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        // 执行单元对象，返回前台用于执行时回传
        List<AidDesSubStepOutDto> aidDesSubStepDtoList = aidDesSubStepConvert.entitiesToDtoList(entities);
        // 组装推荐指令内容，用于发给客户端
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(AdmAlertDetailTypeService.getDescribeByCode(String.valueOf(admIdea.getAlarmTypeDetail())));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepDtoList);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        // 推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        //告警信息存入子表
        admAlertInfoSubService.insert(alarmInfo);
        //缓存站台门已经上报过站台门故障
        BasicCommonCacheUtils.hPut(Cache.ALREADY_REPORT_PLATFORM_DOOR, alarmInfo.getPlatformDoorId(), IidsConstPool.PLATFORM_DOOR_OPEN);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        //更新流程图步骤
        platformDoorUpdateFlowchartsService.pushFirstAux(alarmInfo);
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.ALL_PLATFORM_DOOR_CANNOT_CLOSE;
    }
}
