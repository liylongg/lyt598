package com.tct.itd.adm.msgRouter.executeHandle;

import com.google.common.collect.Lists;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.runGraph.stategy.BroadcastFailureCanManualStrategy;
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
 * @Description 广播故障-可人工推荐指令
 * @Author zyl
 * @Date 2021/5/17 22:20
 **/

@Slf4j
@Component()
public class BroadcastFailureCanManualAdmHandler implements AuxiliaryDecisionHandler {
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private BroadcastFailureCanManualStrategy broadcastFailureCanManualStrategy;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        // (1)更新运行图决策数据库状态
        // (2)调整运行图
        log.info("广播故障-可人工推荐指令:{}", auxiliaryDecision);
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());

        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        log.info(AdmAlertDetailTypeService.getDescribeByCode(String.valueOf(alarmInfo.getAlarmTypeDetail())) + "调图步骤--列车执行辅助调图决策,alarmInfo-" + alarmInfo);
        //更新运行图决策数据库状态
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 1);
        //故障结束时间为当前时间
        alarmInfo.setEndAlarmTime(broadcastFailureCanManualStrategy.getEndAlarmTime(alarmInfo));
        //故障运行至终点站掉线,和算法约定
        alarmInfo.setAlarmState(broadcastFailureCanManualStrategy.getAlarmState(alarmInfo));
        //更新运行图
        log.info("准备调整运行图");
        //执行推荐指令，调用对应handle处理
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(2, 3, 4, 5, 6));
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //故障场景分步骤，更为清晰，后续便于拓展
        //执行第一次推荐指令后，插入第四条告警信息 运行图预览信息
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(), "系统产生运行图方案", "2", "运行图方案选择");
        //更新子表状态为已执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
        admAlertInfoSubService.updateById(alarmInfo);

    }

    @Override
    public String channel() {
        return AlarmTypeConstant.BROADCAST_FAILURE_CAN_MANUAL;
    }
}
