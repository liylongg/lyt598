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
 * @Description 列车空调无通风推荐指令
 * @Author zyl
 * @Date 2021/5/17 22:20
 **/

@Slf4j
@Component
public class AirConditioningNoVentilateAdmHandler implements AuxiliaryDecisionHandler {
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    public AirConditioningNoVentilateAdmHandler() {
    }

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        // (1)下一站需要清客,需要计算相应的时间
        // (2)获取算法需要参数(电子地图返回的数据)，发送http请求至算法服务，获取受影响车次、扣车站台、站台抬车时间信息，存入redis
        // (3)调整运行图
        log.info("列车空调无通风推荐指令:{}", auxiliaryDecision);
        long infoId = auxiliaryDecision.getTableInfoId();
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        log.info(AdmAlertDetailTypeService.getDescribeByCode(String.valueOf(alarmInfo.getAlarmTypeDetail())) + "调图步骤--列车执行辅助调图决策,alarmInfo-" + alarmInfo);

        //执行推荐指令，调用对应handle处理
        // 空调无通风需要清人(clearPeople没放数据库里可配置，在定时任务里，调完图之后清人)
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新运行图决策数据库状态
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 1);
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(2, 3, 4, 5, 11));
        log.info("存储至redis,准备调整运行图");
        //执行第一次推荐指令后，插入第四条告警信息 运行图预览信息
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(), "系统产生运行图方案", "2", "运行图方案选择");
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.AIR_CONDITIONING_VENTILATE_FAILURE;
    }
}
