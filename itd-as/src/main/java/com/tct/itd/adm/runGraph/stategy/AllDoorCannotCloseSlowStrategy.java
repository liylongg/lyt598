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
 * @Description 全列多车门无法关闭-缓行-运行图预览
 * @Author yl
 * @Date 2022-8-5
 */
@Slf4j
@Service
public class AllDoorCannotCloseSlowStrategy extends AbstractPrePlanRunGraphStrategy{

    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_2){
            return AlarmStateEnum.LATE_ADJUSTMENT.getCode();
        }else if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4
                || alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_5){
            return AlarmStateEnum.TRAIN_DOOR_STATION_DROP_LINE.getCode();
        }else{
            return AlarmStateEnum.STATION_DROP_LINE.getCode();
        }
    }

    /**
     * 全列车门无法关闭,执行第二次推荐指令方案 故障时长为一分钟 清客时间为一分钟
     * @param alarmInfo
     * @return
     */
    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        //故障恢复调图，获取当前时间
        if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_2
            || alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4
        ){
            //通用清客时长
            int clearGuest = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_CLEAR_TIME)) * 1000;
            return DateUtil.getTimeStamp(System.currentTimeMillis() + clearGuest);
        }else{
            //获取调图冗余参数,时间毫秒
            long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
            return DateUtil.getTimeStamp(System.currentTimeMillis() + setRedundancyTime);
        }
    }

    @Override
    public List<AlgStrategyResult> listAdjustStrategy(AlarmInfo alarmInfo) {
        log.info("获取方案列表时，传入故障信息：{}", JsonUtils.toJSONString(alarmInfo));
        Assert.notNull(alarmInfo, "客户端未上报故障");
        //获取运行图策略，用于预览运行图
        alarmInfo.setEndAlarmTime(this.getEndAlarmTime(alarmInfo));
        alarmInfo.setAlarmState(this.getAlarmState(alarmInfo));
        List<AlgStrategyResult> resultList = new ArrayList<>();
        try {
            if(IidsConstPool.EXECUTE_STEP_0_2 == alarmInfo.getExecuteStep()
                || IidsConstPool.EXECUTE_STEP_2 == alarmInfo.getExecuteStep()
            ){
                resultList = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
            }else if(IidsConstPool.EXECUTE_STEP_4 == alarmInfo.getExecuteStep()){
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
        alarmInfo.setAlarmState(this.getAlarmState(alarmInfo));
        PrePlanRunGraph runGraph = new PrePlanRunGraph();
        try {
            if(IidsConstPool.EXECUTE_STEP_0_2 == alarmInfo.getExecuteStep()
                    || IidsConstPool.EXECUTE_STEP_2 == alarmInfo.getExecuteStep()
            ){
                runGraph = aidDecisionExecService.getPlanRunGraph(alarmInfo);
            }else if(IidsConstPool.EXECUTE_STEP_4 == alarmInfo.getExecuteStep()){
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
        return AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE_SLOW_DOWN;
    }
}
