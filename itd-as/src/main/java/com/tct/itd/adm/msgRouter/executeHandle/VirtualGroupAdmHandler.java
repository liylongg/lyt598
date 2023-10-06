package com.tct.itd.adm.msgRouter.executeHandle;

import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;

import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.executeHandle.failureRecovery.VirtualGroupRecoveryHandler;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;

import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description: 执行虚拟编组推荐指令
 * @Author: wangsijin
 * @Date: 2023/2/9 009 11:19
 */
@Slf4j
@Component
public class VirtualGroupAdmHandler implements AuxiliaryDecisionHandler {

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;

    @Resource
    private AppPushService appPushService;

    @Resource
    private VirtualGroupRecoveryHandler virtualGroupRecoveryHandler;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        log.info("收到执行推荐指令的参数auxiliaryDecision:{}", JsonUtils.toJSONString(auxiliaryDecision));
        if (auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_1) {
            log.info("执行第一次推荐指令");
            executeFirstStep(auxiliaryDecision);
        } else if (auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_2) {
            log.info("执行第二次推荐指令");
            executeSecondStep(auxiliaryDecision);
        } else if (auxiliaryDecision.getExecuteStep() < 0) {
            log.info("执行故障恢复推荐指令");
            virtualGroupRecoveryHandler.handle(auxiliaryDecision);
        }else {
            log.error("执行虚拟编组推荐指令的执行步骤有误，请进行核实:{}", auxiliaryDecision);
        }
    }

    //执行第一次推荐指令
    public void executeFirstStep(AuxiliaryDecision auxiliaryDecision){
        if (auxiliaryDecision.getTableInfoId() == -1L){
            log.error("执行虚拟编组第二次推荐指令获得的auxiliaryDecision的tableInfoId错误，参数为{}", auxiliaryDecision);
            return;
        }
        //根据tableInfoId查询对应的应急事件
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        if (alarmInfo.getTableBoxId() == -1L){
            log.error("执行虚拟编组第一次推荐指令获得的alarmInfo的tableBoxId错误，参数为{}", auxiliaryDecision);
            return;
        }
        alarmFlowchartService.setExecuteFlag(alarmInfo, 6);
        log.info("执行第一次推荐指令, alarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
        //执行推荐指令内容
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), NumConstant.ONE);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(alarmInfo.getTableInfoId(), AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmFlowchartService.setExecuteFlag(alarmInfo, 7);
        List<AidDesSubStepEntity> aidDesSubStep = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.EXECUTE_STEP_99, 0);
        //获取弹框推送的文字内容
        String subStepContent = aidDesSubStep.get(0).getSubStepContent();
        //推送确认弹窗
        appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.POWER_POP, "0", subStepContent));
        alarmFlowchartService.setExecuteFlag(alarmInfo, 8);
    }

    //执行第二次推荐指令
    public void executeSecondStep(AuxiliaryDecision auxiliaryDecision){
        long infoId = auxiliaryDecision.getTableInfoId();
        if (infoId == -1L){
            log.error("执行虚拟编组第二次推荐指令获得的auxiliaryDecision的tableInfoId错误，参数为{}", auxiliaryDecision);
            return;
        }
        //根据tableInfoId查询对应的应急事件
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        if (alarmInfo.getTableBoxId() == -1L){
            log.error("执行虚拟编组第一次推荐指令获得的alarmInfo的tableBoxId错误，参数为{}", auxiliaryDecision);
            return;
        }
        alarmFlowchartService.setExecuteFlag(alarmInfo, 16);
        // 执行推荐指令，调用对应的handler处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        // 更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(),NumConstant.ONE);
        // 更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案"
                , "2", "运行图方案选择", NumConstant.ONE);
        alarmFlowchartService.setExecuteFlag(alarmInfo, 17);

    }

    @Override
    public String channel() {
        return AlarmTypeConstant.VIRTUAL_GROUP;
    }
}
