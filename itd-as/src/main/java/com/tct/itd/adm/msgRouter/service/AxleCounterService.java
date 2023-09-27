package com.tct.itd.adm.msgRouter.service;

import com.alibaba.excel.util.CollectionUtils;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.router.AlarmInfoRouter;
import com.tct.itd.adm.service.*;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.basedata.dfsread.service.handle.AxisInfoService;
import com.tct.itd.basedata.dfsread.service.handle.ConStationInfoService;
import com.tct.itd.basedata.dfsread.service.handle.LogicSectionDataService;
import com.tct.itd.basedata.dfsread.service.handle.PlatformInfoService;
import com.tct.itd.client.AlgSwitchClient;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.*;
import com.tct.itd.common.enums.FlowchartFlagEnum;
import com.tct.itd.common.enums.StationConfirmEnum;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.enums.PlanRunGraphEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.hub.service.PlanRunGraphService;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.util.AppPushServiceUtil;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kangyi
 * @description 计轴故障业务
 * @date 2022年 01月17日 16:44:02
 */
@Service
@Slf4j
public class AxleCounterService {

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AdmStationService admStationService;
    @Resource
    private AdmAlertDetailService admAlertDetailService;
    @Resource
    private AdmAlertDetailBoxService admAlertDetailBoxService;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AlgSwitchClient algSwitchClient;
    @Resource
    private AxisInfoService axisInfoService;
    @Resource
    private LogicSectionDataService logicSectionDataService;
    @Resource
    private PlanRunGraphService planRunGraphService;
    @Resource
    private AdmAlertInfoUpgradeService admAlertInfoUpgradeService;
    @Resource
    private ConStationInfoService conStationInfoService;
    @Resource
    private PlatformInfoService platformInfoService;
    @Resource
    private AxleCounterPreUpdateFlowchartsService axleCounterUpdateFlowchartsService;
    @Resource
    private AxleCounterResetUpdateFlowchartsService axleCounterResetUpdateFlowchartsService;
    @Resource(name = "ciDeviceStatusCache")
    private com.github.benmanes.caffeine.cache.Cache<String, CiDeviceInfo> ciDeviceStatusCache;
    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    /**
     * 影响行车的步骤beanname
     */
    private static final String[] AFFECT_DRIVING_STR = {"holdTrainGroup", "sendGraphCase", "deleteHoldTrain", "doorCancelHoldTrain"};

    //ATS区段状态数据非arb标志
    private static final String NOT_ARB = "0";
    //ATS区段状态数据通信/非通信车占用
    private static final String IN_USE = "0";
    //ATS区段状态数据进路锁闭
    private static final String LOCK = "1";
    //计轴故障延时上报时间默认3秒
    private static final String AXLE_COUNTER_DELAY = "3000";

    /**
     * @param tableInfoId 接收到的故障信息id
     * @description 计轴故障续报
     * @date 2022/1/17 16:44
     * @author kangyi
     * @return: void
     */
    public void continueReport(long tableInfoId) {
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(tableInfoId);
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.PRE_FAIL_FLAG, String.valueOf(alarmInfo.getExecuteStep()));
        if (alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_SWITCH) && alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4) {
            log.info("接收到故障续报，开始生成第五次推荐指令");
            pushFiveAuxiliaryDecision(alarmInfo);
        } else {
            log.info("接收到故障续报，开始生成第二次推荐指令");
            pushSecondAuxiliaryDecision(alarmInfo);
        }
        axleCounterUpdateFlowchartsService.updateContinueReportFlowcharts(alarmInfo);
    }

    //计轴预复位成功，推送推荐指令
    public void preResetSuccess(AlarmInfo alarmInfo, AxisInfoDto axleCiDeviceInfo) {
        String typeName = AdmAlertDetailTypeService.getTypeNameByCode(String.valueOf(alarmInfo.getAlarmTypeDetail()));
        if(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH.equals(typeName)){
            pushAuxiliaryDecision(alarmInfo, IidsConstPool.EXECUTE_STEP_2, IidsConstPool.DISPOSE_STEP_TYPE_ONE);
            //预复位成功标志放流程图缓存
            BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.PRE_SUCCESS_FLAG, String.valueOf(IidsConstPool.EXECUTE_STEP_2));
            //更新流程图
            axleCounterUpdateFlowchartsService.preResetSuccessNotSwitch(alarmInfo);
        }
        if(AlarmTypeConstant.AXLE_COUNTER_SWITCH.equals(typeName)){
            int executeStep;
            int stepType = IidsConstPool.DISPOSE_STEP_TYPE_ONE;
            if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_3) {
                executeStep = IidsConstPool.EXECUTE_STEP_4;
                //推送车站提示框，关闭划轴结果弹窗
                appPushService.sendMessage(MsgTypeEnum.STATION_RECOVER_ALERT_WID, new WebNoticeDto(CommandEnum.STATION_RECOVER_ALERT_WID.getMsgCode(), String.valueOf(alarmInfo.getStationId()), axleCiDeviceInfo.getAxisName() + "已预复位成功"));
                BasicCommonCacheUtils.delKey(Cache.AXLE_COUNTER_CONFIRM);
                BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.PRE_SUCCESS_FLAG, String.valueOf(IidsConstPool.EXECUTE_STEP_3));
            } else if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4) {
                BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.PRE_SUCCESS_FLAG, String.valueOf(IidsConstPool.EXECUTE_STEP_4));
                executeStep = IidsConstPool.EXECUTE_STEP_5;
            } else {
                executeStep = IidsConstPool.EXECUTE_STEP_2;
                //预复位成功标志放流程图缓存
                BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.PRE_SUCCESS_FLAG, String.valueOf(IidsConstPool.EXECUTE_STEP_2));
            }
            //推送推荐指令
            pushAuxiliaryDecision(alarmInfo, executeStep, stepType);
            //更新流程图
            axleCounterUpdateFlowchartsService.preResetSuccessSwitch(alarmInfo);
        }
    }

    private void pushAuxiliaryDecision(AlarmInfo alarmInfo, int executeStep, int stepType) {
        //中文步骤
        String uppercaseStep = NumberUtil.number2chinese(executeStep);
        long detailId = UidGeneratorUtils.getUID();
        //插入生成推荐指令方案日志
        admAlertDetailService.insert(new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第" + uppercaseStep + "次推荐指令", new Date(), "系统产生第" + uppercaseStep + "次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis()));
        long boxId = UidGeneratorUtils.getUID();
        //推荐指令步骤执行
        alarmInfo.setExecuteStep(executeStep);
        String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
        //获取推送对象
        AdmIdea admIdea = getAdmIdea(alarmInfo, String.valueOf(alarmInfo.getAlarmTypeDetail()), executeStep, stepType, descByCode);
        log.info("获取到推送第" + uppercaseStep + "次推荐指令对象AdmIdea:{}", JsonUtils.toJSONString(admIdea));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推荐指令入库
        admAlertDetailBoxService.insert(new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行"));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        alarmInfo.setTableBoxId2(boxId);
        //设置状态未执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
        admAlertInfoSubService.updateById(alarmInfo);
        //推送推荐指令
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("生成第" + uppercaseStep + "次推荐指令提示成功!");
    }

    public AxleStopAreaResult getAlgorithmResult(AlarmInfo alarmInfo) {
        log.info("开始获取原运行图");
        //获取原运行图
        String planGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        log.info("获取原运行图完成");
        BasicCommonCacheUtils.set(Cache.PLAN_GRAPH, planGraph);
        AlgorithmAxleCounterParam algorithmAxleCounterParam = new AlgorithmAxleCounterParam();
        algorithmAxleCounterParam.setAxleCounterId(alarmInfo.getAxleCounterId());
        algorithmAxleCounterParam.setAxleCounterName(alarmInfo.getAxleCounterName());
        algorithmAxleCounterParam.setCompressTrainGraphString(planGraph);
        algorithmAxleCounterParam.setAlarmStartTime(DateUtil.getTimeStamp());
        log.info("开始获取算法停车区域和上下行");
        BaseResponse<AxleStopAreaResult> axleStopAreaResp = algSwitchClient.getAxleStopArea(algorithmAxleCounterParam);
        if (axleStopAreaResp.getCode() != CodeEnum.SUCCESS.getCode()) {
            log.info("获取算法停车区域和上下行异常,错误码{},信息:{}", axleStopAreaResp.getCode(), axleStopAreaResp.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL.getCode(), axleStopAreaResp.getMessage());
        }
        return axleStopAreaResp.getData();
    }

    public AdmIdea getAdmIdea(AlarmInfo alarmInfo, String pushAdmDetail, int aidDesSubStep, int aidDesSubStepType, String descByCode) {
        //查询故障推荐指令步骤
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), aidDesSubStep, aidDesSubStepType);
        //过滤影响行车的步骤
        List<AidDesSubStepEntity> entitiesFilters = filterAidDesSubStep(entities, pushAdmDetail, alarmInfo);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entitiesFilters, alarmInfo);
        //替换推荐指令中的站名上下行和计轴编号
        stepList = replaceDisposeDesc(stepList, alarmInfo.getAxleCounterName(), alarmInfo);
        //设置故障类型
        alarmInfo.setAlarmTypeDetail(Integer.parseInt(pushAdmDetail));
        //获取调度命令对象
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        //设置故障恢复推荐指令执行步骤
        admIdea.setExecuteStep(aidDesSubStep);
        //设置前端显示推荐指令内容
        admIdea.setDispose(stepList);
        //获取推荐指令步骤
        List<AidDesSubStepOutDto> aidDesSubStepOutDtos = aidDesSubStepConvert.entitiesToDtoList(entitiesFilters);
        //替换推荐指令和调度命令中的站名上下行和计轴编号
        replaceAidDesSubStepDesc(aidDesSubStepOutDtos, alarmInfo);
        //赋值执行单元信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepOutDtos);
        //设置故障详情
        admIdea.setAlarmTypeDetailStr(descByCode);
        return admIdea;
    }

    private List<AidDesSubStepEntity> filterAidDesSubStep(List<AidDesSubStepEntity> list, String alarmTypeDetail, AlarmInfo alarmInfo) {
        AxleStopAreaResult axleStopAreaResult = (AxleStopAreaResult) BasicCommonCacheUtils.get(Cache.AXLE_FAILURE_ALG, AxleStopAreaResult.class);
        if (Objects.isNull(axleStopAreaResult)) {
            return list;
        }
        //故障恢复时间早于2min，不调图
        if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_2) {
            String endAlarmTime = alarmInfo.getEndAlarmTime();
            String startAlarmTime = alarmInfo.getStartAlarmTime();
            if (StringUtils.isEmpty(startAlarmTime) || StringUtils.isEmpty(endAlarmTime)) {
                throw new BizException("故障恢复推送推荐指令时未设置故障开始时间或者故障结束时间");
            }
            if (!isLate()) {
                //设置为未晚点，不调图
                BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG, FlowchartFlagEnum.NOT_LATE.getCode());
                return list.stream().filter(this::isAffectStep).collect(Collectors.toList());
            } else {
                BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG, FlowchartFlagEnum.LATE.getCode());
            }
        }
        return list;
    }

    /**
     * 检查列车是否晚点超过2分钟
     *
     * @author kangyi
     * @date 2022/8/27 23:35
     */
    private boolean isLate() {
        Map<Object, Object> holdTrainMap = BasicCommonCacheUtils.hmget(Cache.HOLD_AND_OFF_TRAIN, HoldOffTrainTimeDto.class);
        if (org.springframework.util.CollectionUtils.isEmpty(holdTrainMap)) {
            return false;
        }
        for (Map.Entry<Object, Object> entry : holdTrainMap.entrySet()) {
            HoldOffTrainTimeDto holdOffTrainTimeDto = (HoldOffTrainTimeDto) entry.getValue();
            if (Objects.isNull(holdOffTrainTimeDto)) {
                continue;
            }
            String trainId = holdOffTrainTimeDto.getTrainId();
            TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent(trainId);
            if (Objects.isNull(tiasTraceInfo)) {
                continue;
            }
            //车次追踪晚点时间
            Integer otpTime = tiasTraceInfo.getOtpTime();
            //系统配置参数的晚点时间
            int lateTime = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_LATE_TIME));
            //车次追踪晚点时间 》 系统配置参数的晚点时间 则晚点
            if ((otpTime * 1000) > lateTime) {
                return true;
            }
        }
        return false;
    }

    //不影响行车的情况，获取不影响行车步骤
    private boolean isAffectStep(AidDesSubStepEntity step) {
        //缓行，调图
        if (step.getSubStepContent().contains("进路闭塞法") || step.getSubStepContent().contains("动态调整运行图") || step.getSubStepContent().contains("降级通过")) {
            return false;
        }
        //如果已经扣车，取消扣车步骤需要
        if (BasicCommonCacheUtils.exist(Cache.HOLD_AND_OFF_TRAIN) && "deleteHoldTrain".equals(step.getBeanName())) {
            step.setBeanName("doorCancelHoldTrain");
            return true;
        }
        if (!StringUtils.isEmpty(step.getBeanName())) {
            return Arrays.stream(AFFECT_DRIVING_STR).noneMatch(str -> str.equals(step.getBeanName()));
        }
        return true;
    }

    //替换推荐指令中的站名上下行和计轴编号
    private List<DisposeDto> replaceDisposeDesc(List<DisposeDto> stepList, String axleCounterName, AlarmInfo alarmInfo) {
        List<DisposeDto> retList = new ArrayList<>();
        //获取集中站名
        String conStationName = alarmInfo.getAlarmSite();
        AxleStopAreaResult axleStopAreaResult = (AxleStopAreaResult) BasicCommonCacheUtils.get(Cache.AXLE_FAILURE_ALG, AxleStopAreaResult.class);
        stepList.forEach((step) -> {
            String step1 = step.getStep();
            step1 = step1.replaceAll("%s计轴", axleCounterName + "计轴").replaceAll("%s集中站", conStationName);
            if (step1.contains("起始车站-终点车站")) {
                step1 = getSlowlyAidDesc(step1, axleStopAreaResult.getAxleStopAreaList());
            }
            step.setStep(step1);
            retList.add(step);
        });

        return retList;
    }

    private String getSlowlyAidDesc(String content, List<AxleStopArea> axleStopAreaList) {
        StringBuilder sb = new StringBuilder();
        for (AxleStopArea axleStopArea : axleStopAreaList) {
            String slowlyStartStation = admStationService.selectByStationId(axleStopArea.getSlowlyStartStationId()).getStationName();
            String slowlyEndStation = admStationService.selectByStationId(axleStopArea.getSlowlyEndStationId()).getStationName();
            sb.append(slowlyStartStation);
            sb.append("-");
            sb.append(slowlyEndStation);
            sb.append(",");
        }
        sb.deleteCharAt(sb.lastIndexOf(","));
        return content.replaceAll("起始车站-终点车站", sb.toString());
    }

    //替换调度命令中的站名上下行和计轴编号
    private void replaceAidDesSubStepDesc(List<AidDesSubStepOutDto> stepList, AlarmInfo alarmInfo) {
        //获取集中站名
        String conStationName = alarmInfo.getAlarmSite();
        AxleStopAreaResult axleStopAreaResult = (AxleStopAreaResult) BasicCommonCacheUtils.get(Cache.AXLE_FAILURE_ALG, AxleStopAreaResult.class);
        stepList.forEach((step) -> {
            String subStepContent = step.getSubStepContent();
            String disCmdContent = step.getDisCmdContent();
            //替换推荐指令中的占位符
            if (!StringUtils.isEmpty(subStepContent)) {
                String temp = subStepContent.replaceAll("%s计轴", alarmInfo.getAxleCounterName() + "计轴").replaceAll("%s集中站", conStationName);
                if (temp.contains("起始车站-终点车站")) {
                    temp = getSlowlyAidDesc(temp, axleStopAreaResult.getAxleStopAreaList());
                }
                step.setSubStepContent(temp);
            }
            //替换调度命令中的占位符
            if (!StringUtils.isEmpty(disCmdContent)) {
                String temp = disCmdContent.replaceAll("%s计轴", alarmInfo.getAxleCounterName() + "计轴").replaceAll("%s集中站", conStationName);
                if (temp.contains("起始车站-终点车站")) {
                    temp = getSlowlyAidDesc(temp, axleStopAreaResult.getAxleStopAreaList());
                }
                step.setDisCmdContent(temp);
            }
        });
    }

    //推送第二次推荐指令
    private void pushSecondAuxiliaryDecision(AlarmInfo alarmInfo) {
        long detailId = UidGeneratorUtils.getUID();
        //插入生成第二次推荐指令方案日志
        admAlertDetailService.insert(new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令", new Date(), "系统产生第二次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis()));
        long boxId = UidGeneratorUtils.getUID();
        //推荐指令步骤执行第二步
        alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_2);
        String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
        //获取推送对象
        AdmIdea admIdea = getAdmIdea(alarmInfo, String.valueOf(alarmInfo.getAlarmTypeDetail()), IidsConstPool.DISPOSE_STEP_TWO, IidsConstPool.DISPOSE_STEP_TYPE_ZERO, descByCode);
        log.info("获取到推送第二次推荐指令对象AdmIdea:{}", JsonUtils.toJSONString(admIdea));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推荐指令入库
        admAlertDetailBoxService.insert(new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行"));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        alarmInfo.setTableBoxId2(boxId);
        //设置状态未执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
        admAlertInfoSubService.updateById(alarmInfo);
        //推送第二次推荐指令
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("生成第二次推荐指令提示成功!");
    }

    //推送第五次推荐指令
    private void pushFiveAuxiliaryDecision(AlarmInfo alarmInfo) {
        long detailId = UidGeneratorUtils.getUID();
        //插入生成第五次推荐指令方案日志
        admAlertDetailService.insert(new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第五次推荐指令", new Date(), "系统产生第五次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis()));
        long boxId = UidGeneratorUtils.getUID();
        //推荐指令步骤执行第五步
        alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_5);
        String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
        int executeStep = IidsConstPool.DISPOSE_STEP_FIVE;
        AlarmInfo upgradeAlarmInfo = null;
        AdmIdea admIdea = null;
        if (BasicCommonCacheUtils.exist(Cache.ALARM_UPGRADE)) {
            if (alarmInfo.getUpgradeId() != -1) {
                upgradeAlarmInfo = admAlertInfoUpgradeService.getById(alarmInfo.getUpgradeId());
            }
            //升级故障
            if (!Objects.isNull(upgradeAlarmInfo)) {
                executeStep = IidsConstPool.EXECUTE_STEP_2;
                upgradeAlarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_2);
                upgradeAlarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
                upgradeAlarmInfo.setTableInfoId(alarmInfo.getTableInfoId());
                admAlertInfoUpgradeService.update(upgradeAlarmInfo);
                admIdea = getAdmIdea(upgradeAlarmInfo, String.valueOf(upgradeAlarmInfo.getAlarmTypeDetail()), executeStep, IidsConstPool.DISPOSE_STEP_TYPE_ZERO, descByCode);
                admIdea.setAlarmType(Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER));
                admIdea.setAlarmTypeDetail(Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_SWITCH));
                admIdea.setExecuteStep(IidsConstPool.EXECUTE_STEP_5);
            }
        } else {
            admIdea = getAdmIdea(alarmInfo, String.valueOf(alarmInfo.getAlarmTypeDetail()), executeStep, IidsConstPool.DISPOSE_STEP_TYPE_ZERO, descByCode);
        }
        //获取推送对象
        admIdea.setAlarmTypeDetailStr(descByCode);
        log.info("获取到推送第五次推荐指令对象AdmIdea:{}", JsonUtils.toJSONString(admIdea));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推荐指令入库
        admAlertDetailBoxService.insert(new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行"));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        alarmInfo.setTableBoxId2(boxId);
        //设置状态未执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
        admAlertInfoSubService.updateById(alarmInfo);
        //推送第五次推荐指令
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("生成第五次推荐指令提示成功!");
    }


    public void waiveAidDecision(AlarmInfo alarmInfo) {
        if (Objects.isNull(alarmInfo) || alarmInfo.getAlarmType() != Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER)) {
            return;
        }
        log.info("计轴故障，放弃推荐指令保存进缓存");
        BasicCommonCacheUtils.hPut(Cache.WAIVE_REPORT_AXLE_COUNTER, String.valueOf(alarmInfo.getAxleCounterId()), alarmInfo.getAlarmTypeDetail());
        //删除已经上报过的计轴故障缓存
        BasicCommonCacheUtils.delMapKey(Cache.ALREADY_REPORT_AXLE_COUNTER, String.valueOf(alarmInfo.getAxleCounterId()));
    }

    //道岔区段计轴故障---车站终端确认计轴占用道岔状态信息
    public void confirmSwitchStatus(Integer code, AlarmInfo alarmInfo) {
        //车站选择结果存放redis
        BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.STATION_CONFIRM_FLAG, code);
        //车站选择道岔强转至行车方向
        if (Objects.equals(code, StationConfirmEnum.FIRST_CONFIRM.getCode())) {
            long detailId = UidGeneratorUtils.getUID();
            //插入生成第三次推荐指令方案日志
            admAlertDetailService.insert(new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第三次推荐指令", new Date(), "系统产生第三次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis()));
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第三步
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_3);
            String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
            //获取推送对象
            AdmIdea admIdea = getAdmIdea(alarmInfo, String.valueOf(alarmInfo.getAlarmTypeDetail()), IidsConstPool.DISPOSE_STEP_THREE, IidsConstPool.DISPOSE_STEP_TYPE_ZERO, descByCode);
            log.info("获取到推送第三次推荐指令对象AdmIdea:{}", JsonUtils.toJSONString(admIdea));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            admAlertDetailBoxService.insert(new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行"));
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            alarmInfo.setTableBoxId2(boxId);
            //设置状态未执行
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
            admAlertInfoSubService.updateById(alarmInfo);
            //推送第三次推荐指令
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            BasicCommonCacheUtils.delKey(Cache.AXLE_COUNTER_CONFIRM);
            log.info("生成第三次推荐指令提示成功!");
        } else if (Objects.equals(code, StationConfirmEnum.SECOND_CONFIRM.getCode())) {//车站选择道岔强转失败
            long detailId = UidGeneratorUtils.getUID();
            //插入生成第三次推荐指令方案日志
            admAlertDetailService.insert(new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第三次推荐指令", new Date(), "系统产生第三次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis()));
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第三步
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_3);
            String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
            //获取推送对象
            AdmIdea admIdea = getAdmIdea(alarmInfo, String.valueOf(alarmInfo.getAlarmTypeDetail()), IidsConstPool.DISPOSE_STEP_THREE, IidsConstPool.DISPOSE_STEP_TYPE_ONE, descByCode);
            log.info("获取到推送第三次推荐指令对象AdmIdea:{}", JsonUtils.toJSONString(admIdea));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            admAlertDetailBoxService.insert(new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行"));
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            alarmInfo.setTableBoxId2(boxId);
            //设置状态未执行
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
            admAlertInfoSubService.updateById(alarmInfo);
            //推送第三次推荐指令
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            BasicCommonCacheUtils.delKey(Cache.AXLE_COUNTER_CONFIRM);
            log.info("生成第三次推荐指令提示成功!");
        } else if (Objects.equals(code, StationConfirmEnum.THIRD_CONFIRM.getCode())) {//车站选择人工划轴成功
            //获取当前划轴后的非道岔区段计轴故障
            AxisInfoDto notSwitchAxis = getNotSwitchAxis();
            if (Objects.isNull(notSwitchAxis)) {
                log.error("收到车站终端确认人工划轴成功状态！但当前未检测到划轴后的非道岔区段计轴故障！");
                AppPushServiceUtil.sendWebNoticeMessageToAny(new WebNoticeDto(MsgPushEnum.ERROR_MSG.getCode(), "0", "收到车站终端确认人工划轴成功状态！但当前未检测到划轴后的非道岔区段计轴故障！"));
                return;
            }
            //删除计轴预复位次数缓存，重新计算计轴预复位次数
            BasicCommonCacheUtils.delKey(Cache.AXIS_RESET_COUNT);
            AlarmUpgrade alarmUpgrade = new AlarmUpgrade();
            alarmUpgrade.setInfoId(alarmInfo.getTableInfoId());
            alarmUpgrade.setAlarmType(Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER));
            alarmUpgrade.setAlarmTypeDetail(Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH));
            alarmUpgrade.setExecuteStep(IidsConstPool.EXECUTE_STEP_1);
            alarmUpgrade.setAxleCounterId(notSwitchAxis.getAxisId());
            alarmUpgrade.setAxleCounterName(notSwitchAxis.getAxisName());
            BasicCommonCacheUtils.hPut(Cache.ALARM_UPGRADE, AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH, alarmUpgrade);
            //插入非道岔区段计轴故障进升级表
            AlarmInfo admAlertInfoUpgrade = new AlarmInfo();
            admAlertInfoUpgrade.setTableInfoId(alarmInfo.getTableInfoId());
            admAlertInfoUpgrade.setAlarmTypeDetail(Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH));
            admAlertInfoUpgrade.setAxleCounterId(notSwitchAxis.getAxisId());
            admAlertInfoUpgrade.setAxleCounterName(notSwitchAxis.getAxisName());
            admAlertInfoUpgrade.setAlarmConStation(String.valueOf(notSwitchAxis.getConStationId()));
            admAlertInfoUpgrade.setAlarmType(Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER));
            admAlertInfoUpgrade.setStartAlarmTime(DateUtil.getTimeStamp());
            admAlertInfoUpgrade.setExecuteStep(IidsConstPool.EXECUTE_STEP_1);
            //算法获取上下行和停车区域id
            AxleStopAreaResult areaResult = getAlgorithmResult(admAlertInfoUpgrade);
            if (Objects.isNull(areaResult)) {
                throw new BizException("获取算法停车区域和上下行为空");
            }
            if (IidsConstPool.AXLE_ALG_NO_HOLD_TYPE.equals(areaResult.getAxleFailureType())) {
                log.info("故障转移至不影响行车的计轴区段，推送故障恢复推荐指令");
                //故障恢复状态
                alarmInfo.setAlarmState(IidsConstPool.ALARM_STATE_FAILOVER);
                AlarmInfoRouter alarmInfoRouter = (AlarmInfoRouter) SpringContextUtil.getBean("alarmInfoRouter");
                alarmInfoRouter.alarmInfoRouter(alarmInfo);
                return;
            }
            //设置计轴参数
            setAlarmInfo(admAlertInfoUpgrade, areaResult);
            long detailId = UidGeneratorUtils.getUID();
            //插入生成第四次推荐指令方案日志
            admAlertDetailService.insert(new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第四次推荐指令", new Date(), "系统产生第四次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis()));
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第四步
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_4);
            String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
            //获取推送对象
            AdmIdea admIdea = getAdmIdea(admAlertInfoUpgrade, String.valueOf(admAlertInfoUpgrade.getAlarmTypeDetail()), IidsConstPool.EXECUTE_STEP_1, IidsConstPool.DISPOSE_STEP_TYPE_ZERO, descByCode);
            admIdea.setExecuteStep(IidsConstPool.EXECUTE_STEP_4);
            admIdea.setAlarmType(Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER));
            admIdea.setAlarmTypeDetail(Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_SWITCH));
            log.info("获取到推送第四次推荐指令对象AdmIdea:{}", JsonUtils.toJSONString(admIdea));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            admAlertDetailBoxService.insert(new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行"));
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            alarmInfo.setTableBoxId2(boxId);
            long upgradeId = admAlertInfoUpgradeService.insert(admAlertInfoUpgrade);
            //设置状态未执行
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
            alarmInfo.setUpgradeId(upgradeId);
            admAlertInfoSubService.updateById(alarmInfo);
            //推送第四次推荐指令
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            BasicCommonCacheUtils.delKey(Cache.AXLE_COUNTER_CONFIRM);
            log.info("生成第四次推荐指令提示成功!");
        } else if (Objects.equals(code, StationConfirmEnum.FOURTH_CONFIRM.getCode())) {//车站选择人工划轴失败
            long detailId = UidGeneratorUtils.getUID();
            //插入生成第四次推荐指令方案日志
            admAlertDetailService.insert(new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第四次推荐指令", new Date(), "系统产生第四次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis()));
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第四步
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_4);
            String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
            //获取推送对象
            AdmIdea admIdea = getAdmIdea(alarmInfo, String.valueOf(alarmInfo.getAlarmTypeDetail()), IidsConstPool.DISPOSE_STEP_FOUR, IidsConstPool.DISPOSE_STEP_TYPE_ZERO, descByCode);
            log.info("获取到推送第四次推荐指令对象AdmIdea:{}", JsonUtils.toJSONString(admIdea));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            admAlertDetailBoxService.insert(new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行"));
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            alarmInfo.setTableBoxId2(boxId);
            //设置状态未执行
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
            admAlertInfoSubService.updateById(alarmInfo);
            //推送第四次推荐指令
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            BasicCommonCacheUtils.delKey(Cache.AXLE_COUNTER_CONFIRM);
            log.info("生成第四次推荐指令提示成功!");
        }
        axleCounterUpdateFlowchartsService.switchStationConfirm(alarmInfo, code);
    }

    //获取划轴后产生的非道岔区段计轴
    private AxisInfoDto getNotSwitchAxis() {
        //获取ATS发送的区段状态数据
        Map<Integer, AxisInfoDto> axleCiDeviceInfo = getAxleCiDeviceInfo();
        for (Map.Entry<Integer, AxisInfoDto> entry : axleCiDeviceInfo.entrySet()) {
            //区段状态数据
            AxisInfoDto axisInfo = entry.getValue();
            //当前区段是否道岔区段
            boolean isSwitch = logicSectionDataService.getIfSwitchByLogicId(axisInfo.getLogSecId());
            if (isSwitch) {
                continue;
            }
            //停车场和已经上报过或者放弃过的计轴不处理
            if (extractedCommon(axisInfo)) {
                continue;
            }
            //通信车占用或者非通信车占用或者进路锁闭
            if (IN_USE.equals(axisInfo.getNCbtcUse()) && !LOCK.equals(axisInfo.getJLock())) {
                //车次追踪数据对应逻辑区段无车次窗
                List<Object> list = trainTraceCache.asMap().values().parallelStream().filter(value -> Objects.equals(value.getLogicalSectionId(), axisInfo.getLogSecId())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(list)) {
                    return axisInfo;
                }
            }
        }
        return null;
    }


    //获取ATS发送的区段状态数据
    public Map<Integer, AxisInfoDto> getAxleCiDeviceInfo() {
        Map<Integer, AxisInfoDto> axleMap = new HashMap<>();
        //获取电子地图中的计轴信息
        List<AxisInfoDto> axisInfoList = axisInfoService.getAxisInfoList();
        if (CollectionUtils.isEmpty(axisInfoList)) {
            log.debug("电子地图获取不到计轴信息");
            return axleMap;
        }
        for (AxisInfoDto axisInfoDto : axisInfoList) {
            //获取ATS发送的计轴区段信息
            CiDeviceInfo ciDeviceInfo = ciDeviceStatusCache.getIfPresent(String.valueOf(axisInfoDto.getLogSecId()));
            if (Objects.isNull(ciDeviceInfo)) {
                continue;
            }
            //封装ATS发送的区段状态数据
            axisInfoDto.setArb(ciDeviceInfo.getArb());
            axisInfoDto.setCbtcUse(ciDeviceInfo.getCbtcUse());
            axisInfoDto.setNCbtcUse(ciDeviceInfo.getNCbtcUse());
            axisInfoDto.setJLock(ciDeviceInfo.getJLock());
            axisInfoDto.setPreReset(ciDeviceInfo.getPreReset());
            //删除放弃推荐指令的缓存计轴
            deleteWaiveAxleCounter(axisInfoDto);
            //同一个计轴包含不同的逻辑区段，只需要取一个计轴判断，所以去重
            axleMap.putIfAbsent(axisInfoDto.getAxisId(), axisInfoDto);
        }
        return axleMap;
    }

    public boolean extractedAutoMonitor(AxisInfoDto axleCiInfo) {
        if (extractedCommon(axleCiInfo)) {
            return true;
        }
        String delaySys = SysParamKit.getByCode(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
        long delayTime = Long.parseLong(AXLE_COUNTER_DELAY);
        if (!StringUtils.isEmpty(delaySys)) {
            delayTime = Long.parseLong(delaySys);
        }
        //计轴延时，防止，计轴故障场景变化
        if (!BasicCommonCacheUtils.existHash(Cache.DELAY_AXLE_COUNTER, String.valueOf(axleCiInfo.getAxisId()))) {
            BasicCommonCacheUtils.hPut(Cache.DELAY_AXLE_COUNTER, String.valueOf(axleCiInfo.getAxisId()), String.valueOf(System.currentTimeMillis()));
            return true;
        }
        //首次上报的时间
        String reportTimeStr = (String) BasicCommonCacheUtils.hGet(Cache.DELAY_AXLE_COUNTER, String.valueOf(axleCiInfo.getAxisId()));
        //时间差
        long timeDiff = System.currentTimeMillis() - Long.parseLong(reportTimeStr);
        return timeDiff < delayTime;
    }

    private boolean extractedCommon(AxisInfoDto axleCiInfo) {
        //报告过计轴故障的计轴不再报告
        if (BasicCommonCacheUtils.existHash(Cache.ALREADY_REPORT_AXLE_COUNTER, String.valueOf(axleCiInfo.getAxisId()))) {
            log.debug("计轴区段编号：{}已经上报过计轴故障！", axleCiInfo.getAxisId());
            return true;
        }
        //已经放弃过的计轴故障不再报告
        if (BasicCommonCacheUtils.existHash(Cache.WAIVE_REPORT_AXLE_COUNTER, String.valueOf(axleCiInfo.getAxisId()))) {
            log.debug("计轴区段编号：{}已经放弃过上报的计轴故障！", axleCiInfo.getAxisId());
            return true;
        }
        //车辆段和停车场内计轴区段不上报
        Boolean onDepotOrPark = axisInfoService.judgeAxisIfOnDepotOrPark(axleCiInfo.getAxisId());
        if (onDepotOrPark) {
            log.debug("计轴区段编号：{},在车辆段或者停车场内，不支持计轴故障处理！", axleCiInfo.getAxisName());
            return true;
        }
        return false;
    }

    //删除放弃推荐指令的缓存计轴
    private void deleteWaiveAxleCounter(AxisInfoDto axisInfoDto) {
        //已经上报过放弃的计轴缓存
        if (BasicCommonCacheUtils.existHash(Cache.WAIVE_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()))) {
            int alarmTypeDetail = (int) BasicCommonCacheUtils.hGet(Cache.WAIVE_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()));
            //ARB计轴故障，非ARB，删除缓存的已经放弃过的ARB计轴故障
            if ((alarmTypeDetail == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_ARB) ||
                    alarmTypeDetail == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_ARB_RESET)) &&
                    NOT_ARB.equals(axisInfoDto.getArb())) {
                BasicCommonCacheUtils.delMapKey(Cache.WAIVE_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()));
                BasicCommonCacheUtils.delMapKey(Cache.ALREADY_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()));
                //道岔区段计轴，非道岔区段计轴故障，非通信车未占用，删除缓存的已经放弃过的道岔区段/非道岔区段计轴故障
            } else if ((alarmTypeDetail == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH)
                    || alarmTypeDetail == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_SWITCH)
                    || alarmTypeDetail == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH_RESET)
                    || alarmTypeDetail == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_SWITCH_RESET)) &&
                    !IN_USE.equals(axisInfoDto.getNCbtcUse())) {
                BasicCommonCacheUtils.delMapKey(Cache.WAIVE_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()));
                BasicCommonCacheUtils.delMapKey(Cache.ALREADY_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()));
            }
        }
        //已经上报过放弃的计轴缓存
        if (BasicCommonCacheUtils.existHash(Cache.ALREADY_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()))) {
            int alarmTypeDetail = (int) BasicCommonCacheUtils.hGet(Cache.ALREADY_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()));
            //ARB计轴故障，非ARB，删除缓存的已经放弃过的ARB计轴故障
            if ((Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_ARB) == alarmTypeDetail ||
                    alarmTypeDetail == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_ARB_RESET))
                    && NOT_ARB.equals(axisInfoDto.getArb())) {
                BasicCommonCacheUtils.delMapKey(Cache.WAIVE_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()));
                BasicCommonCacheUtils.delMapKey(Cache.ALREADY_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()));
                //道岔区段计轴，非道岔区段计轴故障，非通信车未占用，删除缓存的已经放弃过的道岔区段/非道岔区段计轴故障
            } else if ((Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH) == alarmTypeDetail
                    || Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_SWITCH) == alarmTypeDetail
                    || Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH_RESET) == alarmTypeDetail
                    || Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_SWITCH_RESET) == alarmTypeDetail)
                    && !IN_USE.equals(axisInfoDto.getNCbtcUse())) {
                BasicCommonCacheUtils.delMapKey(Cache.WAIVE_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()));
                BasicCommonCacheUtils.delMapKey(Cache.ALREADY_REPORT_AXLE_COUNTER, String.valueOf(axisInfoDto.getAxisId()));
            }
        }
    }

    //设置算法返回内容
    public void setAlarmInfo(AlarmInfo alarmInfo, AxleStopAreaResult areaResult) {
        log.info("获取到算法停车区域和上下行{}", JsonUtils.toJSONString(areaResult));
        //设置算法返回的计轴故障类型，1普通故障类型，2不影响行车计轴故障，2场景不需要扣车调图
        BasicCommonCacheUtils.set(Cache.AXLE_FAILURE_ALG, areaResult);
        AxisInfoDto axisInfoDto = axisInfoService.getAxisInfoByAxisId(alarmInfo.getAxleCounterId());
        if (Objects.isNull(axisInfoDto)) {
            throw new BizException("获取电子地图计轴信息失败！");
        }
        log.info("获取到电子地图集中站id【{}】", axisInfoDto.getConStationId());
        alarmInfo.setAlarmConStation(String.valueOf(axisInfoDto.getConStationId()));
        //获取集中站名
        ConStationDto conStationDto = conStationInfoService.getStationByConStationId(axisInfoDto.getConStationId());
        if (Objects.isNull(conStationDto)) {
            throw new BizException("获取电子地图集中区信息失败！");
        }
        String stationName = conStationDto.getStationName();
        alarmInfo.setAlarmSite(stationName);
        if (org.springframework.util.CollectionUtils.isEmpty(areaResult.getAxleStopAreaList())) {
            throw new BizException("获取算法返回扣车的停车区域为空");
        }
        Integer upDown = areaResult.getAxleStopAreaList().get(0).getUpDown();
        StringBuilder platformIds = new StringBuilder();
        for (AxleStopArea stopArea : areaResult.getAxleStopAreaList()) {
            Integer stopAreaId = stopArea.getStopAreaId();
            Integer platformId = platformInfoService.getPlatformIdByStopArea(stopAreaId);
            platformIds.append(platformId);
            platformIds.append("/");
        }
        platformIds.deleteCharAt(platformIds.lastIndexOf("/"));
        alarmInfo.setPlatformId(platformIds.toString());
        alarmInfo.setUpDown(upDown);
        alarmInfo.setStationId(conStationDto.getStationId());
    }

    /**
     * @param code
     * @param alarmInfo
     * @description arb复位，车站选择计轴复位状态
     * @date 2022/11/30 9:57
     * @author kangyi
     * @return: void
     */
    public void confirmResetArbStatus(Integer code, AlarmInfo alarmInfo) {
        //车站选择结果存放redis
        BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.STATION_CONFIRM_FLAG, code);
        BasicCommonCacheUtils.delKey(Cache.AXLE_COUNTER_CONFIRM);
        if (Objects.equals(code, StationConfirmEnum.FIRST_CONFIRM.getCode())) {
            //故障恢复状态
            alarmInfo.setAlarmState(IidsConstPool.ALARM_STATE_FAILOVER);
            alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
            Assert.notNull(alarmInfo, "该故障生命周期已结束");
            //设置故障结束时间
            alarmInfo.setEndAlarmTime(DateUtil.getDate("yyyy-MM-dd HH:mm:ss.SSS"));
            log.info("接收到计轴故障恢复，开始推送故障恢复推荐指令！alarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
            int executeStepType = IidsConstPool.DISPOSE_STEP_TYPE_ZERO;
            int executeStep = IidsConstPool.EXECUTE_STEP_0_2;
            //故障恢复，第一次推荐指令后和第二次推荐指令后故障恢复更新的流程图步骤不一致,先存缓存
            BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.PRE_SUCCESS_FLAG, String.valueOf(alarmInfo.getExecuteStep()));
            String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
            alarmInfo.setExecuteStep(executeStep);
            //更新故障恢复步骤为已推送推荐指令
            alarmInfo.setFailureRecoveryStep(IidsConstPool.RECOVERY_PUSH_STEP1);
            admAlertInfoSubService.updateById(alarmInfo);
            //获取推送到前端的
            AdmIdea admIdea = getAdmIdea(alarmInfo, AlarmTypeConstant.AXLE_COUNTER_ARB_RESET, executeStep, executeStepType, descByCode);
            //推送弹窗显示时长
            admIdea.setShowSecond(180);
            //设置标题
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            log.info("开始推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
            //推送推荐指令
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        } else {
            //车站选择计轴复位失败推送第二次推荐指令
            pushAuxiliaryDecision(alarmInfo, IidsConstPool.EXECUTE_STEP_2, IidsConstPool.DISPOSE_STEP_TYPE_ZERO);
        }
        //更新流程图
        axleCounterResetUpdateFlowchartsService.arbStationConfirm(alarmInfo, code);
    }

    /**
     * @param code
     * @param alarmInfo
     * @description 非道岔区段计轴故障复位，车站选择计轴复位状态
     * @date 2022/11/30 9:57
     * @author kangyi
     * @return: void
     */
    public void confirmResetNotSwitchStatus(Integer code, AlarmInfo alarmInfo) {
        //车站选择结果存放redis
        BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.STATION_CONFIRM_FLAG, code);
        BasicCommonCacheUtils.delKey(Cache.AXLE_COUNTER_CONFIRM);
        if (Objects.equals(code, StationConfirmEnum.FIRST_CONFIRM.getCode())) {
            //故障恢复状态
            alarmInfo.setAlarmState(IidsConstPool.ALARM_STATE_FAILOVER);
            alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
            Assert.notNull(alarmInfo, "该故障生命周期已结束");
            //设置故障结束时间
            alarmInfo.setEndAlarmTime(DateUtil.getDate("yyyy-MM-dd HH:mm:ss.SSS"));
            log.info("接收到计轴故障恢复，开始推送故障恢复推荐指令！alarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
            int executeStepType = IidsConstPool.DISPOSE_STEP_TYPE_ZERO;
            int executeStep = IidsConstPool.EXECUTE_STEP_0_2;
            String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
            alarmInfo.setExecuteStep(executeStep);
            //更新故障恢复步骤为已推送推荐指令
            alarmInfo.setFailureRecoveryStep(IidsConstPool.RECOVERY_PUSH_STEP1);
            admAlertInfoSubService.updateById(alarmInfo);
            //获取推送到前端的
            AdmIdea admIdea = getAdmIdea(alarmInfo, AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH_RESET, executeStep, executeStepType, descByCode);
            //设置故障详情
            admIdea.setAlarmTypeDetailStr(descByCode);
            //设置标题
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推送弹窗显示时长
            admIdea.setShowSecond(180);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            log.info("开始推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
            //推送推荐指令
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        } else {
            //车站选择计轴复位失败推送第二次推荐指令
            pushAuxiliaryDecision(alarmInfo, IidsConstPool.EXECUTE_STEP_2, IidsConstPool.DISPOSE_STEP_TYPE_ZERO);
        }
        //更新流程图
        axleCounterResetUpdateFlowchartsService.notSwitchStationConfirm(alarmInfo, code);
    }

    /**
     * @param code
     * @param alarmInfo
     * @description 道岔区段计轴故障复位，车站选择计轴复位状态
     * @date 2022/11/30 9:57
     * @author kangyi
     * @return: void
     */
    public void confirmResetSwitchStatus(Integer code, AlarmInfo alarmInfo) {
        //车站选择结果存放redis
        BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.STATION_CONFIRM_FLAG, code);
        BasicCommonCacheUtils.delKey(Cache.AXLE_COUNTER_CONFIRM);
        if (Objects.equals(code, StationConfirmEnum.FIRST_CONFIRM.getCode())) {
            //故障恢复状态
            alarmInfo.setAlarmState(IidsConstPool.ALARM_STATE_FAILOVER);
            alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
            Assert.notNull(alarmInfo, "该故障生命周期已结束");
            //设置故障结束时间
            alarmInfo.setEndAlarmTime(DateUtil.getDate("yyyy-MM-dd HH:mm:ss.SSS"));
            log.info("接收到计轴故障恢复，开始推送故障恢复推荐指令！alarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
            int executeStepType = IidsConstPool.DISPOSE_STEP_TYPE_ZERO;
            int executeStep = IidsConstPool.EXECUTE_STEP_0_2;
            String descByCode = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
            alarmInfo.setExecuteStep(executeStep);
            //更新故障恢复步骤为已推送推荐指令
            alarmInfo.setFailureRecoveryStep(IidsConstPool.RECOVERY_PUSH_STEP1);
            admAlertInfoSubService.updateById(alarmInfo);
            //获取推送到前端的
            AdmIdea admIdea = getAdmIdea(alarmInfo, AlarmTypeConstant.AXLE_COUNTER_SWITCH_RESET, executeStep, executeStepType, descByCode);
            //设置故障详情
            admIdea.setAlarmTypeDetailStr(descByCode);
            //设置标题
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推送弹窗显示时长
            admIdea.setShowSecond(180);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            log.info("开始推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
            //推送推荐指令
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        } else {
            //车站选择计轴复位失败推送第二次推荐指令
            pushAuxiliaryDecision(alarmInfo, IidsConstPool.EXECUTE_STEP_2, IidsConstPool.DISPOSE_STEP_TYPE_ZERO);
        }
        //更新流程图
        axleCounterResetUpdateFlowchartsService.notSwitchStationConfirm(alarmInfo, code);
    }

}
