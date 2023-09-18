package com.tct.itd.adm.runGraph.stategy;


import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.common.dto.PrePlanRunGraph;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 单车门无法打开-运行图预览
 * @Author yl
 * @Date 2021/6/2 14:27
 */
@Slf4j
@Service
public class OnlyDoorCannotOpenStrategy extends AbstractPrePlanRunGraphStrategy implements IPrePlanRunGraphStrategy {

    @Resource
    
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AdmAlertInfoService admAlertInfoService;

    @Resource
    private AppPushService appPushService;

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        return alarmInfo.getCaseCode() == 0 ? 2: alarmInfo.getCaseCode();
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        //获取调图冗余参数,时间毫秒
        long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
        //获取运行图策略，用于预览运行图
        return DateUtil.getTimeStamp( System.currentTimeMillis() + setRedundancyTime);
    }

    @Override
    public List<AlgStrategyResult> listAdjustStrategy(AlarmInfo alarmInfo) {
        log.info("获取方案列表时，传入故障信息：{}", JsonUtils.toJSONString(alarmInfo));
        Assert.notNull(alarmInfo, "客户端未上报故障");
        //获取运行图策略，用于预览运行图
        alarmInfo.setEndAlarmTime(this.getEndAlarmTime(alarmInfo));
        List<AlgStrategyResult> resultList = new ArrayList<>();
        try {
            if(IidsConstPool.EXECUTE_STEP_0_2 == alarmInfo.getExecuteStep()){
                alarmInfo.setAlarmState(IidsConstPool.ALARM_STATE_FAILOVER);
                resultList = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
            }else{
                //故障列车终点站回段-终点站掉线
                alarmInfo.setAlarmState(IidsConstPool.END_STATION_OFFLINE);
                List<AlgStrategyResult> result2 = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
                result2.forEach(algStrategyResult -> {
                    algStrategyResult.setCaseCode(IidsConstPool.END_STATION_OFFLINE);
                    algStrategyResult.setCaseDesc("故障列车终点站回段");
                });
                //列车继续运行-晚点调整
                alarmInfo.setAlarmState(IidsConstPool.ALARM_STATE_FAILOVER);
                List<AlgStrategyResult> result1 = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
                result1.forEach(algStrategyResult -> {
                    algStrategyResult.setCaseCode(IidsConstPool.ALARM_STATE_FAILOVER);
                    algStrategyResult.setCaseDesc("列车继续运行");
                });
                resultList.addAll(result1);
                resultList.addAll(result2);
            }
        } catch (Exception e) {
            log.error("获取运行图预览方案列表失败,失败信息:【{}】", e.getMessage());
            ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        Assert.notEmpty(resultList, "未获取到调整方案");
        //更新结束时间到数据库
        admAlertInfoSubService.updateEndTime(alarmInfo);
        return resultList;
    }


    @Override
    public PrePlanRunGraph previewRunGraph(AlarmInfo alarmInfo) {
        int caseCode = alarmInfo.getCaseCode();
        int executeStep1 = alarmInfo.getExecuteStep();
        //获取告警信息
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo, "当前故障生命周期已结束");
        alarmInfo.setCaseCode(caseCode);
        alarmInfo.setAlarmState(caseCode);
        //前端执行，推送执行步骤为晚点恢复，则重新赋值
        if (executeStep1 == IidsConstPool.EXECUTE_STEP_0_2) {
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_2);
            alarmInfo.setAlarmState(2);
        }
        //获取运行图并返回，用于预览运行图
        return aidDecisionExecService.getPlanRunGraph(alarmInfo);
    }

    @Override
    public String strategy() {
        return AlarmTypeConstant.ONLY_DOOR_CANNOT_OPEN;
    }
}
