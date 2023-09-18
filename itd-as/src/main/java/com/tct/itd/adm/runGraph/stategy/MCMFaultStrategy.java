package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.common.dto.PrePlanRunGraph;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description 牵引故障-运行图预览处理类
 * @Author zhangyinglong
 * @Date 2021/6/2 14:27
 */
@Slf4j
@Service
public class MCMFaultStrategy extends AbstractPrePlanRunGraphStrategy implements IPrePlanRunGraphStrategy {

    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
       /* //默认立即掉线
        return AlarmStateEnum.NEXT_STATION_DROP_LINE.getCode();*/
        //1立即掉线;5:终点站掉线
        return alarmInfo.getAlarmState();
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        String endAlarmTime = DateUtil.getTimeStamp(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss.SSS");
        return endAlarmTime;

    }

    @Override
    public List<AlgStrategyResult> listAdjustStrategy(AlarmInfo alarmInfo) {
        log.info("联锁双机获取方案列表时，传入故障信息：{}", alarmInfo);
        Assert.notNull(alarmInfo, "客户端未上报故障");
        //通过infoId获取应急事件信息
        List<AlgStrategyResult> algStrategyResultList = null;
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss.SSS"));
        try {
            algStrategyResultList = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
        } catch (Exception e) {
            log.error("牵引故障取调整方案列表出错,异常信息:【{}】", e.getMessage());
            // 获取运行图预览方案失败,直接结束故障流程。
            ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
        }
        Assert.notEmpty(algStrategyResultList, "未获取到调整方案");
        return algStrategyResultList;
    }

    @Override
    public PrePlanRunGraph previewRunGraph(AlarmInfo alarmInfo) {
        int caseCode = alarmInfo.getCaseCode();
        //获取告警信息
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo, "当前故障生命周期已结束");
        alarmInfo.setCaseCode(caseCode);
        PrePlanRunGraph prePlanRunGraph;
        //通过infoId获取应急事件信息
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss.SSS"));
        prePlanRunGraph = aidDecisionExecService.getPlanRunGraph(alarmInfo);
        prePlanRunGraph.setCountDown(180);
        return prePlanRunGraph;
    }

    @Override
    public String strategy() {
        return AlarmTypeConstant.MORE_MCM_FAILURE;
    }
}
