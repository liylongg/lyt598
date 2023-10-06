package com.tct.itd.adm.msgRouter.executeHandle;


import com.google.common.collect.Lists;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;


/**
 * @Description 列车空调故障推荐指令
 * @Author zyl
 * @Date 2021/5/17 22:20
 **/

@Slf4j
@Component
public class AirConditioningFailureAdmHandler implements AuxiliaryDecisionHandler {
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        long infoId = auxiliaryDecision.getTableInfoId();
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        log.info("获取故障信息：{}", alarmInfo);
        //更新运行图决策数据库状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 1);
        //执行推荐指令，调用对应handle处理
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(2, 3, 4, 5));
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //执行第一次推荐指令后，插入第四条告警信息 运行图预览信息
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(), "系统产生运行图方案", "2", "运行图方案选择");
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.AIR_CONDITIONING_FAILURE;
    }
}
