package com.tct.itd.adm.controller.adm;

import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.handle.InterLockingDoubleHandler;
import com.tct.itd.adm.msgRouter.handle.SignalElectricSourceHandler;
import com.tct.itd.adm.msgRouter.handle.VirtualGroupHandler;
import com.tct.itd.adm.msgRouter.router.AlarmInfoRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionRouter;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AxleCounterService;
import com.tct.itd.adm.msgRouter.service.StationCaseConfirmService;
import com.tct.itd.adm.msgRouter.service.SwitchFailureService;
import com.tct.itd.adm.runGraph.PrePlanRunGraphContext;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AdmGraphRecordService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.GraphDataUtil;
import com.tct.itd.client.GraphClient;
import com.tct.itd.common.dto.*;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.TrainGraphDto;
import com.tct.itd.exception.BizException;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.GZIPUtil;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Description: 推荐指令api
 * Author: zhangyinglong
 * Date:2021/5/18 11:31
 */

@Slf4j
@RestController
public class AdmAidDecisionController {

    /**
     * 数字常量 1
     */
    public static final int ONE_NUM = 1;

    @Resource
    private AuxiliaryDecisionRouter auxiliaryDecisionRouter;

    
    @Resource
    private AlarmInfoRouter alarmInfoRouter;

    @Resource
    private PrePlanRunGraphContext prePlanRunGraphContext;

    @Resource
    private AdmCommonMethodService admCommonMethodService;

    @Resource
    private SwitchFailureService switchFailureService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AxleCounterService axleCounterService;

    @Resource
    private AdmGraphRecordService admGraphRecordService;

    @Resource
    private InterLockingDoubleHandler interLockingDoubleHandler;

    @Resource
    private SignalElectricSourceHandler signalElectricSourceHandler;

    
    @Resource
    private StationCaseConfirmService stationCaseConfirmService;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource
    private GraphClient graphClient;

    @Resource
    private VirtualGroupHandler virtualGroupHandler;

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;


    @PostMapping("/test")
    public Map<Integer, Integer> test(@RequestParam Integer park) {
        Map<Integer, Integer> map = new HashMap<>(16);
        map.put(1, 1);
        map.put(2, 2);
        return map;
    }

    @PostMapping("/test1")
    public void test1(Integer executeStep, Integer alarmType, Integer alarmTypeDetail) {
        AlarmInfo alarmInfo = new AlarmInfo();
        alarmInfo.setExecuteStep(executeStep);
        alarmInfo.setAlarmType(alarmType);
        alarmInfo.setAlarmTypeDetail(alarmTypeDetail);
        alarmInfo.setStartAlarmTime(DateUtil.getTimeStamp());
        alarmInfo.setTrainId("12003");
        alarmInfo.setAlarmState(2);
        //补充前端录入故障参数
        alarmInfoRouter.setAlarmInfoParam(alarmInfo);
        TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent("12003");
        alarmInfo.setStopAreaNumber(tiasTraceInfo.getStopAreaNumber());
        //将故障流程图数据持久化到数据库
        alarmFlowchartService.saveFlowchart(alarmInfo);
        //路由到到各辅助决策
        alarmInfoRouter.alarmInfoRouter(alarmInfo);
    }

    @PostMapping("/test2")
    public void test2(Integer executeStep) {
        AlarmInfo alarmInfo = admAlertInfoSubService.getInfoInLife();
        alarmInfo.setExecuteStep(executeStep);
        virtualGroupHandler.handle(alarmInfo);
    }

    /**
     * @Description: 接收前端执行推荐指令请求，执行或放弃推荐指令
     * @Author: zhangyinglong
     * @Date:2021/5/18 14:42
     */
    @PostMapping("/aid_decision/execute")
    public void execAidDecision(@RequestBody AuxiliaryDecision auxiliaryDecision) {
        //TODO: 将业务逻辑移到handler中去
        //根据类型选择推荐指令并执行
        log.info("收到执行推荐指令:{}", JsonUtils.toJSONString(auxiliaryDecision));
        auxiliaryDecisionRouter.auxiliaryRouter(auxiliaryDecision);
    }

    /**
     * @param alarmInfo 故障信息
     * @Description 接收前端录入故障信息
     * @Date 11:04 2020/12/1
     **/
    @PostMapping("/alert_info/send_info")
    public void sendInfoToTias(@RequestBody AlarmInfo alarmInfo) {
        if (log.isDebugEnabled()) {
            log.debug("收到前端录入故障信息:{}", JsonUtils.toJSONString(alarmInfo));
        }
        //补充前端录入故障参数
        alarmInfoRouter.setAlarmInfoParam(alarmInfo);
        //将故障流程图数据持久化到数据库
        alarmFlowchartService.saveFlowchart(alarmInfo);
        //路由到到各推荐指令
        alarmInfoRouter.alarmInfoRouter(alarmInfo);
    }

    /**
     * @param alarmInfo 故障信息
     * @return com.tct.model.vo.tias.push.PrePlanRunGraph
     * @Description 根据故障类型和选择的调图方案获取预览运行图
     * @Author zhangyinglong
     * @Date 2021/6/2 20:20
     */
    @PostMapping("/runGraph/preview")
    public PrePlanRunGraph previewRunGraph(@RequestBody AlarmInfo alarmInfo) {
        log.info("根据故障类型和选择的调图方案获取预览运行图:{}", JsonUtils.toJSONString(alarmInfo));
        PrePlanRunGraph prePlanRunGraph = prePlanRunGraphContext.previewRunGraph(alarmInfo);
        prePlanRunGraph.setCaseCode(alarmInfo.getCaseCode());
        prePlanRunGraph.setTableInfoId(alarmInfo.getTableInfoId());
        return prePlanRunGraph;
    }

    /**
     * @param alarmInfo 故障信息
     * @return java.util.List<com.tct.model.vo.tias.push.PrePlanRunGraphStrategy>
     * @Description 运行图调整方案列表
     * @Author zhangyinglong
     * @Date 2021/6/7 11:24
     */
    @PostMapping("/runGraph/case")
    public AdmRunGraphCases listRunGraphCase(@RequestBody AlarmInfo alarmInfo) {
        AdmRunGraphCases admRunGraphCases = prePlanRunGraphContext.listPreviewRunGraph(alarmInfo);
        admRunGraphCases.setExecuteStep(alarmInfo.getExecuteStep());
        List<AlgStrategyResult> resultList = admRunGraphCases.getAdmRunGraphCases();
        resultList.forEach(result -> {
            result.setAdjustStatisticalDtoList(GraphDataUtil.generateAdjustStatisticalResult(result.getTrainNumberAdjustDtoList()));
            result.setTrainNumberAdjustDtoList(null);
            result.setBackTrainNumber(null);
            result.setRecoveryTrainDtoList(null);
            result.setCompressTrainGraphString(null);
            result.setBeforeTrainGraphString(null);
            result.setSlowlyTrainDtoList(null);
            result.setRecoverySlowlyTrainDtoList(null);
        });
        admRunGraphCases.setTitle(TitleUtil.getTitle(alarmInfo));
        return admRunGraphCases;
    }

    /**
     * @param alarmInfo 故障信息
     * @Description 确认选择的运行图方案
     * @Author zhangyinglong
     * @Date 2021/6/3 16:33
     */
    @PostMapping("/runGraph/confirm")
    public void confirmPreviewRunGraph(@RequestBody AlarmInfo alarmInfo) {
        //更新caseCode
        admAlertInfoSubService.updateCaseCodeByInfoId(alarmInfo);
        //判断故障类型是否为只有第一次推荐指令，如果是的话，则将预览图执行按钮置灰，否则可以预览运行图
        admCommonMethodService.disablePreviewButtonWhenOnlyOneAdm(alarmInfo);
        //某些场景在确认方案后直接调图
        admCommonMethodService.changeGraphAfterCase(alarmInfo);
    }

    /**
     * 运行图预览按钮置灰
     *
     * @param alarmInfo 故障信息
     * @author liyunlong
     * @date 2021/11/13 10:26
     */
    @PostMapping("/disable/previewButton")
    public Integer disablePreviewButton(@RequestBody AlarmInfo alarmInfo) {
        return admCommonMethodService.disablePreviewButton(alarmInfo);
    }

    /**
     * 故障续报
     *
     * @param alarmInfo 告警信息
     * @author liyunlong
     * @date 2021/12/23 16:34
     */
    @PostMapping("/switch/report")
    public void report(@RequestBody AlarmInfo alarmInfo) {
        if (Objects.isNull(alarmInfo)) {
            log.error("接收到客户端故障续报故障信息为空!");
            return;
        }
        log.info("接收到客户端故障续报故障信息AlarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
        long tableInfoId = alarmInfo.getTableInfoId();
        AlarmInfo alarmInfoExists = admAlertInfoSubService.queryByInfoId(tableInfoId);
        Assert.notNull(alarmInfoExists, "生命周期已结束，推荐指令流程结束");
        String alarmType = String.valueOf(alarmInfoExists.getAlarmType());
        if(alarmType.equals(AlarmTypeConstant.SWITCH_FAILURE)){
            switchFailureService.sendAuxiliaryDecisionThree(tableInfoId);
        } else if (alarmType.equals(AlarmTypeConstant.INTERLOCKING_DOUBLE_PARENT)) {
            interLockingDoubleHandler.pushSecondAdm(tableInfoId);
        } else if (alarmType.equals(AlarmTypeConstant.SIGNAL_ELECTRIC_PARENT)) {
            signalElectricSourceHandler.pushSecondAdm(tableInfoId);
        }else {
            throw new BizException("该故障类型不支持故障续报");
        }
    }

    /**
     * @param detailId id
     * @return com.tct.model.vo.algorithm.AdmRunGraphCases
     * @Description 获取方案历史
     * @Author yuelei
     * @Date 2022/5/4 15:01
     */
    @PostMapping("/runGraph/case/history")
    public AlgStrategyHistory listRunGraphCaseHistory(@RequestParam("detailId") long detailId) {
        return admGraphRecordService.queryAdjustCase(detailId);
    }

    /**
     * @return void
     * @Description 查询运行图前后对比
     * @Author yuelei
     * @Date 2022/3/7 10:17
     */
    @GetMapping("/graph/compare")
    public AdmGraphRecordDto queryGraphCompare(@RequestParam("id") long id) {
        return admGraphRecordService.getRecord(id);
    }

    /**
     * (接触网失电故障)弹窗确认接口
     *
     * @param popDto 确认结果 1 是 2 否
     */
    @PostMapping("power/pop/confirm")
    public void powerPopConfirm(@RequestBody PopDto popDto) {
        admCommonMethodService.popRouter(popDto.getChoice(),popDto.getCode());
    }

    /**
     * @Author yuelei
     * @Description 车站确认方案
     * @Date 14:41 2022/6/16
     */
    @PostMapping("station/confirm")
    public void stationCaseConfirm(@RequestBody PopDto popDto) {
        stationCaseConfirmService.sendAid(popDto.getCode());
    }


    /**
     * 车站确认调度命令下发回收
     * 中心确认道岔已摇至行车方向，具备列车通过条件
     *
     * @param alarmInfo 告警信息
     * @author liyunlong
     * @date 2022/7/27 16:24
     */
    @PostMapping("switch/station/confirm")
    public void stationCaseConfirm(@RequestBody AlarmInfo alarmInfo) {
        stationCaseConfirmService.switchStationConfirm(alarmInfo);
    }

    /**
     * 调度确认是否故障恢复
     * @author liyunlong
     * @date 2023/3/1 14:25
     * @param alarmInfo 告警信息
     */
    @PostMapping("switch/recovery/confirm")
    public void centerCaseConfirm(@RequestBody AlarmInfo alarmInfo) {
        stationCaseConfirmService.centerConfirm(alarmInfo);
    }

    /**
     * 调度确认定反操作超过配置次数
     * @author liyunlong
     * @date 2023/3/1 14:25
     * @param alarmInfo 告警信息
     */
    @PostMapping("switch/over/confirm")
    public void overConfirm(@RequestBody AlarmInfo alarmInfo) {
        stationCaseConfirmService.overConfirm(alarmInfo);
    }

    /**
     * @Author yuelei
     * @Desc 车站超时放弃
     * @Date 11:59 2022/6/20
     */
    @PostMapping("/station/give/up")
    public void stationOverTimeGiveUp() {
        stationCaseConfirmService.stationOverTimeGiveUp();
    }

    /**
     * @description: 获取应急事件流程图
     * @author: yuzhenxin
     * @date: 2022/7/14 17:51
     **/
    @GetMapping("/flowchart/generateFlowchart")
    public FlowchartNode generateFlowchart(@RequestParam("infoId") Long infoId) {
        return alarmFlowchartService.getFlowchartNodeByInfoId(infoId);
    }
}

