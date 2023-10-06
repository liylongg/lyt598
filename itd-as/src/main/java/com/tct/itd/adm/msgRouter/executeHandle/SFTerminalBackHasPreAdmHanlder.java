package com.tct.itd.adm.msgRouter.executeHandle;

import com.google.common.collect.Lists;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.executeHandle.failureRecovery.SwitchFaultAdmFailureRecoveryHandler;
import com.tct.itd.adm.msgRouter.executeHandle.failureRecovery.SwitchFaultAdmFailureRecoveryHandlerForSZ;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;

/**
 * @classname: SFTerminalBackHasPreAdmHanlder
 * @description: 终端站折返道岔故障-具备本站折返推荐指令handler
 * @author: liyunlong
 * @date: 2021/12/21 9:57
 */
@Slf4j
@Service
public class SFTerminalBackHasPreAdmHanlder implements AuxiliaryDecisionHandler {

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource
    private SwitchFaultAdmFailureRecoveryHandler switchFaultAdmFailureRecoveryHandler;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        int executeStep = auxiliaryDecision.getExecuteStep();
        switch (executeStep) {
            case 1:
                log.info("道岔故障-终端站后折返具备本站折返：执行第一次推荐指令:{}", auxiliaryDecision);
                doAidDecisionOne(auxiliaryDecision);
                break;
            case 2:
                log.info("道岔故障-终端站后折返具备本站折返：执行第二次推荐指令:{}", auxiliaryDecision);
                doAidDecisionTwo(auxiliaryDecision);
                break;
            case 3:
                log.info("道岔故障-终端站后折返具备本站折返：执行第三次推荐指令:{}", auxiliaryDecision);
                doAidDecisionThree(auxiliaryDecision);
                break;
            // 故障恢复
            case -1:
            case -2:
            case -3:
                switchFaultAdmFailureRecoveryHandler.handle(auxiliaryDecision);
                break;
            default:
                log.error("推荐指令命令步骤有误,请查看数据是否正确--{}", JsonUtils.toJSONString(auxiliaryDecision));
                throw new BizException("推荐指令命令步骤有误,请查看数据是否正确");
        }

    }

    @Override
    public String channel() {
        return AlarmTypeConstant.SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE;
    }

    /**
     * 执行第一次推荐指令
     * @author liyunlong
     * @date 2021/10/21 16:09
     * @param auxiliaryDecision 推荐指令相关信息
     */
    private void doAidDecisionOne(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        // 更新推荐指令方案状态已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), NumConstant.ONE);
        // 执行推荐指令，调用对应的handler处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        BasicCommonCacheUtils.set(Cache.SWITCH_FAILURE_EXECUTE_ONE_TIME, System.currentTimeMillis());
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(3,4,5,6,7));
    }

    /**
     * 执行第二次推荐指令
     * @author liyunlong
     * @date 2021/10/11 10:16
     * @param auxiliaryDecision 推荐指令相关信息
     */
    private void doAidDecisionTwo(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        // 更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),NumConstant.ONE);
        // 执行推荐指令，调用对应的handler处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        // 更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案"
                , "2", "运行图方案选择", 1);
        BasicCommonCacheUtils.hPut(Cache.SWITCH_TWENTY_PREVIEW_SIGN, String.valueOf(alarmInfo.getSwitchNo()),
                System.currentTimeMillis());
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(12, 13, 14, 15));
    }

    /**
     * 执行第三次推荐指令
     * @author liyunlong
     * @date 2021/12/21 17:39
     * @param auxiliaryDecision 推荐指令相关信息
     */
    private void doAidDecisionThree(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        // 更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(), NumConstant.ONE);
        // 执行推荐指令，调用对应的handler处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        // 更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案"
                , "2", "运行图方案选择", 1);
        // BasicCommonCacheUtils.delKey(Cache.SF_HAS_PRE_THREE_AID);
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(18, 19, 20, 21));
    }
}
