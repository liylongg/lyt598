package com.tct.itd.adm.msgRouter.handle;

import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.msgRouter.service.AxleCounterService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AxleCounterResetUpdateFlowchartsService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.AxleStopAreaResult;
import com.tct.itd.common.dto.Info;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author kangyi
 * @description 非道岔区段计轴故障
 * @date 2022年 01月15日 14:03:24
 */
@Slf4j
@Component
public class AxleCounterAlarmNotSwitchResetHandler implements AlarmInfoMessageHandler {

    @Resource
    private AxleCounterService axleCounterService;
    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AxleCounterResetUpdateFlowchartsService axleCounterResetUpdateFlowchartsService;

    //故障恢复弹窗显示时长
    private static final int RECOVERY_WINDOW_DISPLAY_TIME = 180;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        //故障状态
        switch (alarmInfo.getAlarmState()) {
            //产生故障-推送第一次推荐指令
            case IidsConstPool.ALARM_STATE_OCCURRENCE:
                pushAuxiliaryDecisionFirst(alarmInfo);
                break;
            //故障恢复-推送故障恢复推荐指令
            case IidsConstPool.ALARM_STATE_FAILOVER:
                axleCounterRecovery(alarmInfo);
                break;
            default:
                log.error("ARB计轴故障故障状态不正确alarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
        }
    }

    private void pushAuxiliaryDecisionFirst(AlarmInfo alarmInfo) {
        log.info("接收到产生计轴故障，开始生成第一次推荐指令");
        String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
        //算法获取上下行和停车区域id
        AxleStopAreaResult areaResult = axleCounterService.getAlgorithmResult(alarmInfo);
        if (Objects.isNull(areaResult)) {
            throw new BizException("获取算法停车区域和上下行为空");
        }
        //不影响行车的计轴不上报推荐指令，中心前台推送报警弹窗
        if (IidsConstPool.AXLE_ALG_NO_HOLD_TYPE.equals(areaResult.getAxleFailureType())) {
            log.info("算法反馈计轴区段【{}】为不影响行车的计轴区段，不推送推荐指令", alarmInfo.getAxleCounterId());
            BasicCommonCacheUtils.hPut(Cache.ALREADY_REPORT_AXLE_COUNTER, String.valueOf(alarmInfo.getAxleCounterId()), alarmInfo.getAlarmTypeDetail());
            return;
        }
        //根据算法结果返回车站，集中站和上下行
        axleCounterService.setAlarmInfo(alarmInfo, areaResult);
        log.info("产生故障:{},alarmInfo:{}", descByCode, JsonUtils.toJSONString(alarmInfo));
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_1);
        alarmInfo.setTableBoxId2(IidsConstPool.INIT_AUXILIARY_DECISION_TABLE_ID);
        //存入日志信息
        long detailId = aidDecisionExecService.doExecAidDecision(alarmInfo);
        //获取推送到前端的推送对象
        AdmIdea admIdea = axleCounterService.getAdmIdea(alarmInfo, String.valueOf(alarmInfo.getAlarmTypeDetail()), IidsConstPool.DISPOSE_STEP_ONE, IidsConstPool.DISPOSE_STEP_TYPE_ZERO, descByCode);
        //设置标题
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        log.info("推送第一次推荐指令对象:{}", JsonUtils.toJSONString(admIdea));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        //插入推荐指令数据
        long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        //告警信息存入子表
        admAlertInfoSubService.insert(alarmInfo);
        //缓存计轴区段已经上报过计轴故障
        BasicCommonCacheUtils.hPut(Cache.ALREADY_REPORT_AXLE_COUNTER, String.valueOf(alarmInfo.getAxleCounterId()), alarmInfo.getAlarmTypeDetail());
        log.info("第一次推荐指令推送内容admInfo:{}", JsonUtils.toJSONString(admInfo));
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        //更新流程图步骤
        axleCounterResetUpdateFlowchartsService.pushFirstAux(alarmInfo);
    }

    private void axleCounterRecovery(AlarmInfo alarmInfo) {
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo, "该故障生命周期已结束");
        //设置故障结束时间
        alarmInfo.setEndAlarmTime(DateUtil.getDate("yyyy-MM-dd HH:mm:ss.SSS"));
        log.info("接收到计轴故障恢复，开始推送故障恢复推荐指令！alarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
        int executeStepType = IidsConstPool.DISPOSE_STEP_TYPE_ZERO;
        int executeStep = IidsConstPool.EXECUTE_STEP_0_2;
        //道岔区段计轴故障，故障恢复，缓行后故障恢复不一样
        if (BasicCommonCacheUtils.exist(Cache.AXLE_COUNTER_SLOWLY_GRAPH_SIGN)) {
            executeStepType = IidsConstPool.DISPOSE_STEP_TYPE_ONE;
            executeStep = IidsConstPool.EXECUTE_STEP_0_3;
        }
        //更新流程图
        axleCounterResetUpdateFlowchartsService.notSwitchPushRecovery(alarmInfo);
        if (BasicCommonCacheUtils.exist(Cache.AXLE_COUNTER_CONFIRM)) {
            //推送车站提示框，关闭划轴结果弹窗
            appPushService.sendMessage(MsgTypeEnum.STATION_RECOVER_ALERT_WID, new WebNoticeDto(CommandEnum.STATION_RECOVER_ALERT_WID.getMsgCode(), String.valueOf(alarmInfo.getStationId()), alarmInfo.getAxleCounterName() + "已恢复"));
        }
        String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
        alarmInfo.setExecuteStep(executeStep);
        //更新故障恢复步骤为已推送推荐指令
        alarmInfo.setFailureRecoveryStep(IidsConstPool.RECOVERY_PUSH_STEP1);
        admAlertInfoSubService.updateById(alarmInfo);
        //获取推送到前端的
        AdmIdea admIdea = axleCounterService.getAdmIdea(alarmInfo, AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH_RESET, executeStep, executeStepType, descByCode);
        //设置故障详情
        admIdea.setAlarmTypeDetailStr(descByCode);
        //设置标题
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推送弹窗显示时长
        admIdea.setShowSecond(RECOVERY_WINDOW_DISPLAY_TIME);
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        log.info("开始推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
        //推送推荐指令
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
    }


    @Override
    public String channel() {
        return AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH_RESET;
    }
}
