package com.tct.itd.adm.runGraph.stategy;


import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.common.dto.PrePlanRunGraph;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description 列车空调-运行图预览
 * @Author zhangyinglong
 * @Date 2021/6/7 16:38
 */
@Slf4j
@Service
public class LargePassengerFlowStrategy extends AbstractPrePlanRunGraphStrategy{

    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;


    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        //终点站掉线
        return AlarmStateEnum.TERMINAL_POINT_DROP_LINE.getCode();
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        //故障结束时间为当前时间
        return DateUtil.getDate("yyyy-MM-dd HH:mm:ss.SSS");
    }


    @Override
    public PrePlanRunGraph previewRunGraph(AlarmInfo alarmInfo) {
        int caseCode = alarmInfo.getCaseCode();
        //获取告警信息
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo,"该故障生命周期已结束");
        alarmInfo.setCaseCode(caseCode);
        //获取运行图并返回，用于预览运行图
        PrePlanRunGraph prePlanRunGraph = aidDecisionExecService.getPlanRunGraphLargePassFlow(alarmInfo);
        //设置倒计时
//        long timestamp = DateUtil.getStringToDate(alarmInfo.getStartAlarmTime(), "yyyy-MM-dd HH:mm:ss.SSS").getTime();
//        //获取redis存储的时间，减去当前时间返回至前端,用于倒计时
//        long countDown = 180L - (System.currentTimeMillis() - timestamp)/1000;
//        //过期
//        if(countDown<0){
//            countDown = 0L;
//            log.error("缓存ADM_RUN_GRAPH_CASE_TIMESTAMP过期,倒计时置为0");
//        }
//        prePlanRunGraph.setCountDown(countDown);
        return prePlanRunGraph;
    }

    @Override
    public List<AlgStrategyResult> listAdjustStrategy(AlarmInfo alarmInfo) {
        log.info("获取方案列表时，传入故障信息：{}", JsonUtils.toJSONString(alarmInfo));
        //前端传入的alarmInfo信息不完整,需要重新从缓存获取赋值
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo,"该故障生命周期已结束");
        //TODO 获取大客流方案
        //算法入参: 运行图/备车信息/大客流时间
        List<AlgStrategyResult> algStrategyResultList;
        try {
            algStrategyResultList = aidDecisionExecService.adjustRunGraphAlgLargePassFlow(alarmInfo);
        } catch (Exception e) {
            log.error("获取运行图预览方案列表失败,失败信息:【{}】", e.getMessage());
            ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        Assert.notEmpty(algStrategyResultList,"未获取到调整方案");
        return algStrategyResultList;
    }

    @Override
    public String strategy() {
        return AlarmTypeConstant.LARGE_PASSENGER_FLOW;
    }
}
