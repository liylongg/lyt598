package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.common.dto.PrePlanRunGraph;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @Description 计轴故障-非道岔区段计轴故障
 * @Author kangyi
 * @Date 2022/1/27 14:27
 */
@Slf4j
@Service
public class AxleCounterAlarmNotSwitchStrategy extends AbstractPrePlanRunGraphStrategy implements IPrePlanRunGraphStrategy {

    /**
     * 计轴扣车3min
     */
    private static final long HOLD_TRAIN_END_TIME = 3 * 60 * 1000;

    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        return 1;
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        Date endAlarmTime = new Date();
        endAlarmTime.setTime(endAlarmTime.getTime() + HOLD_TRAIN_END_TIME);
        return DateUtil.getStringToDate(endAlarmTime, "yyyy-MM-dd HH:mm:ss.SSS");
    }

    @Override
    public String strategy() {
        return AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH;

    }


    @Override
    public List<AlgStrategyResult> listAdjustStrategy(AlarmInfo alarmInfo) {
        log.info("获取方案列表时，传入故障信息：{}", JsonUtils.toJSONString(alarmInfo));
        Assert.notNull(alarmInfo, "当前不存在处于生命周期的故障");
        //设置算法参数
        alarmInfo.setAlarmState(getAlarmState(alarmInfo));
        List<AlgStrategyResult> algStrategyResultList = null;
        try {
            algStrategyResultList = aidDecisionExecService.adjustRunGraphAxleCounterAlg(alarmInfo);
            //插入运行图预览信息
            alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                    alarmInfo.getTableInfoId(), "系统产生运行图方案", "2", "运行图方案选择");
        } catch (Exception e) {
            log.error("计轴故障取调整方案列表出错,异常信息:", e);
            // 获取运行图预览方案失败,直接结束故障流程。
            ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
        }
        Assert.notEmpty(algStrategyResultList, "未获取到调整方案");
        return algStrategyResultList;
    }


    @Override
    public PrePlanRunGraph previewRunGraph(AlarmInfo alarmInfo) {
        //获取告警信息
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo, "当前故障生命周期已结束");
        alarmInfo.setCaseCode(alarmInfo.getCaseCode());
        alarmInfo.setAlarmState(getAlarmState(alarmInfo));
        //设置算法参数
        alarmInfo.setEndAlarmTime(getEndAlarmTime(alarmInfo));
        PrePlanRunGraph prePlanRunGraph = aidDecisionExecService.getAxleCounterPlanRunGraph(alarmInfo);
        prePlanRunGraph.setCountDown(180);
        return prePlanRunGraph;
    }

}

