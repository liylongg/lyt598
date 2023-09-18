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
 * @Description 全列多车门无法关闭-运行图预览
 * @Author yhf
 * @Date 2021-7-7
 */
@Slf4j
@Service
public class AllDoorCannotCloseStrategy extends AbstractPrePlanRunGraphStrategy{

    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    /**
     * 字符串常量 “0”
     */
    public static final String ZERO_STR = "0";

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_8){
            return AlarmStateEnum.LATE_ADJUSTMENT.getCode();
        }
        //1:立即掉线
        return AlarmStateEnum.STATION_DROP_LINE.getCode();
    }

    /**
     * 全列车门无法关闭,执行第二次推荐指令方案 故障时长为一分钟 清客时间为一分钟
     * @param alarmInfo
     * @return
     */
    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        //故障恢复调图，获取当前时间
        if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_2){
            //获取调图冗余参数,时间毫秒
            long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
            return DateUtil.getTimeStamp(System.currentTimeMillis() + setRedundancyTime);
        }else if(IidsConstPool.EXECUTE_STEP_5 == alarmInfo.getExecuteStep() || IidsConstPool.EXECUTE_STEP_6 == alarmInfo.getExecuteStep()){
            //推送15分钟图
            int time = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.DOOR_FAILURE_THIRTY_PREVIEW_TIME));
            return DateUtil.getTimeStamp(System.currentTimeMillis() + time);
        }else{
            //通用清客时长
            int clearGuest = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_CLEAR_TIME)) * 1000;
            return DateUtil.getTimeStamp(System.currentTimeMillis() + clearGuest);
        }
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
            }else if (IidsConstPool.EXECUTE_STEP_2 == alarmInfo.getExecuteStep()){
                //全列车门-立刻掉线
                alarmInfo.setAlarmState(AlarmStateEnum.STATION_DROP_LINE.getCode());
                List<AlgStrategyResult> result2 = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
                resultList.addAll(result2);
            }else if(IidsConstPool.EXECUTE_STEP_5 == alarmInfo.getExecuteStep() || IidsConstPool.EXECUTE_STEP_6 == alarmInfo.getExecuteStep()){
                resultList = aidDecisionExecService.adjustRunGraphDoorAlg(alarmInfo);
            }else if(IidsConstPool.EXECUTE_STEP_7 == alarmInfo.getExecuteStep()){
                alarmInfo.setAlarmState(AlarmStateEnum.TRAIN_DOOR_STATION_DROP_LINE.getCode());
                resultList = aidDecisionExecService.adjustRunGraphDoorAlg(alarmInfo);
            }else if (IidsConstPool.EXECUTE_STEP_8 == alarmInfo.getExecuteStep()){
                alarmInfo.setAlarmState(AlarmStateEnum.STATION_DROP_LINE.getCode());
                resultList = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
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
                alarmInfo.setAlarmState(IidsConstPool.ALARM_STATE_FAILOVER);
                runGraph = aidDecisionExecService.getPlanRunGraph(alarmInfo);
            }else if (IidsConstPool.EXECUTE_STEP_2 == alarmInfo.getExecuteStep()){
                //故障列车立即清人掉线
                alarmInfo.setAlarmState(AlarmStateEnum.STATION_DROP_LINE.getCode());
                runGraph = aidDecisionExecService.getPlanRunGraph(alarmInfo);
            }else if(IidsConstPool.EXECUTE_STEP_5 == alarmInfo.getExecuteStep() || IidsConstPool.EXECUTE_STEP_6 == alarmInfo.getExecuteStep()){
                runGraph = aidDecisionExecService.getDoorPlanRunGraph(alarmInfo);
            }else if(IidsConstPool.EXECUTE_STEP_7 == alarmInfo.getExecuteStep()){
                alarmInfo.setAlarmState(AlarmStateEnum.TRAIN_DOOR_STATION_DROP_LINE.getCode());
                runGraph = aidDecisionExecService.getDoorPlanRunGraph(alarmInfo);
            }else if (IidsConstPool.EXECUTE_STEP_8 == alarmInfo.getExecuteStep()){
                alarmInfo.setAlarmState(AlarmStateEnum.STATION_DROP_LINE.getCode());
                runGraph = aidDecisionExecService.getPlanRunGraph(alarmInfo);
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
        return AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE;
    }
}
