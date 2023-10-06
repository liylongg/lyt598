package com.tct.itd.adm.msgRouter.executeHandle;

import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.executeHandle.failureRecovery.InterLockingDoubleRecoveryHandler;
import com.tct.itd.adm.msgRouter.handle.TractionPowerHandler;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;

/**
 * @Description : 联锁双机执行推荐指令
 * @Author : zhoukun
 * @Date : Created in 2022/4/8
 */
@Slf4j
@Component
public class InterLockingDoubleAdmHandler implements AuxiliaryDecisionHandler {
    private static final String ONE = "1";
    private static final String TWO = "2";
    private static final int ONE_NUM = 1;

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AdmAlertInfoService admAlertInfoService;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private TractionPowerHandler tractionPowerHandler;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;
    @Resource
    private InterLockingDoubleRecoveryHandler recoveryHandler;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        int executeStep = auxiliaryDecision.getExecuteStep();
        if (executeStep==1){
            log.info("联锁双机失电：执行第一次推荐指令:{}", auxiliaryDecision);
            doAidDecisionOne(auxiliaryDecision);
        }
        else if (executeStep==2){
            log.info("联锁双机失电：执行第二次推荐指令:{}", auxiliaryDecision);
            doAidDecisionTwo(auxiliaryDecision);
        }
        else if(executeStep<0){
            log.info("联锁双机失电：执行故障恢复推荐指令:{}", auxiliaryDecision);
            recoveryHandler.handle(auxiliaryDecision);
        }

    }

    private void doAidDecisionOne(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        // 显示故障续报按钮
        admAlertInfoService.updateReportById(alarmInfo.getTableInfoId(), 1);
        //点亮流程图
        alarmFlowchartService.setExecuteFlag(alarmInfo,3);
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmFlowchartService.setExecuteFlag(alarmInfo,4);
        alarmFlowchartService.setExecuteFlag(alarmInfo,5);
        alarmFlowchartService.setExecuteFlag(alarmInfo,6);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), ONE_NUM);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        //刷新前台页面
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", "刷新联锁故障按钮"));
        alarmFlowchartService.setExecuteFlag(alarmInfo,7);
    }

    private void doAidDecisionTwo(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //点亮流程图：第二次推荐指令
        alarmFlowchartService.setExecuteFlag(alarmInfo,17);
        // 更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),ONE_NUM);
        // 执行推荐指令，调用对应的handler处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmFlowchartService.setExecuteFlag(alarmInfo,18);
        alarmFlowchartService.setExecuteFlag(alarmInfo,19);
        alarmFlowchartService.setExecuteFlag(alarmInfo,20);
        alarmFlowchartService.setExecuteFlag(alarmInfo,21);
        // 更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案"
                , TWO, "运行图方案选择", ONE_NUM);
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.INTERLOCKING_DOUBLE;
    }
}