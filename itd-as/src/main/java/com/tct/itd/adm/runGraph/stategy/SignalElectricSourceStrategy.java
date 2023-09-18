package com.tct.itd.adm.runGraph.stategy;


import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.common.dto.PrePlanRunGraph;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description : 信号电源策略
 * @Author : zhoukun
 * @Date : Created in 2022/4/19
 */
@Slf4j
@Service
public class SignalElectricSourceStrategy extends AbstractPrePlanRunGraphStrategy implements IPrePlanRunGraphStrategy {
    @Resource
    
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AdmAlertInfoService admAlertInfoService;

    @Resource
    private AdmCommonMethodService admCommonMethodService;

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        //调60分钟图
        int tractionPowerTime = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.SIGNAL_ELECTRIC_PREVIEW_TIME));
        // 发生重复故障,故障结束时间以当前时间向后推15分钟
        return DateUtil.getTimeStamp(System.currentTimeMillis() + tractionPowerTime, "yyyy-MM-dd HH:mm:ss.SSS");
    }

    @Override
    public List<AlgStrategyResult> listAdjustStrategy(AlarmInfo alarmInfo) {
        log.info("信号电源获取方案列表时，传入故障信息：{}", alarmInfo);
        Assert.notNull(alarmInfo,"客户端未上报故障");
        //记录推图时间
        BasicCommonCacheUtils.hPut(Cache.SINGAL_ELECTRIC_PREVIEW_TIME_RECORD,alarmInfo.getAlarmConStation(),System.currentTimeMillis());
        List<AlgStrategyResult> algStrategyResultList = null;
        //通过infoId获取应急事件信息
        AlarmInfo alarmInfo1 = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());

        try {
            algStrategyResultList = aidDecisionExecService.adjustRunGraphLockAlg(alarmInfo);
        } catch (Exception e) {
            log.error("信号电源取调整方案列表出错,异常信息:【{}】", e.getMessage());
            // 获取运行图预览方案失败,直接结束故障流程。
            // 禁用故障续报按钮
            admAlertInfoService.updateReportById(alarmInfo.getTableInfoId(), 2);
            // 置灰运行图预览
            admCommonMethodService.disablePreviewButton(alarmInfo);
            ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
        }
        Assert.notEmpty(algStrategyResultList,"未获取到调整方案");
        return algStrategyResultList;
    }

    @Override
    public PrePlanRunGraph previewRunGraph(AlarmInfo alarmInfo) {
        int caseCode = alarmInfo.getCaseCode();
        //获取告警信息
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo, "当前故障生命周期已结束");
        alarmInfo.setCaseCode(caseCode);
        alarmInfo.setAlarmState(getAlarmState(alarmInfo));
        PrePlanRunGraph prePlanRunGraph;
        //通过infoId获取应急事件信息
        AlarmInfo alarmInfo1 = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());

//        //alarmInfo判断是第一次执行后，就调adjustRunGraphAlg，
//        if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_1){
//            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp());
//            alarmInfo.setAlarmState(2);
//            prePlanRunGraph = aidDecisionExecService.getPlanRunGraph(alarmInfo);
//        }else {
        prePlanRunGraph = aidDecisionExecService.getLockPlanRunGraph(alarmInfo);
//        }
        prePlanRunGraph.setCountDown(180);
        return prePlanRunGraph;
    }

    @Override
    public String strategy() {
        return AlarmTypeConstant.SIGNAL_ELECTRIC;
    }
}