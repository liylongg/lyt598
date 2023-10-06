package com.tct.itd.adm.msgRouter.executeHandle;

import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.executeHandle.failureRecovery.TractionPowerRecoveryHandler;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.constant.StringConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description : 接触网失电执行推荐指令
 * @Author : zhangjiarui
 * @Date : Created in 2022/3/8
 */
@Slf4j
@Component
public class TractionPowerAdmHandler implements AuxiliaryDecisionHandler {
    private static final String ONE = "1";
    private static final String TWO = "2";
    private static final int ONE_NUM = 1;
    private static final String POP_STRING_1 = "是否确认为供电设备故障?";
    private static final String POP_STRING_2 = "无电区段试送电是否成功?";


    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AdmAlertInfoService admAlertInfoService;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;
    @Resource
    private TractionPowerRecoveryHandler recoveryHandler;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        int executeStep = auxiliaryDecision.getExecuteStep();
        if (executeStep==1){
            log.info("接触网失电：执行第一次推荐指令:{}", auxiliaryDecision);
            doAidDecisionOne(auxiliaryDecision);
        }else if(executeStep==2){
            log.info("接触网失电：执行第二次推荐指令:{}", auxiliaryDecision);
            doAidDecisionTwo(auxiliaryDecision);
        }else if(executeStep==3){
            log.info("接触网失电：执行第三次推荐指令:{}", auxiliaryDecision);
            doAidDecisionThree(auxiliaryDecision);
        }else if(executeStep==4){
            log.info("接触网失电：执行第四次推荐指令:{}", auxiliaryDecision);
            doAidDecisionFour(auxiliaryDecision);
        }else if(executeStep<0){
            log.info("接触网失电：执行故障恢复推荐指令:{}", auxiliaryDecision);
            recoveryHandler.handle(auxiliaryDecision);
        }
    }

    private void doAidDecisionOne(AuxiliaryDecision auxiliaryDecision) {
        //删除推送故障确认弹框标志（删除了之后才能继续推该弹框）
        BasicCommonCacheUtils.delKey(StringConstant.CONFIRM_FAULT_RECOVERY_POP);
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //点亮流程图：第一次推荐指令
        alarmFlowchartService.setExecuteFlag(alarmInfo,3);
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmFlowchartService.setExecuteFlag(alarmInfo,4);
        alarmFlowchartService.setExecuteFlag(alarmInfo,5);
        alarmFlowchartService.setExecuteFlag(alarmInfo,6);
        alarmFlowchartService.setExecuteFlag(alarmInfo,7);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), ONE_NUM);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        //查询99弹窗推荐指令内容第一条
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.EXECUTE_STEP_99, 0);
        String subStepContent = entities.get(0).getSubStepContent();
        // 执行完后立即推出弹窗确认是否供电设备故障
        appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.POWER_POP,
                        "0", subStepContent));
        //缓存是否有供电设备弹框标志
        BasicCommonCacheUtils.set(Cache.TRACTION_POWER_POP_FLAG, StringConstant.POWER_SUPPLY_EQUIPMENT_POP);
    }

    private void doAidDecisionTwo(AuxiliaryDecision auxiliaryDecision) {
        //删除推送故障确认弹框标志（删除了之后才能继续推该弹框）
        BasicCommonCacheUtils.delKey(StringConstant.CONFIRM_FAULT_RECOVERY_POP);
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        // 更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),ONE_NUM);
        // 执行推荐指令，调用对应的handler处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        // 更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案"
                , TWO, "运行图方案选择", ONE_NUM);

        //根据是否供电设备故障，点亮不同的流程图节点
        if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 11)){
            alarmFlowchartService.setExecuteFlag(alarmInfo, 13);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 14);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 15);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 16);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 17);
        }
        if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 12)){
            alarmFlowchartService.setExecuteFlag(alarmInfo, 25);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 26);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 27);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 28);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 29);
        }
    }

    private void doAidDecisionThree(AuxiliaryDecision auxiliaryDecision) {
        //删除推送故障确认弹框标志（删除了之后才能继续推该弹框）
        BasicCommonCacheUtils.delKey(StringConstant.CONFIRM_FAULT_RECOVERY_POP);
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        // 更新推荐指令方案状态为已执行, 此时TableBoxId2已在推送时更新为第三次的id
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),ONE_NUM);
        // 执行推荐指令，调用对应的handler处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        // 更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        //根据试送点是否成功，点亮不同的流程图节点
        if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 31)){
            alarmFlowchartService.setExecuteFlag(alarmInfo, 34);
        }
        if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 32)){
            alarmFlowchartService.setExecuteFlag(alarmInfo, 43);
        }
    }

    private void doAidDecisionFour(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        // 更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),ONE_NUM);
        // 执行推荐指令，调用对应的handler处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        // 更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案"
                , TWO, "运行图方案选择", ONE_NUM);
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.TRACTION_POWER;
    }
}