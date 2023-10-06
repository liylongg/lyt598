package com.tct.itd.adm.msgRouter.executeHandle;

import com.google.common.collect.Lists;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.executeHandle.failureRecovery.SwitchFaultAdmFailureRecoveryHandler;
import com.tct.itd.adm.msgRouter.executeHandle.failureRecovery.SwitchFaultAdmFailureRecoveryHandlerForSZ;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.service.AdmAlertInfoService;
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
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;

/**
 * @classname: MiddleFailureSingleAdmHandler
 * @description: 线路中间站道岔故障(非小交路折返站 - 影响单行)
 * @author: liyunlong
 * @date: 2023/3/1 19:56
 */
@Slf4j
@Component
public class MiddleFailureSingleAdmHandler implements AuxiliaryDecisionHandler {

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource
    private AdmAlertInfoService admAlertInfoService;

    @Resource
    private SwitchFaultAdmFailureRecoveryHandler switchFaultAdmFailureRecoveryHandler;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        int executeStep = auxiliaryDecision.getExecuteStep();
        switch (executeStep) {
            case 1:
                log.info("道岔故障-线路中间站道岔故障(非小交路折返站-影响单行)：执行第一次推荐指令:{}", auxiliaryDecision);
                doAidDecisionOne(auxiliaryDecision);
                break;
            case 2:
                log.info("道岔故障-线路中间站道岔故障(非小交路折返站-影响单行)：执行第二次推荐指令:{}", auxiliaryDecision);
                doAidDecisionTwo(auxiliaryDecision);
                break;
            case 3:
                log.info("道岔故障-线路中间站道岔故障(非小交路折返站-影响单行)：执行第三次推荐指令:{}", auxiliaryDecision);
                doAidDecisionThree(auxiliaryDecision);
                break;
            case 4:
                log.info("道岔故障-线路中间站道岔故障(非小交路折返站-影响单行):{}", auxiliaryDecision);
                doAidDecisionFour(auxiliaryDecision);
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
        return AlarmTypeConstant.MIDDLE_FAILURE_SINGLE;
    }

    /**
     * 执行第一次推荐指令
     * @author liyunlong
     * @date 2021/12/23 14:41
     * @param auxiliaryDecision 推荐指令信息
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
        String switchNo = alarmInfo.getSwitchNo();
        // String[] switchNoArr = switchNo.split(NumStrConstant.SEPARATOR);
        BasicCommonCacheUtils.set(Cache.SWITCH_OPERATE_RECOVERY_FLAG, switchNo);
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(3, 4, 5, 6, 7));
    }

    /**
     * 执行第二次推荐指令
     * @author liyunlong
     * @date 2021/12/23 15:26
     * @param auxiliaryDecision 推荐指令信息
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
        BasicCommonCacheUtils.hPut(Cache.SWITCH_TEN_PREVIEW_SIGN, String.valueOf(alarmInfo.getSwitchNo()),
                System.currentTimeMillis());
        // String noticeMsg = "道岔已摇至行车方向,具备列车通过条件";
        // alarmInfo.setSwitchName(noticeMsg);
        // AdmNoticeDto admNoticeDto = new AdmNoticeDto(alarmInfo,
        //         admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
        // appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(MsgPushEnum.ADM_INSERT_ALARM_CONFIRM_MSG.getCode(),
        //         NumStrConstant.ZERO, admNoticeDto));
        // 具备通车条件按钮可用
        admAlertInfoService.updateReadyById(infoId, NumConstant.ONE);
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(),Lists.newArrayList(12,13,14,15));
    }

    /**
     * 执行第三次推荐指令
     * @author liyunlong
     * @date 2021/12/21 17:39
     * @param auxiliaryDecision 推荐指令相关信息
     */
    private void doAidDecisionThree(AuxiliaryDecision auxiliaryDecision) {
        // 删除运行图预览及时间标识
        BasicCommonCacheUtils.delKey(Cache.SWITCH_TEN_PREVIEW_SIGN);
        BasicCommonCacheUtils.delKey(Cache.SWITCH_FAILURE_TEN_TIME);
        BasicCommonCacheUtils.delKey(Cache.SWITCH_TWENTY_PREVIEW_SIGN);
        BasicCommonCacheUtils.delKey(Cache.SWITCH_FAILURE_TWENTY_TIME);
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
        BasicCommonCacheUtils.hPut(Cache.SWITCH_TWENTY_PREVIEW_SIGN,alarmInfo.getSwitchNo(),System.currentTimeMillis());
        // 故障恢复按钮可用
        admAlertInfoService.updateAllowFailoverById(auxiliaryDecision.getTableInfoId(), NumConstant.ONE);
        // 禁用具备通车条件按钮
        admAlertInfoService.updateReadyById(infoId, NumConstant.TWO);
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(18, 19, 20, 21));
    }

    /**
     * 执行第四次推荐指令
     * @author liyunlong
     * @date 2021/12/23 20:56
     * @param auxiliaryDecision 推荐指令相关信息
     */
    private void doAidDecisionFour(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        // 更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),NumConstant.ONE);
        // 执行推荐指令，调用对应的handler处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
    }
}
