package com.tct.itd.adm.msgRouter.executeHandle;


import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.executeHandle.failureRecovery.AllTrainDoorCannotOpenAdmFailureRecoveryHandler;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;


/**
 * @Description 全列车门无法关闭推荐指令
 * @Author yhf
 * @Date 2021/5/17 22:20
 **/

@Slf4j
@Component
public class AllDoorCannotOpenAdmHandler implements AuxiliaryDecisionHandler {

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;
    @Resource
    private AllTrainDoorCannotOpenAdmFailureRecoveryHandler allTrainDoorCannotOpenAdmFailureRecoveryHandler;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        int executeStep = auxiliaryDecision.getExecuteStep();
        log.info("执行1扣车推荐指令:{}",auxiliaryDecision);
        if(executeStep == 1){
            doOffHoldADM(auxiliaryDecision);
        } else if (executeStep == 2) {
            showGraphADM(auxiliaryDecision);
        } else if (executeStep == 3){
            doGraphADM(auxiliaryDecision);
        } else if (executeStep < 0) {
            allTrainDoorCannotOpenAdmFailureRecoveryHandler.handle(auxiliaryDecision);
        } else {
            log.error("推荐指令命令步骤有误,请查看数据是否正确--{}", JsonUtils.toJSONString(auxiliaryDecision));
            throw new RuntimeException("推荐指令命令步骤有误,请查看数据是否正确");
        }
    }

    /**
     * @Author yuelei
     * @Desc 执行第二次推荐指令
     * @Date 20:39 2022/6/15
     */
    private void showGraphADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        //获取告警信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        log.info("执行二次推荐指令,alarmInfo-" + alarmInfo);
        //更新方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(),alarmInfo.getTableBoxId2(),1);
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(alarmInfo.getTableInfoId(), AlarmInfoEnum.EXECUTE_END_1.getCode());
        //自动获取ATS自动清客状态消失后，触发第3次推荐指令。
        BasicCommonCacheUtils.set(Cache.CLEAR_PEOPLE_IS_FINISH, alarmInfo);
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(11);
            add(12);
            add(13);
            add(14);
        }});
    }
    /**
     * @Author yuelei
     * @Desc 执行第三次推荐指令
     * @Date 20:39 2022/6/15
     */
    private void doGraphADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        //获取告警信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        log.info("执行三次推荐指令,alarmInfo-" + alarmInfo);
        //更新方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(),alarmInfo.getTableBoxId2(),1);
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新子表状态为已执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmInfo.setEndLife(false);
        admAlertInfoSubService.updateById(alarmInfo);
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(17);
            add(18);
            add(19);
            add(20);
            add(21);
        }});
    }

    /**
     * @Author yuelei
     * @Desc  执行第一次推荐指令
     * @Date 17:54 2022/6/15
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
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(3);
            add(4);
            add(5);
            add(6);
        }});
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_OPEN;
    }

}
