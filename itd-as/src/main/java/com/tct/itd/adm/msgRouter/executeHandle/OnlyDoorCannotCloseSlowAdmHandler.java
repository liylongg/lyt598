package com.tct.itd.adm.msgRouter.executeHandle;


import com.google.common.collect.Lists;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.executeHandle.failureRecovery.OnlyDoorCannotCloseSlowRecoveryHandler;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;


/**
 * @Description 单车门无法关闭-缓行 推荐指令
 * @Author yl
 * @Date 2021/5/17 22:20
 **/

@Slf4j
@Component
public class OnlyDoorCannotCloseSlowAdmHandler implements AuxiliaryDecisionHandler {

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
    private AlarmFlowchartService alarmFlowchartService;
    @Resource
    private OnlyDoorCannotCloseSlowRecoveryHandler onlyDoorCannotCloseSlowRecoveryHandler;
    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        int executeStep = auxiliaryDecision.getExecuteStep();
        log.info("执行扣车推荐指令:{}",auxiliaryDecision);
        if(executeStep == 1){
            doOffHoldADM(auxiliaryDecision);
            alarmFlowchartService.setExecuteFlags(auxiliaryDecision.getTableInfoId(), Lists.newArrayList(3,4,5,6));
        } else if (executeStep == 2) {
            doGraphADM(auxiliaryDecision);
            alarmFlowchartService.setExecuteFlags(auxiliaryDecision.getTableInfoId(), Lists.newArrayList(23,24));
        } else if (executeStep == 3) {
            doThreeADM(auxiliaryDecision);
            alarmFlowchartService.setExecuteFlags(auxiliaryDecision.getTableInfoId(), Lists.newArrayList(11,12,13,14));
        } else if (executeStep == 4) {
            doFourADM(auxiliaryDecision);
            alarmFlowchartService.setExecuteFlags(auxiliaryDecision.getTableInfoId(), Lists.newArrayList(17,18,19,20,21));
        } else if (executeStep < 0) {
            onlyDoorCannotCloseSlowRecoveryHandler.handle(auxiliaryDecision);
        }else {
            log.error("推荐指令命令步骤有误,请查看数据是否正确--{}", JsonUtils.toJSONString(auxiliaryDecision));
            throw new RuntimeException("推荐指令命令步骤有误,请查看数据是否正确");
        }
    }

    /***
     * @Author yuelei
     * @Desc 执行第二次推荐指令
     * @Date 11:50 2022/6/20
     */
    private void doFourADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),1);
        //更新子表状态为已执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
        //流程已结束
        alarmInfo.setEndLife(false);
        admAlertInfoSubService.updateById(alarmInfo);
    }

    /**
     * @Author yuelei
     * @Desc 执行第三次推荐指令
     * @Date 17:57 2022/6/18
     */
    private void doThreeADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(),"系统产生运行图方案","2","运行图方案选择");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        log.info("执行第二次推荐指令推送调图方案");
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),1);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        //自动获取ATS自动清客状态消失后，触发第3次推荐指令。
        BasicCommonCacheUtils.set(Cache.CLEAR_PEOPLE_IS_FINISH, alarmInfo);
    }

    /**
     * @author yuelei
     * @desc 执行第二次推荐指令
     * @date 14:17 2022/6/18
     */
    private void doGraphADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        //获取告警信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        log.info("执行二次推荐指令,alarmInfo-" + alarmInfo);
        //更新方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(),alarmInfo.getTableBoxId2(),1);
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(),"系统产生运行图方案","2","运行图方案选择");
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(alarmInfo.getTableInfoId(), AlarmInfoEnum.EXECUTE_END_1.getCode());
    }

    /**
     * @Author yuelei
     * @Desc  执行第一次推荐指令
     * @Date 14:16 2022/6/18
     */
    private void doOffHoldADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(),alarmInfo.getTableBoxId(),1);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        //存入自动检测故障是否恢复缓存
        BasicCommonCacheUtils.set(Cache.TRAIN_DOOR_AUTO_CHECK_RECOVERY, alarmInfo.getTrainId());
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE_SLOW_DOWN;
    }
}
