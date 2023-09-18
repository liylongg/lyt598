package com.tct.itd.adm.runGraph;


import com.tct.itd.adm.convert.AlarmInfoConvert;
import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.runGraph.stategy.IPrePlanRunGraphStrategy;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.StrategyMatchUtil;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.AdmRunGraphCases;
import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.common.dto.PrePlanRunGraph;
import com.tct.itd.common.dto.TiasTraceInfo;

import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * @Description 运行图预览Context
 * @Author zhangyinglong
 * @Date 2021/6/2 14:09
 */
@Slf4j
@Component
public class PrePlanRunGraphContext {

    @Resource
    private List<IPrePlanRunGraphStrategy> graphDataStrategyList;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    private Map<String, IPrePlanRunGraphStrategy> graphDataStrategyHashMap = new HashMap<>();
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AlarmInfoConvert alarmInfoConvert;
    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;
    /**
     * @Description 建立映射关系
     * @Author zhangyinglong
     * @Date 2021/6/2 14:24
     */
    @PostConstruct
    public void init() {
        graphDataStrategyList.forEach(
                graphDataStrategy -> graphDataStrategyHashMap.put(graphDataStrategy.strategy(), graphDataStrategy)
        );
    }

    public AdmRunGraphCases listPreviewRunGraph(AlarmInfo alarmInfo) {
        int executeStep1 = alarmInfo.getExecuteStep();
        //通过infoId获取应急事件信息
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo, "该故障生命周期已结束");
        if (executeStep1 == IidsConstPool.EXECUTE_STEP_0_2) {
            alarmInfo.setExecuteStep(executeStep1);
        }
        if (executeStep1 == IidsConstPool.EXECUTE_STEP_0_1 &&
                (alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.SIGNAL_ELECTRIC) ||
                 alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.INTERLOCKING_DOUBLE))) {
            alarmInfo.setExecuteStep(executeStep1);
        }
        // 故障类型
        int alarmType = alarmInfo.getAlarmType();
        // 故障子类型
        String strategy = String.valueOf(alarmInfo.getAlarmTypeDetail());
        // 执行推荐指令步骤
        int executeStep = alarmInfo.getExecuteStep();
        strategy = StrategyMatchUtil.matchStrategy(alarmType, strategy, executeStep);
        IPrePlanRunGraphStrategy graphDataStrategy = graphDataStrategyHashMap.get(strategy);
        if (graphDataStrategy == null) {
            log.error("未找到故障类型对应的运行图预览策略类");
            throw new BizException(CodeEnum.ALARM_TYPE_UNHANDLED);
        }
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);

        //处理特殊情况：在折返轨换端前录入故障，在换端后调图
        if (isAirConditionBroadcastMCM(alarmInfo1) && Objects.equals(alarmInfo1.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
            String trainId = alarmInfo1.getTrainId();
            TiasTraceInfo traceInfo = trainTraceCache.getIfPresent(trainId);
            //设置OrderNumber
            if (Objects.isNull(traceInfo)) {
                log.error("根据车组号【{}】获取车次追踪数据失败！", trainId);
                throw new BizException(CodeEnum.NO_GET_TRAIN_TRACE);
            }
            alarmInfo1.setOrderNum(traceInfo.getOrderNumber());
        }

        List<AlgStrategyResult> prePlanRunGraphList = graphDataStrategy.listAdjustStrategy(alarmInfo1);
        AdmRunGraphCases admRunGraphCases = alarmInfoConvert.alarmInfoToGraphCases(alarmInfo);
        log.info("预览运行图AlarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
        admRunGraphCases.setTitle(TitleUtil.getTitle(alarmInfo));
        admRunGraphCases.setAdmRunGraphCases(prePlanRunGraphList);
        admRunGraphCases.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
        return admRunGraphCases;
    }

    public PrePlanRunGraph previewRunGraph(AlarmInfo alarmInfo) {
        int caseCode = alarmInfo.getCaseCode();
        int executeStep1 = alarmInfo.getExecuteStep();
        //获取故障信息
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo, "该故障生命周期已结束");
        //前端执行，推送执行步骤为晚点恢复，则重新赋值
        if (executeStep1 == IidsConstPool.EXECUTE_STEP_0_2) {
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_2);
        }
        if (executeStep1 == IidsConstPool.EXECUTE_STEP_0_1 &&
                (alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.SIGNAL_ELECTRIC) ||
                        alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.INTERLOCKING_DOUBLE))) {
            alarmInfo.setExecuteStep(executeStep1);
        }

        alarmInfo.setCaseCode(caseCode);
        // 故障类型
        int alarmType = alarmInfo.getAlarmType();
        // 故障子类型
        String strategy = String.valueOf(alarmInfo.getAlarmTypeDetail());
        // 执行推荐指令步骤
        int executeStep = alarmInfo.getExecuteStep();
        log.info("匹配运行图预览参数,alarmType:【{}】,strategy:【{}】,executeStep:【{}】", alarmType, strategy, executeStep);
        strategy = StrategyMatchUtil.matchStrategy(alarmType, strategy, executeStep);
        log.info("匹配运行图预览结果,strategy:【{}】", strategy);
        IPrePlanRunGraphStrategy graphDataStrategy = graphDataStrategyHashMap.get(strategy);
        log.info("匹配运行图策略类:【{}】", graphDataStrategy);
        if (graphDataStrategy == null) {
            log.error("未找到故障类型对应的运行图预览策略类");
            throw new BizException(CodeEnum.ALARM_TYPE_UNHANDLED);
        }
        PrePlanRunGraph prePlanRunGraph = graphDataStrategy.previewRunGraph(alarmInfo);
//        int countDown = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.PREVIEW_RUN_GRAPH_TIME));
//        //设置预览图弹窗倒计时
//        prePlanRunGraph.setCountDown(countDown);
        prePlanRunGraph.setCountDown(0L);
        return prePlanRunGraph;
    }

    private boolean isAirConditionBroadcastMCM(AlarmInfo alarmInfo) {
        return Objects.equals(alarmInfo.getAlarmType(), Integer.parseInt(AlarmTypeConstant.AIR_CONDITIONING_FAILURE)) ||
                Objects.equals(alarmInfo.getAlarmType(), Integer.parseInt(AlarmTypeConstant.BROADCAST_FAILURE)) ||
                Objects.equals(alarmInfo.getAlarmType(), Integer.parseInt(AlarmTypeConstant.MCM_FAILURE));
    }
}