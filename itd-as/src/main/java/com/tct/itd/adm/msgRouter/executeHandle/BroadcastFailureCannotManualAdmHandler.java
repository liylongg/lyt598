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
 * @Description 广播故障-不可人工推荐指令
 * @Author zyl
 * @Date 2021/5/17 22:20
 **/

@Slf4j
@Component
public class BroadcastFailureCannotManualAdmHandler implements AuxiliaryDecisionHandler {
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
        // (1)下一站需要清客,需要计算相应的时间
        // (2)调整运行图
        log.info("广播故障-不可人工推荐指令:{}", auxiliaryDecision);
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        log.info(AdmAlertDetailTypeService.getDescribeByCode(String.valueOf(alarmInfo.getAlarmTypeDetail())) + "调图步骤--列车执行辅助调图决策,alarmInfo-" + alarmInfo);
        //执行推荐指令，调用对应handle处理
        // 广播人工不可用需要清人(clearPeople没放数据库里可配置，在定时任务里，调完图之后清人)
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新运行图决策数据库状态
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 1);
        log.info("存储至redis,准备调整运行图");
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(2, 3, 4, 5, 6));
        //执行第一次推荐指令后，插入第四条告警信息 运行图预览信息
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(), "系统产生运行图方案", "2", "运行图方案选择");
        //更新子表状态为已执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
        admAlertInfoSubService.updateById(alarmInfo);
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.BROADCAST_FAILURE_CANNOT_MANUAL;
    }
}
