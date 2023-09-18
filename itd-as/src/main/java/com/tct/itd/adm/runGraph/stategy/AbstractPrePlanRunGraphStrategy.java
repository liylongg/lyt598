package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.AdjustStatisticalData;
import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.common.dto.PrePlanRunGraph;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.enums.PlanRunGraphEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.hub.service.PlanRunGraphService;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Description 运行图预览模板类
 * @Author zhangyinglong
 * @Date 2021/6/2 14:27
 */
@Slf4j
@Service
public abstract class AbstractPrePlanRunGraphStrategy implements IPrePlanRunGraphStrategy {

    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private PlanRunGraphService planRunGraphService;

    @Resource
    private AdmAlertInfoService admAlertInfoService;

    @Resource
    private AppPushService appPushService;

    private static final int PRE_TIME = 270000;

    /**
     * 继承
     * @param alarmInfo 故障信息
     * @return 调用算法执行步骤
     */
    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        return 0;
    }

    @Override
    public PrePlanRunGraph previewRunGraph(AlarmInfo alarmInfo) {
        int caseCode = alarmInfo.getCaseCode();
        int executeStep1 = alarmInfo.getExecuteStep();
        //获取告警信息
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo, "当前故障生命周期已结束");
        alarmInfo.setCaseCode(caseCode);
        alarmInfo.setAlarmState(getAlarmState(alarmInfo));
        //前端执行，推送执行步骤为晚点恢复，则重新赋值
        if (executeStep1 == IidsConstPool.EXECUTE_STEP_0_2) {
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_2);
            alarmInfo.setAlarmState(2);
        }
        //获取运行图并返回，用于预览运行图
        alarmInfo.setEndAlarmTime(this.getEndTime(alarmInfo));
        return aidDecisionExecService.getPlanRunGraph(alarmInfo);
    }


    @Override
    public List<AlgStrategyResult> listAdjustStrategy(AlarmInfo alarmInfo) {
        log.info("获取方案列表时，传入故障信息：{}", JsonUtils.toJSONString(alarmInfo));
        Assert.notNull(alarmInfo, "客户端未上报故障");
        //获取运行图策略，用于预览运行图
        alarmInfo.setEndAlarmTime(getEndTime(alarmInfo));
        if(IidsConstPool.EXECUTE_STEP_0_2 == alarmInfo.getExecuteStep()){
            alarmInfo.setAlarmState(2);
        }else{
            alarmInfo.setAlarmState(getAlarmState(alarmInfo));
        }
        List<AlgStrategyResult> algStrategyResultList;
        try {
            algStrategyResultList = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
        } catch (Exception e) {
            log.error("获取运行图预览方案列表失败,失败信息:【{}】", e.getMessage());
            ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        Assert.notEmpty(algStrategyResultList, "未获取到调整方案");
        return algStrategyResultList;
    }

    @Override
    public List<AlgStrategyResult> mockListAdjustStrategy() {
        log.info("模拟获取方案列表时");
        return new ArrayList<AlgStrategyResult>() {{
            AlgStrategyResult algStrategyResult = new AlgStrategyResult();
            algStrategyResult.setCaseCode(1);
            AdjustStatisticalData adjustStatisticalData = new AdjustStatisticalData();
            adjustStatisticalData.setNumber(0);
            add(algStrategyResult);
        }};
    }

    @Override
    public PrePlanRunGraph mockPreviewRunGraph() {
        PrePlanRunGraph prePlanRunGraph = new PrePlanRunGraph();
        //获取数据库原运行图
        String planRunGraphXml = (String)planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.XML);
        prePlanRunGraph.setCaseCode(1);
        prePlanRunGraph.setCountDown(180L);
        prePlanRunGraph.setGraphDataXml(planRunGraphXml);
        prePlanRunGraph.setBeforeGraphDataXml(planRunGraphXml);
        return prePlanRunGraph;
    }

    /**
     * @param alarmInfo
     * @return java.lang.String
     * @Description 获取运行图预览结束时间
     * @Author yuelei
     * @Date 2022/1/4 15:39
     */
    public String getEndTime(AlarmInfo alarmInfo) {
        //故障恢复晚点,返回当前时间为故障结束时间
        if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_2) {
            return DateUtil.getTimeStamp();
        }
        //牵引故障
        else if (alarmInfo.getAlarmType() == Integer.parseInt(AlarmTypeConstant.MCM_FAILURE) && alarmInfo.getSectionFlag() == 1) {
            //取出开始告警时间
            Date startAlarmTime = DateUtil.getStringToDate(alarmInfo.getStartAlarmTime(), "yyyy-MM-dd HH:mm:ss.SSS");
            String mcmEndAlarmTime = DateUtil.getTimeStamp(startAlarmTime.getTime() + PRE_TIME, "yyyy-MM-dd HH:mm:ss.SSS");
            log.info("牵引区间故障运行图预览, 给定值4min,预览图故障结束时间为:{}", mcmEndAlarmTime);
            return mcmEndAlarmTime;
        }
        return getEndAlarmTime(alarmInfo);
    }

}
