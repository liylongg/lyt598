package com.tct.itd.adm.runGraph.stategy;


import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.common.dto.PrePlanRunGraph;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @classname: SwitchFailureStrategy
 * @description: 道岔故障策略类
 * @author: liyunlong
 * @date: 2021/12/22 11:15
 */
@Component
@Slf4j
public class SwitchFailureStrategy extends AbstractPrePlanRunGraphStrategy {

    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AppPushService appPushService;

    /**
     * 故障状态
     */
    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        return super.getAlarmState(alarmInfo);
    }

    /**
     * 故障结束时间
     */
    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        return DateUtil.getStringToDate(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
    }

    /**
     * 策略类对应的码值,供适配策略类使用
     */
    @Override
    public String strategy() {
        return null;
    }

    /**
     * 运行图预览
     */
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
        if (BasicCommonCacheUtils.existHash(Cache.SWITCH_FAILURE_TEN_TIME, alarmInfo.getSwitchNo())) {
            Long pushTime = (Long) BasicCommonCacheUtils.hGet(Cache.SWITCH_FAILURE_TEN_TIME, alarmInfo.getSwitchNo());
            //调30分钟图
            Integer switchFailureTime =
                    Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.SWITCH_FAILURE_PRRVIEW_TIME));
            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp(pushTime + switchFailureTime, "yyyy-MM-dd HH:mm:ss.SSS"));
        } else if (BasicCommonCacheUtils.existHash(Cache.SWITCH_FAILURE_TWENTY_TIME, alarmInfo.getSwitchNo())) {
            Long pushTime = (Long) BasicCommonCacheUtils.hGet(Cache.SWITCH_FAILURE_TWENTY_TIME, alarmInfo.getSwitchNo());
            // 调30分钟图
            Integer switchFailureTime =
                    Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.SWITCH_FAILURE_THIRTY_PRRVIEW_TIME));
            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp(pushTime + switchFailureTime, "yyyy-MM-dd HH:mm:ss.SSS"));
        } else {
            //获取运行图策略，用于预览运行图
            alarmInfo.setEndAlarmTime(getEndAlarmTime(alarmInfo));
        }
        PrePlanRunGraph prePlanRunGraph;
        prePlanRunGraph = aidDecisionExecService.getSFPlanRunGraph(alarmInfo);
        prePlanRunGraph.setCountDown(180);
        return prePlanRunGraph;
    }

    /**
     * 运行图方案列表
     */
    @Override
    public List<AlgStrategyResult> listAdjustStrategy(AlarmInfo alarmInfo) {
        log.info("获取方案列表时，传入故障信息：{}", JsonUtils.toJSONString(alarmInfo));
        Assert.notNull(alarmInfo, "客户端未上报故障");
        if (BasicCommonCacheUtils.existHash(Cache.SWITCH_FAILURE_TEN_TIME, alarmInfo.getSwitchNo())) {
            Long pushTime = (Long) BasicCommonCacheUtils.hGet(Cache.SWITCH_FAILURE_TEN_TIME, alarmInfo.getSwitchNo());
            // 调30分钟图
            Integer switchFailureTime = Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.SWITCH_FAILURE_PRRVIEW_TIME));
            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp(pushTime + switchFailureTime, "yyyy-MM-dd HH:mm:ss.SSS"));
        } else if (BasicCommonCacheUtils.existHash(Cache.SWITCH_FAILURE_TWENTY_TIME, alarmInfo.getSwitchNo())) {
            Long pushTime = (Long) BasicCommonCacheUtils.hGet(Cache.SWITCH_FAILURE_TWENTY_TIME, alarmInfo.getSwitchNo());
            // 调60分钟图
            Integer switchFailureTime = Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.SWITCH_FAILURE_THIRTY_PRRVIEW_TIME));
            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp(pushTime + switchFailureTime, "yyyy-MM-dd HH:mm:ss.SSS"));
        } else {
            // 故障结束时间
            alarmInfo.setEndAlarmTime(getEndAlarmTime(alarmInfo));
        }
        alarmInfo.setAlarmState(getAlarmState(alarmInfo));
        List<AlgStrategyResult> algStrategyResultList = null;
        try {
            algStrategyResultList = aidDecisionExecService.adjustRunGraphSFAlg(alarmInfo);
        } catch (BizException e) {
            log.error("获取调整方案异常,异常信息:【{}】", e.getMessage());
            appPushService.sendWebNoticeMessageToAny(
                    new WebNoticeDto(MsgPushEnum.ERROR_MSG.getCode(), "0", e.getErrorMsg()));
            ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
        } catch (Exception e) {
            log.error("获取调整方案异常,异常信息:【{}】", e.getMessage());
            ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
            throw new BizException(CodeEnum.ALG_ADJUST_FAIL_GIVE_UP);
        }
        Assert.notEmpty(algStrategyResultList, "未获取到调整方案");
        return algStrategyResultList;
    }
}
