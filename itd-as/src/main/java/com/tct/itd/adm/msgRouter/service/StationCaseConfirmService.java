package com.tct.itd.adm.msgRouter.service;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.executeHandle.common.step.CtrlSwitchHandler;
import com.tct.itd.adm.msgRouter.handle.MCMFaultHandler;
import com.tct.itd.adm.msgRouter.handle.failureRecovery.FailureRecoveryStrategy;
import com.tct.itd.adm.msgRouter.handle.failureRecovery.FailureRecoveryStrategyFactory;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.runGraph.PrePlanRunGraphContext;
import com.tct.itd.adm.service.*;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.adm.util.StrategyMatchUtil;
import com.tct.itd.basedata.dfsread.service.handle.ConStationInfoService;
import com.tct.itd.basedata.dfsread.service.handle.StopRegionDataService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.FireInfoPushAlertMsgConstant;
import com.tct.itd.common.dto.AdmRunGraphCases;
import com.tct.itd.common.dto.AlgorithmData;
import com.tct.itd.common.dto.ConStationDto;
import com.tct.itd.common.dto.Info;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.constant.NumStrConstant;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @ClassName StationCaseConfirmService
 * @Description 故障信息入库
 * @Author yl
 * @Date 2022/6/16 14:24
 */
@Slf4j
@Service
public class StationCaseConfirmService {

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AxleCounterService axleCounterService;
    @Resource
    private MCMFaultHandler mcmFaultHandler;
    @Resource
    private PlatformDoorService platformDoorService;


    @Resource
    private List<AlarmInfoMessageHandler> alarmInfoMessageHandlerList;

    private Map<String, AlarmInfoMessageHandler> alarmInfoMessageHandlerMap = new HashMap<>();

    @Resource
    private TrainDoorAlarmService trainDoorAlarmService;

    @Resource
    private PlatFormDoorOpenAlarmService platFormDoorOpenAlarmService;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource(name = "ctrlSwitch")
    private CtrlSwitchHandler ctrlSwitchHandler;

    @Resource
    private AdmAlertDetailService admAlertDetailService;

    @Resource
    private PrePlanRunGraphContext prePlanRunGraphContext;

    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;

    @Resource
    private ConStationInfoService conStationInfoService;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;

    @Resource
    private AdmAlertDetailBoxService admAlertDetailBoxService;

    @Resource
    private StopRegionDataService stopRegionDataService;

    @Resource
    private AdmStationService admStationService;

    @Resource
    
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AdmAdjustStrategyInterfaceService admAdjustStrategyInterfaceService;

    @PostConstruct
    public void init() {
        alarmInfoMessageHandlerList.forEach(
                alarmInfoMessageHandler -> alarmInfoMessageHandlerMap.put(alarmInfoMessageHandler.channel(), alarmInfoMessageHandler)
        );
    }
    public void sendAid(Integer code){
        //获取存活的alarmInfo
        AlarmInfo alarmInfo = admAlertInfoSubService.getInfoInLife();
        if(Objects.isNull(alarmInfo)){
            throw new BizException(CodeEnum.END_LIFE);
        }
        //故障类型
        String alarmTypeDetail = String.valueOf(alarmInfo.getAlarmTypeDetail());


        if(alarmInfo.getAlarmType() == Integer.parseInt(AlarmTypeConstant.TRAIN_DOOR_FAILURE)){
            trainDoorAlarmService.dealStationConfirm(alarmInfo, code);
        }else if(AlarmTypeConstant.AXLE_COUNTER_SWITCH.equals(alarmTypeDetail)){
            axleCounterService.confirmSwitchStatus(code,alarmInfo);
        }else if(AlarmTypeConstant.MORE_MCM_FAILURE.equals(alarmTypeDetail)){
            log.info("点击弹框，选择牵引掉线方式");
            mcmFaultHandler.pushSecondAdm(code);
        }else if(AlarmTypeConstant.SINGLE_PLATFORM_DOOR_CANNOT_OPEN.equals(alarmTypeDetail)){
            platformDoorService.stationConfirmSingleOpen(code,alarmInfo);
        } else if(AlarmTypeConstant.SINGLE_PLATFORM_DOOR_CANNOT_CLOSE.equals(alarmTypeDetail)){
            platformDoorService.stationConfirmSingleClose(code,alarmInfo);
        }else if(AlarmTypeConstant.ALL_PLATFORM_DOOR_CANNOT_OPEN.equals(alarmTypeDetail)){
            platformDoorService.stationConfirmAllOpen(code,alarmInfo);
        } else if(AlarmTypeConstant.ALL_PLATFORM_DOOR_CANNOT_CLOSE.equals(alarmTypeDetail)){
            platformDoorService.stationConfirmAllClose(code,alarmInfo);
        }else if (AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_INTO_STATION.equals(alarmTypeDetail) ||
                AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_OUT_STATION.equals(alarmTypeDetail)
        ){
            platFormDoorOpenAlarmService.dealStationConfirm(alarmInfo, code);
        }else if(AlarmTypeConstant.AXLE_COUNTER_ARB_RESET.equals(alarmTypeDetail)){
            axleCounterService.confirmResetArbStatus(code,alarmInfo);
        }else if(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH_RESET.equals(alarmTypeDetail)){
            axleCounterService.confirmResetNotSwitchStatus(code,alarmInfo);
        } else if(AlarmTypeConstant.AXLE_COUNTER_SWITCH_RESET.equals(alarmTypeDetail)){
            axleCounterService.confirmResetSwitchStatus(code,alarmInfo);
        }  else{
            throw new BizException("未知得故障类型");
        }
    }

    /**
     * @Author yuelei
     * @Desc 车站对话框超时放弃
     * @Date 13:50 2022/6/20
     */
    public void stationOverTimeGiveUp() {
        //获取存活的alarmInfo
        AlarmInfo alarmInfo = admAlertInfoSubService.getInfoInLife();
        if(Objects.isNull(alarmInfo)){
            return;
        }
        //终止流程图执行状态
        alarmFlowchartService.giveUp(alarmInfo.getTableInfoId());
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(),"故障车站超时未选择处置方案","0","故障车站超时未选择处理方案，流程已放弃");
        //修改应急事件状态为超时已放弃
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(),alarmInfo.getTableBoxId(),2);
        //放弃应急事件
        ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
    }

    /**
     * 车站确认调度命令下发回收
     * 中心确认道岔已摇至行车方向，具备列车通过条件
     * @author liyunlong
     * @date 2023/3/1 15:04
     * @param alarmInfo 告警信息
     */
    public void switchStationConfirm(AlarmInfo alarmInfo) {
        log.info("车站中心确认参数:【{}】", JsonUtils.toJSONString(alarmInfo));
        // 控制权转换类型 -1:推给中心的弹窗,不需要控制权转换,其它值是推给车站的弹窗,控制权转换
        int crtlSwitch = alarmInfo.getCrtlSwitch();
        if (NumConstant.NEGATIVE_ONE.equals(crtlSwitch) || NumConstant.ZERO.equals(crtlSwitch)) {
            AlarmInfo info = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
            info.setIfConfirmed(alarmInfo.getIfConfirmed());
            long start = System.currentTimeMillis();
            String channel = String.valueOf(info.getAlarmTypeDetail());
            AlarmInfoMessageHandler alarmInfoMessageHandler = alarmInfoMessageHandlerMap.get(channel);
            if (alarmInfoMessageHandler == null) {
                log.error("未找到故障类型处理类:{}", JsonUtils.toJSONString(info));
                throw new BizException(CodeEnum.ALARM_TYPE_UNHANDLED);
            }
            //故障路由
            alarmInfoMessageHandler.handle(info);
            long spend = (System.currentTimeMillis() - start) / 1000;
            log.info("{} handler exec success, spend:{}s", channel, spend);
            //推送和利时
            AlarmUtil.sendFireInfoPush(alarmInfo, FireInfoPushAlertMsgConstant.ALARM_SEND_SUCCESS);
        }
        // 控制权转换
        else {
            AidDesSubStepOutDto aidDesSubStepOutDto = new AidDesSubStepOutDto();
            aidDesSubStepOutDto.setParam(String.valueOf(crtlSwitch));
            ctrlSwitchHandler.handle(alarmInfo, aidDesSubStepOutDto);
        }
    }

    /**
     * 调度确认是否故障恢复
     * @author liyunlong
     * @date 2023/3/1 14:25
     * @param alarmInfo 告警信息
     */
    public void centerConfirm(AlarmInfo alarmInfo) {
        // 确认标识 1:确认 0:未确认
        Integer ifConfirmed = alarmInfo.getIfConfirmed();
        log.info("道岔故障恢复调度确认结果:【{}】", ifConfirmed);
        // 故障恢复
        if (NumConstant.ONE.equals(ifConfirmed)) {
            FailureRecoveryStrategy strategy = FailureRecoveryStrategyFactory.getStrategy(alarmInfo);
            strategy.pushRecoveryAdm(alarmInfo);
        } else {
            sendAuxiliaryDecisionTwo(alarmInfo);
        }
        BasicCommonCacheUtils.delKey(Cache.SWITCH_OPERATE_SIGN);
        BasicCommonCacheUtils.delKey(Cache.SWITCH_OPERATE_RECOVERY_FLAG);
        BasicCommonCacheUtils.delKey(Cache.SWITCH_OPERATE_RECOVERY);
        BasicCommonCacheUtils.delKey(Cache.MONITOR_SWITCH_FIXED);
        BasicCommonCacheUtils.delKey(Cache.MONITOR_SWITCH_INVERSE);
    }

    /**
     * 调度确认定反操作超过配置次数
     * @author liyunlong
     * @date 2023/3/1 14:25
     * @param alarmInfo 告警信息
     */
    public void overConfirm(AlarmInfo alarmInfo) {
        // 确认标识 1:确认 0:未确认
        Integer ifConfirmed = alarmInfo.getIfConfirmed();
        log.info("道岔故障调度确认定反操作超过配置次数确认结果:【{}】", ifConfirmed);
        // 继续执行道岔故障处置
        if (NumConstant.ONE.equals(ifConfirmed)) {
            sendAuxiliaryDecisionTwo(alarmInfo);
            BasicCommonCacheUtils.delKey(Cache.SWITCH_OPERATE_SIGN);
            BasicCommonCacheUtils.delKey(Cache.SWITCH_OPERATE_RECOVERY_FLAG);
            BasicCommonCacheUtils.delKey(Cache.SWITCH_OPERATE_RECOVERY);
            BasicCommonCacheUtils.delKey(Cache.MONITOR_SWITCH_FIXED);
            BasicCommonCacheUtils.delKey(Cache.MONITOR_SWITCH_INVERSE);
        }
    }

    /**
     * 推送第二次推荐指令
     * @author liyunlong
     * @date 2023/3/1 15:10
     * @param alarmInfo 告警信息
     */
    private void sendAuxiliaryDecisionTwo(AlarmInfo alarmInfo) {
        long detailId = UidGeneratorUtils.getUID();
        AdmAlertDetail alertDetail3 = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令",
                new Date(), "系统产生第二次推荐指令", NumStrConstant.ONE, NumConstant.ONE, System.currentTimeMillis());
        admAlertDetailService.insert(alertDetail3);
        // 调用算法获取调整方案列表
        AdmRunGraphCases admRunGraphCases = null;
        // 终端站折返道岔故障-具备本站折返，调用算法返回开始折返车次号
        if (Integer.valueOf(AlarmTypeConstant.SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE).equals(alarmInfo.getAlarmTypeDetail())) {
            try {
                admRunGraphCases = prePlanRunGraphContext.listPreviewRunGraph(alarmInfo);
            } catch (Exception e) {
                log.error("道岔故障推送第二次推荐指令调用算法获取调整方案列表出错,异常信息:【{}】", e.getMessage());
                // 获取运行图预览方案失败,直接结束故障流程。
                alarmInfo.setEndLife(false);
                admAlertInfoSubService.updateById(alarmInfo);
            }
            log.info("调用算法获取运行图方案列表:【{}】", admRunGraphCases);
        }
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(),
                NumConstant.TWO, NumConstant.ZERO);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        ConStationDto conStationDto =
                conStationInfoService.getStationByConStationId(Integer.valueOf(alarmInfo.getAlarmConStation()));
        // 辅助决策内同显示具体的站名
        stepList = StrategyMatchUtil.formatStepList(stepList, conStationDto);
        // 具备站前折返，判断区间是否有车需要降级开到下一站
        stepList = rebuildStepList(alarmInfo, stepList, admRunGraphCases);
        // 调中断图显示第一个折返车次
        stepList = getStepList(alarmInfo, admRunGraphCases, stepList);
        long boxId = UidGeneratorUtils.getUID();
        //推荐指令步骤执行第二步
        alarmInfo.setExecuteStep(NumConstant.TWO);
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
        // 站后折返道岔故障特殊情况,直接调缓行图(bj19)
        if (Integer.valueOf(AlarmTypeConstant.BEHIND_FAILURE_CHANGE).equals(alarmInfo.getAlarmTypeDetail())) {
            formatDisposeList(stepList, alarmInfo);
        }
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        String alarmTypeDetail = admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail());
        admIdea.setAlarmTypeDetailStr(alarmTypeDetail);
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推荐指令入库
        AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea),
                "方案待执行");
        admAlertDetailBoxService.insert(alertDetailBox);
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmInfo.setTableBoxId2(boxId);
        admAlertInfoSubService.updateById(alarmInfo);
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(8,10));
    }

    /**
     * 重新组装stepList
     * @author liyunlong
     * @date 2022/2/19 14:01
     * @param alarmInfo 告警信息
     * @param admRunGraphCases 运行如预览方案
     * @param stepList 推荐指令执行步骤
     * @return java.util.List<java.lang.String>
     */
    private List<DisposeDto> getStepList(AlarmInfo alarmInfo, AdmRunGraphCases admRunGraphCases, List<DisposeDto> stepList) {
        List<DisposeDto> newStepList = Lists.newArrayList();
        if (Integer.valueOf(AlarmTypeConstant.SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE).equals(alarmInfo.getAlarmTypeDetail())) {
            // 第一个折返车次号
            String backTrainNumber = admRunGraphCases.getAdmRunGraphCases().get(0).getBackTrainNumber();
            if (StringUtils.isEmpty(backTrainNumber)) {
                log.error("终端站折返道岔故障-具备本站折返:没有返回第一个折返车次号！");
                return stepList.stream().filter(s -> !s.getStep().contains("%s")).collect(Collectors.toList());
            }
            BasicCommonCacheUtils.set(Cache.BACK_TRAIN_NUMBER,backTrainNumber,180L, TimeUnit.SECONDS);
            String finalBackTrainNumber = backTrainNumber;
            stepList.forEach(s -> {
                if (s.getStep().contains("%s")) {
                    s.setStep(String.format(s.getStep(), finalBackTrainNumber));
                    newStepList.add(s);
                    return;
                }
                newStepList.add(s);
            });
            stepList = newStepList;
        }
        return stepList;
    }

    /**
     * 终端站折返道岔故障-具备本站折返,
     * 根据条件判断是否显示
     * "因xxx道岔故障,准许xxx次列车降级运行至xxx站台”内容
     * @author liyunlong
     * @date 2022/3/8 18:37
     * @param stepList 推荐指令内容
     * @param alarmInfo 告警信息
     * @return java.util.List<java.lang.String>
     */
    private List<DisposeDto> rebuildStepList(AlarmInfo alarmInfo, List<DisposeDto> stepList,AdmRunGraphCases admRunGraphCases) {
        String disCmdContent;
        List<DisposeDto> listStr = Lists.newArrayList();
        String switchNo = alarmInfo.getSwitchNo();
        if (Integer.valueOf(AlarmTypeConstant.SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE).equals(alarmInfo.getAlarmTypeDetail())) {
            List<String> trainNumberList = admRunGraphCases.getAdmRunGraphCases().get(0).getTrainNumberList();
            Integer nextStopRegionId = admRunGraphCases.getAdmRunGraphCases().get(0).getNextStopRegionId();
            if (CollectionUtils.isNotEmpty(trainNumberList) && Objects.nonNull(nextStopRegionId)) {
                String trainNumber = String.join("、", trainNumberList);
                Integer stationId = stopRegionDataService.getStationIdByStopAreaId(nextStopRegionId);
                String stationName = admStationService.selectByStationId(stationId).getStationName();
                DisposeDto dto = stepList.get(0);
                String cmd = dto.getStep();
                disCmdContent = String.format(cmd, alarmInfo.getSwitchName(), trainNumber, stationName);
                dto.setStep(disCmdContent);
                listStr.add(dto);
                BasicCommonCacheUtils.hPut(Cache.TRAIN_NUMBER_LIST, switchNo, trainNumberList);
                BasicCommonCacheUtils.hPut(Cache.NEXT_STOP_REGION_ID, switchNo, nextStopRegionId);
            }
            for (int i = 1; i < stepList.size(); i++) {
                listStr.add(stepList.get(i));
            }
        } else {
            listStr = stepList;
        }
        return listStr;
    }

    /**
     * 拼接推荐指令显示内容
     * @author liyunlong
     * @date 2023/3/24 15:31
     * @param list 待拼接辅助决策内容
     * @return java.util.List<com.tct.itd.dto.DisposeDto>
     */
    private void formatDisposeList (List<DisposeDto> list, AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = aidDecisionExecService.getSFAlgorithmData(alarmInfo);
        BaseResponse<String> response = admAdjustStrategyInterfaceService.getReversalTrackName(algorithmData);
        String reversalTrackName = response.getData();
        if (org.apache.commons.lang3.StringUtils.isEmpty(reversalTrackName)) {
            throw new BizException("调用算法返回的折返轨名称为空。");
        }
        list.forEach(s -> {
            String step = s.getStep();
            if (step.contains("%s")) {
                s.setStep(String.format(step, reversalTrackName));
            }
        });
    }
}
