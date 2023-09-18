package com.tct.itd.adm.runGraph.stategy;


import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
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
 * @Description 单车门无法关闭-缓行-运行图预览
 * @Author zhangyinglong
 * @Date 2021/6/2 14:27
 */
@Slf4j
@Service
public class OnlyDoorCannotCloseSlowStrategy extends AbstractPrePlanRunGraphStrategy implements IPrePlanRunGraphStrategy {

    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        return alarmInfo.getCaseCode() == 0 ? 2: alarmInfo.getCaseCode();
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        if(IidsConstPool.EXECUTE_STEP_3 == alarmInfo.getExecuteStep()){
            //预览方案，当前时间+清客时长为故障结束时间
            int time = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_CLEAR_TIME));
            return DateUtil.getTimeStamp(System.currentTimeMillis() + time);
        }
        //获取调图冗余参数,时间毫秒
        long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
        return DateUtil.getTimeStamp(System.currentTimeMillis() + setRedundancyTime);
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
                alarmInfo.setAlarmState( AlarmStateEnum.LATE_ADJUSTMENT.getCode());
                resultList = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
            }else if (IidsConstPool.EXECUTE_STEP_2 == alarmInfo.getExecuteStep()){
                //故障列车终点站回段-终点站掉线
                alarmInfo.setAlarmState(AlarmStateEnum.TERMINAL_POINT_DROP_LINE_HOLD.getCode());
                List<AlgStrategyResult> result2 = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
                result2.forEach(algStrategyResult -> {
                    algStrategyResult.setCaseCode(AlarmStateEnum.TERMINAL_POINT_DROP_LINE_HOLD.getCode());
                    algStrategyResult.setCaseDesc("故障列车终点站回段");
                });
                //列车继续运行-晚点调整
                alarmInfo.setAlarmState(AlarmStateEnum.LATE_ADJUSTMENT.getCode());
                List<AlgStrategyResult> result1 = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
                result1.forEach(algStrategyResult -> {
                    algStrategyResult.setCaseCode(AlarmStateEnum.LATE_ADJUSTMENT.getCode());
                    algStrategyResult.setCaseDesc("列车继续运行");
                });
                resultList.addAll(result1);
                resultList.addAll(result2);
            }else if(IidsConstPool.EXECUTE_STEP_3 == alarmInfo.getExecuteStep()){
                alarmInfo.setAlarmState(AlarmStateEnum.TRAIN_DOOR_STATION_DROP_LINE.getCode());
                resultList = aidDecisionExecService.adjustRunGraphDoorSlowAlg(alarmInfo);
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

    /**
     * 运行图预览
     */
    @Override
    public PrePlanRunGraph previewRunGraph(AlarmInfo alarmInfo) {
        log.info("获取方案列表时，传入故障信息：{}", JsonUtils.toJSONString(alarmInfo));
        Assert.notNull(alarmInfo, "客户端未上报故障");
        //获取运行图策略，用于预览运行图
        PrePlanRunGraph runGraph = new PrePlanRunGraph();
        try {
            if(IidsConstPool.EXECUTE_STEP_0_2 == alarmInfo.getExecuteStep()){
                alarmInfo.setAlarmState(AlarmStateEnum.LATE_ADJUSTMENT.getCode());
                runGraph = aidDecisionExecService.getPlanRunGraph(alarmInfo);
            }else if (IidsConstPool.EXECUTE_STEP_2 == alarmInfo.getExecuteStep()){
                //故障列车终点站回段-终点站掉线
                alarmInfo.setAlarmState(alarmInfo.getCaseCode());
                runGraph = aidDecisionExecService.getPlanRunGraph(alarmInfo);
            }else if(IidsConstPool.EXECUTE_STEP_3 == alarmInfo.getExecuteStep()){
                //预览方案，当前时间+清客时长为故障结束时间
                alarmInfo.setAlarmState(AlarmStateEnum.TRAIN_DOOR_STATION_DROP_LINE.getCode());
                runGraph = aidDecisionExecService.getDoorSlowPlanRunGraph(alarmInfo);
            }
        } catch (Exception e) {
            log.error("运行图预览失败,失败信息:【{}】", e.getMessage());
            ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        return runGraph;
    }

    @Override
    public String strategy() {
        return AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE_SLOW_DOWN;
    }
}
