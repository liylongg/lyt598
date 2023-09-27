package com.tct.itd.adm.msgRouter.service;

import com.google.common.collect.Lists;
import com.tct.itd.adm.api.AlgPowerClient;
import com.tct.itd.adm.api.AlgorithmClient;
import com.tct.itd.adm.convert.TiasDataConvert;
import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.RecoveryCodeEnum;
import com.tct.itd.adm.msgRouter.executeHandle.WaiveAidDecisionAdmHandler;
import com.tct.itd.adm.runGraph.stategy.IPrePlanRunGraphStrategy;
import com.tct.itd.adm.runGraph.stategy.TractionPowerStrategy;
import com.tct.itd.adm.service.*;
import com.tct.itd.adm.util.*;
import com.tct.itd.basedata.dfsread.service.base.MapLinkBaseService;
import com.tct.itd.basedata.dfsread.service.handle.AxisInfoService;
import com.tct.itd.basedata.dfsread.service.handle.LogicSectionDataService;
import com.tct.itd.basedata.dfsread.service.handle.PlatformInfoService;
import com.tct.itd.basedata.dfsread.service.handle.StopRegionDataService;
import com.tct.itd.client.AlgSwitchClient;
import com.tct.itd.client.SpareCarClient;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.dto.*;
import com.tct.itd.common.enums.AlarmSourceEnum;
import com.tct.itd.common.enums.CommandTypeEnum;
import com.tct.itd.common.enums.SwitchFailureTypeEnum;
import com.tct.itd.common.enums.TrainStateEnum;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.*;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.enums.PlanRunGraphEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.hub.service.PlanRunGraphService;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.tias.service.AtsByteCommandFlatService;
import com.tct.itd.tias.service.SendNotifyService;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description:
 * @Author: zhangyinglong
 * @Date:2021/5/18 16:19
 */
@Slf4j
@Service
public class AidDecisionExecService {

    @Resource
    private PlatformInfoService platformInfoService;
    //算法接口
    @Resource
    private AlgorithmClient algorithmClient;

    @Resource
    private AlgPowerClient algPowerClient;

    @Resource
    TiasDataConvert tiasDataConvert;

    @Resource
    private AtsByteCommandFlatService atsByteCommandFlatClient;

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Resource
    private PlanRunGraphService planRunGraphService;

    @Resource
    private AlarmUtil alarmUtil;

    @Resource
    private SpareCarClient depotDataClient;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private List<IPrePlanRunGraphStrategy> graphDataStrategyList;

    private final Map<String, IPrePlanRunGraphStrategy> graphDataStrategyHashMap = new HashMap<>();

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AlgSwitchClient algSwitchClient;
    @Resource
    private TractionPowerStrategy tractionPowerStrategy;

    @Resource
    private AdmStationService admStationService;

    @Resource
    private LogicSectionDataService logicSectionDataService;

    @Resource
    private StopRegionDataService stopRegionDataService;

    @Resource
    private AxisInfoService axisInfoService;

    @Resource
    private AdmAlertInfoUpgradeService admAlertInfoUpgradeService;

    @Resource
    private SendNotifyService sendNotifyService;

    @Resource
    private MapLinkBaseService mapLinkService;

    @Resource
    private AppPushService appPushService;

    @Resource
    private WaiveAidDecisionAdmHandler waiveAidDecisionAdmHandler;

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    @Resource
    private DisCmdSendUtils disCmdSendUtils;

    @Resource
    private AdmAdjustStrategyInterfaceService adjustStrategyInterfaceService;

    private final static String SECOND_DAY_TIME = "00:00:00";
    private final static String ALG_SECOND_DAY_TAG = "1.";
    private final static String ALG_FAULT_MESSAGE = "中断后智能调度无法计算出小交路导致调图失败，请运营人员人工处理故障";

    /***
     * @Description 建立映射关系
     * @Author yuelei
     * @date 2021/9/30 15:26
     */
    @PostConstruct
    public void init() {
        graphDataStrategyList.forEach(graphDataStrategy -> graphDataStrategyHashMap.put(graphDataStrategy.strategy(), graphDataStrategy));
    }

    private static final Logger logger = LoggerFactory.getLogger(AidDecisionExecService.class);

    public long doExecAidDecision(AlarmInfo alarmInfo) {
        //推荐指令步骤
        alarmInfo.setExecuteStep(1);
        Long id = alarmInfo.getTableInfoId();
        //赋值全局主键
        Cache.alarmInfoId = id;

        //根据故障子类型获取描述
        String alarmDescribe = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
        log.info("插入故障信息");
        //是否允许故障恢复
        int allowFailover = getAllowFailover(alarmInfo.getAlarmTypeDetail());
        //获取消息字符串
        String msg = getAlarmInfoOprtMsg(alarmInfo);
        //报警源
        //未设值默认人工
        String source = alarmInfo.getSource() == null ? AlarmSourceEnum.MAN_MADE.getName() : AlarmSourceEnum.getNameByType(alarmInfo.getSource());
        //添加故障信息,推荐指令已生成
        alarmInfoOprtService.insertAdmAlertInfo(id, alarmDescribe, String.valueOf(alarmInfo.getAlarmTypeDetail()), alarmInfo.getAlarmSite(), msg, "已生成", alarmInfo.getStartAlarmTime(), allowFailover, source);
        //插入第一条告警信息 上报故障
        String message = getMessage(alarmInfo, alarmDescribe);
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), id, message, "0", "上报" + alarmDescribe, alarmInfo.getStartAlarmTime());
        // 插入第三条告警信息 推荐指令流程
        long detailId = UidGeneratorUtils.getUID();
        alarmInfoOprtService.insertAdmAlertDetail(detailId, id, "系统产生第一次推荐指令", "1", "第一次推荐指令");
        return detailId;
    }

    private String getAlarmInfoOprtMsg(AlarmInfo alarmInfo) {
        StringBuilder sb = new StringBuilder();
        int alarmType = alarmInfo.getAlarmType();
        if(alarmType == Integer.parseInt(AlarmTypeConstant.SWITCH_FAILURE)){
            sb.append(alarmInfo.getSwitchName());
            sb.append("号道岔发生故障");
        }else if(alarmType == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER)){
            sb.append(alarmInfo.getAxleCounterName());
            sb.append("计轴区段发生故障");
        }else if(alarmType == Integer.parseInt(AlarmTypeConstant.TRACTION_POWER_PARENT)){
            sb.append(alarmInfo.getAlarmSite());
            sb.append("区段发生接触网失电故障");
        } else if (alarmType == Integer.parseInt(AlarmTypeConstant.INTERLOCKING_DOUBLE_PARENT)) {
            sb.append(alarmInfo.getAlarmSite());
            sb.append("发生联锁故障");
        } else if (alarmType == Integer.parseInt(AlarmTypeConstant.SIGNAL_ELECTRIC_PARENT)) {
            sb.append(alarmInfo.getAlarmSite());
            sb.append("发生信号电源故障");
        } else {
            AlarmUtil alarmUtil = SpringContextUtil.getBean(AlarmUtil.class);
            //获取表号
            String serverNum = alarmUtil.getServerNumByTrainId(alarmInfo.getTrainId());
            sb.append("车组号:");
            sb.append(alarmInfo.getTrainId());
            sb.append(" 车次号:");
            sb.append(serverNum + alarmInfo.getOrderNum());
        }
        return sb.toString();
    }

    /**
     * 空调故障、广播故障、大客流故障不需要故障恢复
     *
     * @param alarmTypeDetail 故障小类类型
     * @return 是否故障恢复
     */
    public static int getAllowFailover(int alarmTypeDetail) {
        if (AlarmTypeConstant.AIR_CONDITIONING_FAILURE.equals(String.valueOf(alarmTypeDetail))//空调故障
                || AlarmTypeConstant.AIR_CONDITIONING_VENTILATE_FAILURE.equals(String.valueOf(alarmTypeDetail))//空调故障-无通风
                || AlarmTypeConstant.BROADCAST_FAILURE_CAN_MANUAL.equals(String.valueOf(alarmTypeDetail))//广播故障-人工可用
                || AlarmTypeConstant.BROADCAST_FAILURE_CANNOT_MANUAL.equals(String.valueOf(alarmTypeDetail))//广播故障-人工不可用
                || AlarmTypeConstant.LARGE_PASSENGER_FLOW.equals(String.valueOf(alarmTypeDetail))//大客流
                || AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_INTO_STATION.equals(String.valueOf(alarmTypeDetail))//站台门故障-列车进站过程中站台门打开
                || AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_OUT_STATION.equals(String.valueOf(alarmTypeDetail))//站台门故障-列车进站过程中站台门打开
                || AlarmTypeConstant.ALL_PLATFORM_DOOR_CANNOT_OPEN.equals(String.valueOf(alarmTypeDetail))//站台门故障-整侧站台门无法打开
                || AlarmTypeConstant.SINGLE_PLATFORM_DOOR_CANNOT_OPEN.equals(String.valueOf(alarmTypeDetail))//站台门故障-单个站台门无法打开
                || AlarmTypeConstant.SINGLE_PLATFORM_DOOR_CANNOT_CLOSE.equals(String.valueOf(alarmTypeDetail))//站台门故障-单个站台门无法关闭
                || AlarmTypeConstant.ALL_PLATFORM_DOOR_CANNOT_CLOSE.equals(String.valueOf(alarmTypeDetail))//站台门故障-整侧站台门无法关闭
                || AlarmTypeConstant.AXLE_COUNTER_ARB.equals(String.valueOf(alarmTypeDetail))//计轴故障-ARB计轴故障
                || AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH.equals(String.valueOf(alarmTypeDetail))//计轴故障-非道岔区段计轴
                || AlarmTypeConstant.AXLE_COUNTER_SWITCH.equals(String.valueOf(alarmTypeDetail))//计轴故障-道岔区段计轴故障
                || AlarmTypeConstant.AXLE_COUNTER_ARB_RESET.equals(String.valueOf(alarmTypeDetail))//计轴故障-ARB计轴故障
                || AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH_RESET.equals(String.valueOf(alarmTypeDetail))//计轴故障-非道岔区段计轴
                || AlarmTypeConstant.AXLE_COUNTER_SWITCH_RESET.equals(String.valueOf(alarmTypeDetail))//计轴故障-道岔区段计轴故障
                || AlarmTypeConstant.ONLY_DOOR_CANNOT_OPEN.equals(String.valueOf(alarmTypeDetail))//单车门无法打开
                || AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE.equals(String.valueOf(alarmTypeDetail))//单车门无法关闭
                || AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE_SLOW_DOWN.equals(String.valueOf(alarmTypeDetail))//单车门无法关闭-缓行
                || AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE.equals(String.valueOf(alarmTypeDetail))//全列车门无法关闭
                || AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE_SLOW_DOWN.equals(String.valueOf(alarmTypeDetail))//全列车门无法关闭-缓行
                || AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_OPEN.equals(String.valueOf(alarmTypeDetail))//全列车门无法打开
                || AlarmTypeConstant.MORE_MCM_FAILURE.equals(String.valueOf(alarmTypeDetail))//牵引区间故障
                || AlarmTypeConstant.INTERLOCKING_DOUBLE.equals(String.valueOf(alarmTypeDetail))//联锁故障
                // 站后折返道岔故障不具备站前折返
                || AlarmTypeConstant.BEHIND_FAILURE_NOT_HAS_FRONT_TURN.equals(String.valueOf(alarmTypeDetail))
                // 站前折返道岔故障不具备站后折返
                || AlarmTypeConstant.FRONT_FAILURE_NOT_HAS_BEHIND_TURN.equals(String.valueOf(alarmTypeDetail))
                // 线路中间站道岔故障(非小交路折返站-影响单行)
                || AlarmTypeConstant.MIDDLE_FAILURE_SINGLE.equals(String.valueOf(alarmTypeDetail))
                // 线路中间站道岔故障(非小交路折返站-影响上下行)
                || AlarmTypeConstant.MIDDLE_FAILURE_UP_DOWN.equals(String.valueOf(alarmTypeDetail))
                // 虚拟编组
                || AlarmTypeConstant.VIRTUAL_GROUP.equals(String.valueOf(alarmTypeDetail))
        ) {
            return 0;
        }
        //初始故障恢复按钮为禁用
        return 2;
    }

    public static String getMessage(AlarmInfo alarmInfo, String alarmDescribe) {
        StringBuilder sb = new StringBuilder();
        String alarmType = String.valueOf(alarmInfo.getAlarmType());
        if(alarmType.equals(AlarmTypeConstant.SWITCH_FAILURE)){
            sb.append(alarmInfo.getSwitchName());
            sb.append("号道岔发生故障");
        } else if (alarmType.equals(AlarmTypeConstant.AXLE_COUNTER)) {
            sb.append(alarmInfo.getAxleCounterName());
            sb.append("计轴区段发生故障");
        } else if (alarmType.equals(AlarmTypeConstant.TRACTION_POWER_PARENT)) {
            sb.append(alarmInfo.getAlarmSite());
            sb.append("区段发生接触网失电故障");
        } else if (alarmType.equals(AlarmTypeConstant.INTERLOCKING_DOUBLE_PARENT)) {
            sb.append(alarmInfo.getAlarmSite());
            sb.append("发生联锁故障");
        } else if (alarmType.equals(AlarmTypeConstant.SIGNAL_ELECTRIC_PARENT)) {
            sb.append(alarmInfo.getAlarmSite());
            sb.append("发生信号电源故障");
        }else {
            AlarmUtil alarmUtil = SpringContextUtil.getBean(AlarmUtil.class);
            //获取表号
            String serverNum = alarmUtil.getServerNumByTrainId(alarmInfo.getTrainId());
            String msg = String.format("%s次（%s车组号）在%s发生%s", serverNum + alarmInfo.getOrderNum(), alarmInfo.getTrainId(), alarmInfo.getAlarmSite(), alarmDescribe);
            sb.append(msg);
            if (StringUtils.isNotBlank(alarmInfo.getAlarmData())) {
                sb.append(",").append(alarmInfo.getAlarmData());
            }
        }
        return sb.toString();
    }

    public boolean changeGraph(AlarmInfo alarmInfo) {
        //处理特殊情况：在折返轨换端前录入故障，在换端后调图
        if (isAirConditionBroadcastMCM(alarmInfo) && Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
            String trainId = alarmInfo.getTrainId();
            TiasTraceInfo traceInfo = trainTraceCache.getIfPresent(trainId);
            //设置OrderNumber
            if (Objects.isNull(traceInfo)) {
                log.error("根据车组号【{}】获取车次追踪数据失败！", trainId);
                throw new BizException(CodeEnum.NO_GET_TRAIN_TRACE);
            }
            alarmInfo.setOrderNum(traceInfo.getOrderNumber());
        }


        AlgorithmData algorithmData = getAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "调整运行图时,未获取到获取算法参数");
        //算法自检
        BaseResponse<String> baseResponse = algorithmClient.check(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        log.info("获取算法参数成功,准备调整运行图");
        log.info("获取算法参数成功,获取所有备车信息：{}", JsonUtils.toJSONString(algorithmData.getDepotInfoDtoList()));
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> result = algorithmClient.apiSolver(algorithmData);
        if (!result.getSuccess()) {
            this.waiveAidDecision(alarmInfo);
            log.error("准备获取新的运行图，调用算法异常：{}", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = result.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        //获取调整车次列表 dto 转 model
        List<TrainNumberAdjust> trainNumberAdjusts = tiasDataConvert.dtosToEntitys(algorithmResult.getTrainNumberAdjustDtoList());
        //增加告警表主键关联 日志使用
        //新建车次调整对象
        List<TrainNumberAdjust> trainNumberAdjustNew = new ArrayList<>();
        //将加开车次存入缓存 等待执行
        for (TrainNumberAdjust trainNumberAdjust : trainNumberAdjusts) {
            log.info("调整车次列表:{}", JsonUtils.toJSONString(trainNumberAdjust));
            log.info("发送电子调度命令通知:{}", trainNumberAdjust.getAdjustType());
            trainNumberAdjust.setId(UidGeneratorUtils.getUID());
            trainNumberAdjust.setTAdmAlertInfoId(alarmInfo.getTableInfoId());
            trainNumberAdjustNew.add(trainNumberAdjust);
        }
        BasicCommonCacheUtils.hPut(Cache.TRAIN_NUMBER_ADJUST, String.valueOf(alarmInfo.getTableInfoId()), trainNumberAdjustNew, 10 * 60 * 60L, TimeUnit.SECONDS);
        log.info("获取所有的车次调整信息：{}", JsonUtils.toJSONString(trainNumberAdjusts));
        //gzip压缩运行图
        String gzipGraph = algorithmResult.getCompressTrainGraphString();
        //入口
        planRunGraphService.updatePlanRunGraph(gzipGraph);
        log.info("调整计划运行图已更新ATS数据库");
        //推送前端通知
        NotifyParam notifyParam = new NotifyParam();
        notifyParam.setMsgPushEnum(MsgPushEnum.SEND_CHANGE_TRAIN_GRAPH_MSG);
        notifyParam.setInfoId(alarmInfo.getTableInfoId());
        sendNotifyService.sendNotify(notifyParam);
        //向tias系统发送 更新运行图指令
        AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
        atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.CHANGE_TRAIN_GRAPH);
        atsByteCmdData.setParameter(TrainStateEnum.CHANGE_GRAPH.getValue());
        TrainCtrlPrv trainCtrlPrv = new TrainCtrlPrv();
        trainCtrlPrv.setAlarmInfoId(String.valueOf(alarmInfo.getTableInfoId()));
        atsByteCmdData.setTrainCtrl(trainCtrlPrv);
        atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
        log.info("下发更新运行图指令至ats");
        return true;
    }

    private boolean isAirConditionBroadcastMCM(AlarmInfo alarmInfo) {
        String alarmType = String.valueOf(alarmInfo.getAlarmType());
        return Objects.equals(alarmType, AlarmTypeConstant.CONDITIONING_FAILURE) ||
                Objects.equals(alarmType, AlarmTypeConstant.BROADCAST_FAILURE) ||
                Objects.equals(alarmType, AlarmTypeConstant.MCM_FAILURE);
    }

    /**
     * @Author yuelei
     * @Desc 车门故障调图-晚点-中断-恢复
     * @Date 11:06 2022/6/19
     */
    public void changeGraphForDoorFailure(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getDoorFailureAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "调整运行图时,未获取到获取算法参数");
        //算法自检
        BaseResponse<String> baseResponse = algPowerClient.tractionPowerCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        log.info("获取算法参数成功,准备调整运行图");
        log.info("获取算法参数成功,获取所有备车信息：{}", JsonUtils.toJSONString(algorithmData.getDepotInfoDtoList()));
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> result;
        //大小交路调图
        result = algPowerClient.tractionPowerSolver(algorithmData);
        if (!result.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = result.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        //根据停车区域，获取站台编号
        log.info("交路停车区域ID集合：{}", algorithmResult.getPlatformStopAreaList());
        BasicCommonCacheUtils.set(Cache.PLATFORM_STOP_AREA_LIST, new PlatformStopAreaListDto(algorithmResult.getPlatformStopAreaList(), alarmInfo.getTableInfoId()));
        log.info("获取所有调整信息集合：{}", JsonUtils.toJSONString(algorithmResult.getTrainNumberAdjustDtoList()));
        //获取调整车次列表 dto 转 model
        List<TrainNumberAdjust> trainNumberAdjusts = tiasDataConvert.dtosToEntitys(algorithmResult.getTrainNumberAdjustDtoList());
        //新建车次调整对象
        List<TrainNumberAdjust> trainNumberAdjustNew = new ArrayList<>();
        //将加开车次存入缓存 等待执行
        for (TrainNumberAdjust trainNumberAdjust : trainNumberAdjusts) {
            log.info("调整车次列表:{}", JsonUtils.toJSONString(trainNumberAdjust));
            log.info("发送电子调度命令通知:{}", trainNumberAdjust.getAdjustType());
            trainNumberAdjust.setId(UidGeneratorUtils.getUID());
            trainNumberAdjust.setTAdmAlertInfoId(alarmInfo.getTableInfoId());
            trainNumberAdjustNew.add(trainNumberAdjust);
        }
        BasicCommonCacheUtils.hPut(Cache.TRAIN_NUMBER_ADJUST, String.valueOf(alarmInfo.getTableInfoId()), trainNumberAdjustNew, 10 * 60 * 60L, TimeUnit.SECONDS);
        log.info("获取所有的车次调整信息：{}", JsonUtils.toJSONString(trainNumberAdjusts));
        //压缩运行图
        String gzipGraph = algorithmResult.getCompressTrainGraphString();
        //入口
        planRunGraphService.updatePlanRunGraph(gzipGraph);
        log.info("加载运行图");
        //推送前端通知
        NotifyParam notifyParam = new NotifyParam();
        notifyParam.setMsgPushEnum(MsgPushEnum.SEND_CHANGE_TRAIN_GRAPH_MSG);
        notifyParam.setInfoId(alarmInfo.getTableInfoId());
        sendNotifyService.sendNotify(notifyParam);
        //向tias系统发送 更新运行图指令
        AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
        atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.CHANGE_TRAIN_GRAPH);
        atsByteCmdData.setParameter(TrainStateEnum.CHANGE_GRAPH.getValue());
        TrainCtrlPrv trainCtrlPrv = new TrainCtrlPrv();
        trainCtrlPrv.setAlarmInfoId(String.valueOf(alarmInfo.getTableInfoId()));
        atsByteCmdData.setTrainCtrl(trainCtrlPrv);
        atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
        log.info("下发更新运行图指令至ats");
        String alarmTypeDetail = String.valueOf(alarmInfo.getAlarmTypeDetail());
        if ((alarmTypeDetail.equals(AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE) && alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_5)
                || (alarmTypeDetail.equals(AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE) && alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_7)) {
            //走车次追踪获取表号
            TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent(alarmInfo.getTrainId());
            BasicCommonCacheUtils.set(Cache.AFTER_CHANGE_GRAPH_STATISTICS, new StatisticsAlgorithmDataDto(alarmInfo.getTableInfoId(), gzipGraph, (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH), alarmInfo.getStartAlarmTime(), tiasTraceInfo == null ? null : tiasTraceInfo.getServerNumber(), alarmInfo.getOrderNum()));
            log.info("存入故障恢复缓存，调图指标");
            //发送中断恢复调度命令，自xxx列车起后续列车恢复正常折返运行
            List<RecoveryTrainDto> recoveryTrainDtoList = algorithmResult.getRecoveryTrainDtoList();
            recoveryTrainDtoList.parallelStream().forEach(recoveryTrainDto -> {
                StringBuilder sb = new StringBuilder();
                recoveryTrainDto.getCmdAcceptStationList().forEach(stationId -> sb.append(stationId).append(","));
                disCmdSendUtils.sendDisCmd(alarmInfo, "自" + recoveryTrainDto.getRecoveryTrainNumber() + "次列车起，后续列车恢复正常方式运行；", sb.toString());
            });
        }
    }

    /**
     * @Author yuelei
     * @Desc 车门故障调图-晚点-缓行
     * @Date 11:06 2022/6/19
     */
    public void changeGraphForDoorSlowFailure(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getDoorFailureSlowAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "调整运行图时,未获取到获取算法参数");
        //算法自检
        BaseResponse<String> baseResponse = algPowerClient.tractionPowerCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        log.info("获取算法参数成功,准备调整运行图");
        log.info("获取算法参数成功,获取所有备车信息：{}", JsonUtils.toJSONString(algorithmData.getDepotInfoDtoList()));
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> result;
        //大小交路调图
        result = algPowerClient.tractionPowerSolver(algorithmData);
        if (!result.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = result.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        //根据停车区域，获取站台编号
        log.info("交路停车区域ID集合：{}", algorithmResult.getPlatformStopAreaList());
        BasicCommonCacheUtils.set(Cache.PLATFORM_STOP_AREA_LIST, new PlatformStopAreaListDto(algorithmResult.getPlatformStopAreaList(), alarmInfo.getTableInfoId()));
        log.info("获取所有调整信息集合：{}", JsonUtils.toJSONString(algorithmResult.getTrainNumberAdjustDtoList()));
        //获取调整车次列表 dto 转 model
        List<TrainNumberAdjust> trainNumberAdjusts = tiasDataConvert.dtosToEntitys(algorithmResult.getTrainNumberAdjustDtoList());
        //新建车次调整对象
        List<TrainNumberAdjust> trainNumberAdjustNew = new ArrayList<>();
        //将加开车次存入缓存 等待执行
        for (TrainNumberAdjust trainNumberAdjust : trainNumberAdjusts) {
            log.info("调整车次列表:{}", JsonUtils.toJSONString(trainNumberAdjust));
            log.info("发送电子调度命令通知:{}", trainNumberAdjust.getAdjustType());
            trainNumberAdjust.setId(UidGeneratorUtils.getUID());
            trainNumberAdjust.setTAdmAlertInfoId(alarmInfo.getTableInfoId());
            trainNumberAdjustNew.add(trainNumberAdjust);
        }
        BasicCommonCacheUtils.hPut(Cache.TRAIN_NUMBER_ADJUST, String.valueOf(alarmInfo.getTableInfoId()), trainNumberAdjustNew, 10 * 60 * 60L, TimeUnit.SECONDS);
        log.info("获取所有的车次调整信息：{}", JsonUtils.toJSONString(trainNumberAdjusts));
        //压缩运行图
        String gzipGraph = algorithmResult.getCompressTrainGraphString();
        //入口
        planRunGraphService.updatePlanRunGraph(gzipGraph);
        log.info("加载运行图");
        //推送前端通知
        NotifyParam notifyParam = new NotifyParam();
        notifyParam.setMsgPushEnum(MsgPushEnum.SEND_CHANGE_TRAIN_GRAPH_MSG);
        notifyParam.setInfoId(alarmInfo.getTableInfoId());
        sendNotifyService.sendNotify(notifyParam);
        //向tias系统发送 更新运行图指令
        AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
        atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.CHANGE_TRAIN_GRAPH);
        atsByteCmdData.setParameter(TrainStateEnum.CHANGE_GRAPH.getValue());
        TrainCtrlPrv trainCtrlPrv = new TrainCtrlPrv();
        trainCtrlPrv.setAlarmInfoId(String.valueOf(alarmInfo.getTableInfoId()));
        atsByteCmdData.setTrainCtrl(trainCtrlPrv);
        atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
        log.info("下发更新运行图指令至ats");
    }


    public void changeGraphForPower(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getPowerAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "调整运行图时,未获取到获取算法参数");
        //算法自检
        BaseResponse<String> baseResponse = algPowerClient.tractionPowerCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        log.info("获取算法参数成功,准备调整运行图");
        log.info("获取算法参数成功,获取所有备车信息：{}", JsonUtils.toJSONString(algorithmData.getDepotInfoDtoList()));
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> result;
        //大小交路调图
        result = algPowerClient.tractionPowerSolver(algorithmData);
        if (!result.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = result.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        int executeStep = alarmInfo.getExecuteStep();
        // 调中断图,算法返回需要跑小交路的站台
        if (NumConstant.TWO.equals(executeStep)) {
            List<Integer> platformStopAreaList = algorithmResult.getPlatformStopAreaList();
            if (!org.apache.commons.collections4.CollectionUtils.isEmpty(platformStopAreaList)) {
                PlatformStopAreaListDto platformStopAreaListDto = new PlatformStopAreaListDto();
                platformStopAreaListDto.setTableInfoId(alarmInfo.getTableInfoId());
                platformStopAreaListDto.setPlatformStopAreaList(platformStopAreaList);
                BasicCommonCacheUtils.set(Cache.SWITCH_FAILURE_STOP_AREA_LIST, platformStopAreaListDto);
            } else {
                log.error("算法调中断图时没有返回跑小交路的停车区域id");
            }
        }

        //获取调整车次列表 dto 转 model
        List<TrainNumberAdjust> trainNumberAdjusts = tiasDataConvert.dtosToEntitys(algorithmResult.getTrainNumberAdjustDtoList());
        //新建车次调整对象
        List<TrainNumberAdjust> trainNumberAdjustNew = new ArrayList<>();
        //将加开车次存入缓存 等待执行
        for (TrainNumberAdjust trainNumberAdjust : trainNumberAdjusts) {
            log.info("调整车次列表:{}", JsonUtils.toJSONString(trainNumberAdjust));
            log.info("发送电子调度命令通知:{}", trainNumberAdjust.getAdjustType());
            trainNumberAdjust.setId(UidGeneratorUtils.getUID());
            trainNumberAdjust.setTAdmAlertInfoId(alarmInfo.getTableInfoId());
            trainNumberAdjustNew.add(trainNumberAdjust);
        }
        BasicCommonCacheUtils.hPut(Cache.TRAIN_NUMBER_ADJUST, String.valueOf(alarmInfo.getTableInfoId()), trainNumberAdjustNew, 10 * 60 * 60L, TimeUnit.SECONDS);
        log.info("获取所有的车次调整信息：{}", JsonUtils.toJSONString(trainNumberAdjusts));
        //压缩运行图
        String gzipGraph = algorithmResult.getCompressTrainGraphString();
        //入口
        planRunGraphService.updatePlanRunGraph(gzipGraph);
        log.info("加载运行图");
        //推送前端通知
        NotifyParam notifyParam = new NotifyParam();
        notifyParam.setMsgPushEnum(MsgPushEnum.SEND_CHANGE_TRAIN_GRAPH_MSG);
        notifyParam.setInfoId(alarmInfo.getTableInfoId());
        sendNotifyService.sendNotify(notifyParam);
        //向tias系统发送 更新运行图指令
        AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
        atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.CHANGE_TRAIN_GRAPH);
        atsByteCmdData.setParameter(TrainStateEnum.CHANGE_GRAPH.getValue());
        TrainCtrlPrv trainCtrlPrv = new TrainCtrlPrv();
        trainCtrlPrv.setAlarmInfoId(String.valueOf(alarmInfo.getTableInfoId()));
        atsByteCmdData.setTrainCtrl(trainCtrlPrv);
        atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
        log.info("下发更新运行图指令至ats");

        //故障恢复调图时新增指标统计
        if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_2 || alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4) {
            //走车次追踪获取表号
            BasicCommonCacheUtils.set(Cache.AFTER_CHANGE_GRAPH_STATISTICS, new StatisticsAlgorithmDataDto(alarmInfo.getTableInfoId(), gzipGraph, (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH), alarmInfo.getStartAlarmTime(), "", ""));
            log.info("存入故障恢复缓存，调图指标:{}", alarmInfo.getAlarmTypeDetail());
        }

    }


    public void changeGraphForLock(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getLockAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "调整运行图时,未获取到获取算法参数");
        //算法自检
        BaseResponse<String> baseResponse = algPowerClient.tractionPowerCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        log.info("获取算法参数成功,准备调整运行图");
        log.info("获取算法参数成功,获取所有备车信息：{}", JsonUtils.toJSONString(algorithmData.getDepotInfoDtoList()));
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> result;
        // 故障恢复调图或 大小交路
        result = algPowerClient.tractionPowerSolver(algorithmData);
        if (!result.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = result.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        int executeStep = alarmInfo.getExecuteStep();
        // 调中断图,算法返回需要跑小交路的站台
        if (NumConstant.TWO.equals(executeStep)) {
            List<Integer> platformStopAreaList = algorithmResult.getPlatformStopAreaList();
            if (!org.apache.commons.collections4.CollectionUtils.isEmpty(platformStopAreaList)) {
                PlatformStopAreaListDto platformStopAreaListDto = new PlatformStopAreaListDto();
                platformStopAreaListDto.setTableInfoId(alarmInfo.getTableInfoId());
                platformStopAreaListDto.setPlatformStopAreaList(platformStopAreaList);
                BasicCommonCacheUtils.set(Cache.SWITCH_FAILURE_STOP_AREA_LIST, platformStopAreaListDto);
            } else {
                log.error("算法调中断图时没有返回跑小交路的停车区域id");
            }
        }

        //获取调整车次列表 dto 转 model
        List<TrainNumberAdjust> trainNumberAdjusts = tiasDataConvert.dtosToEntitys(algorithmResult.getTrainNumberAdjustDtoList());
        //新建车次调整对象
        List<TrainNumberAdjust> trainNumberAdjustNew = new ArrayList<>();
        //将加开车次存入缓存 等待执行
        for (TrainNumberAdjust trainNumberAdjust : trainNumberAdjusts) {
            log.info("调整车次列表:{}", JsonUtils.toJSONString(trainNumberAdjust));
            log.info("发送电子调度命令通知:{}", trainNumberAdjust.getAdjustType());
            trainNumberAdjust.setId(UidGeneratorUtils.getUID());
            trainNumberAdjust.setTAdmAlertInfoId(alarmInfo.getTableInfoId());
            trainNumberAdjustNew.add(trainNumberAdjust);
        }
        BasicCommonCacheUtils.hPut(Cache.TRAIN_NUMBER_ADJUST, String.valueOf(alarmInfo.getTableInfoId()), trainNumberAdjustNew, 10 * 60 * 60L, TimeUnit.SECONDS);
        log.info("获取所有的车次调整信息：{}", JsonUtils.toJSONString(trainNumberAdjusts));
        //压缩运行图
        String gzipGraph = algorithmResult.getCompressTrainGraphString();
        //入口
        planRunGraphService.updatePlanRunGraph(gzipGraph);
        log.info("加载运行图");
        //推送前端通知
        NotifyParam notifyParam = new NotifyParam();
        notifyParam.setMsgPushEnum(MsgPushEnum.SEND_CHANGE_TRAIN_GRAPH_MSG);
        notifyParam.setInfoId(alarmInfo.getTableInfoId());
        sendNotifyService.sendNotify(notifyParam);
        //向tias系统发送 更新运行图指令
        AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
        atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.CHANGE_TRAIN_GRAPH);
        atsByteCmdData.setParameter(TrainStateEnum.CHANGE_GRAPH.getValue());
        TrainCtrlPrv trainCtrlPrv = new TrainCtrlPrv();
        trainCtrlPrv.setAlarmInfoId(String.valueOf(alarmInfo.getTableInfoId()));
        atsByteCmdData.setTrainCtrl(trainCtrlPrv);
        atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
        log.info("下发更新运行图指令至ats");

        //故障恢复调图时新增指标统计
        if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_3) {
            //走车次追踪获取表号
            BasicCommonCacheUtils.set(Cache.AFTER_CHANGE_GRAPH_STATISTICS, new StatisticsAlgorithmDataDto(alarmInfo.getTableInfoId(), gzipGraph, (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH), alarmInfo.getStartAlarmTime(), "", ""));
            log.info("存入故障恢复缓存，调图指标:{}", alarmInfo.getAlarmTypeDetail());
        }
    }


    public void changeGraphForSignalElec(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getLockAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "调整运行图时,未获取到获取算法参数");
        //算法自检
        BaseResponse<String> baseResponse = algPowerClient.tractionPowerCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        log.info("获取算法参数成功,准备调整运行图");
        log.info("获取算法参数成功,获取所有备车信息：{}", JsonUtils.toJSONString(algorithmData.getDepotInfoDtoList()));
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> result;
        // 故障恢复调图或 大小交路
        result = algPowerClient.tractionPowerSolver(algorithmData);
        if (!result.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = result.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        int executeStep = alarmInfo.getExecuteStep();
        // 调中断图,算法返回需要跑小交路的站台
        if (NumConstant.TWO.equals(executeStep)) {
            List<Integer> platformStopAreaList = algorithmResult.getPlatformStopAreaList();
            if (!org.apache.commons.collections4.CollectionUtils.isEmpty(platformStopAreaList)) {
                PlatformStopAreaListDto platformStopAreaListDto = new PlatformStopAreaListDto();
                platformStopAreaListDto.setTableInfoId(alarmInfo.getTableInfoId());
                platformStopAreaListDto.setPlatformStopAreaList(platformStopAreaList);
                BasicCommonCacheUtils.set(Cache.SWITCH_FAILURE_STOP_AREA_LIST, platformStopAreaListDto);
            } else {
                log.error("算法调中断图时没有返回跑小交路的停车区域id");
            }
        }

        //获取调整车次列表 dto 转 model
        List<TrainNumberAdjust> trainNumberAdjusts = tiasDataConvert.dtosToEntitys(algorithmResult.getTrainNumberAdjustDtoList());
        //新建车次调整对象
        List<TrainNumberAdjust> trainNumberAdjustNew = new ArrayList<>();
        //将加开车次存入缓存 等待执行
        for (TrainNumberAdjust trainNumberAdjust : trainNumberAdjusts) {
            log.info("调整车次列表:{}", JsonUtils.toJSONString(trainNumberAdjust));
            log.info("发送电子调度命令通知:{}", trainNumberAdjust.getAdjustType());
            trainNumberAdjust.setId(UidGeneratorUtils.getUID());
            trainNumberAdjust.setTAdmAlertInfoId(alarmInfo.getTableInfoId());
            trainNumberAdjustNew.add(trainNumberAdjust);
        }
        BasicCommonCacheUtils.hPut(Cache.TRAIN_NUMBER_ADJUST, String.valueOf(alarmInfo.getTableInfoId()), trainNumberAdjustNew, 10 * 60 * 60L, TimeUnit.SECONDS);
        log.info("获取所有的车次调整信息：{}", JsonUtils.toJSONString(trainNumberAdjusts));
        //获取压缩运行图
        String gzipGraph = algorithmResult.getCompressTrainGraphString();
        planRunGraphService.updatePlanRunGraph(gzipGraph);
        log.info("调整计划运行图加载ATS数据库成功");
        //推送前端通知
        NotifyParam notifyParam = new NotifyParam();
        notifyParam.setMsgPushEnum(MsgPushEnum.SEND_CHANGE_TRAIN_GRAPH_MSG);
        notifyParam.setInfoId(alarmInfo.getTableInfoId());
        sendNotifyService.sendNotify(notifyParam);
        //向tias系统发送 更新运行图指令
        AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
        atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.CHANGE_TRAIN_GRAPH);
        atsByteCmdData.setParameter(TrainStateEnum.CHANGE_GRAPH.getValue());
        TrainCtrlPrv trainCtrlPrv = new TrainCtrlPrv();
        trainCtrlPrv.setAlarmInfoId(String.valueOf(alarmInfo.getTableInfoId()));
        atsByteCmdData.setTrainCtrl(trainCtrlPrv);
        atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
        log.info("下发更新运行图指令至ats");

        //故障恢复调图时新增指标统计
        if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_3) {
            //走车次追踪获取表号
//            TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent(alarmInfo.getTrainId());
            BasicCommonCacheUtils.set(Cache.AFTER_CHANGE_GRAPH_STATISTICS, new StatisticsAlgorithmDataDto(alarmInfo.getTableInfoId(), gzipGraph, (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH), alarmInfo.getStartAlarmTime(), "", ""));
            log.info("存入故障恢复缓存，调图指标:{}", alarmInfo.getAlarmTypeDetail());
        }
    }

    //车次追踪数据中根据车次号获取车组号
    private String getTrainIdByOrderNumber(String trainOrderNumber, String serverNumber) {
        for (Map.Entry<String, TiasTraceInfo> entry : trainTraceCache.asMap().entrySet()) {
            TiasTraceInfo tiasTraceInfo = entry.getValue();
            if (tiasTraceInfo.getOrderNumber().equals(trainOrderNumber) && tiasTraceInfo.getServerNumber().equals(serverNumber)) {
                return tiasTraceInfo.getTrainId();
            }
        }
        log.info("通过车次号：{}和表号：{}未获取到车组号", trainOrderNumber, serverNumber);
        return "";
    }

    /**
     * @param alarmInfo 故障信息
     * @return com.tct.model.vo.AlgorithmData
     * @Description 获取算法参数
     * @Author zhangyinglong
     * @Date 2021/5/19 20:01
     */

    public AlgorithmData getAlgorithmData(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        //处理特殊情况：在折返轨换端前录入故障，在换端后调图
        if (isAirConditionBroadcastMCM(alarmInfo) && Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
            String trainId = alarmInfo.getTrainId();
            TiasTraceInfo traceInfo = trainTraceCache.getIfPresent(trainId);
            //设置OrderNumber
            if (Objects.isNull(traceInfo)) {
                log.error("根据车组号【{}】获取车次追踪数据失败！", trainId);
                throw new BizException(CodeEnum.NO_GET_TRAIN_TRACE);
            }
            alarmInfo1.setOrderNum(traceInfo.getOrderNumber());
        }
        log.info("获取算法参数传入alarmInfo为:{}", alarmInfo);
        //获取算法参数
        AlgorithmData algorithmData;
        //获取运行图
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        log.info("获取到运行图");
        log.info("获取查询ATS数据库中车辆段的所有备车信息");
        //查询ATS数据库中车辆段的所有备车信息
        //查询ATS数据库中车辆段的所有备车信息
        BaseResponse<DepotInfoDto> baseResponse = depotDataClient.getSpareCar();
        if(!baseResponse.getSuccess()){
            throw new BizException(CodeEnum.GET_BACK_CAR_FAIL);
        }
        DepotInfoDto depotInfo = baseResponse.getData();
        log.info("获取到查询ATS数据库中车辆段的所有备车信息{}", depotInfo);
        ConcurrentMap<String, TiasTraceInfo> map = trainTraceCache.asMap();
        if (CollectionUtils.isEmpty(map)) {
            throw new BizException("车次追踪数据不存在！");
        }
        List<TiasTraceInfo> tiastraceInfoList = new ArrayList<>(map.values());
        tiastraceInfoList.forEach(tiasTraceInfo -> tiasTraceInfo.setOrderNumber(AlarmUtil.getOrderNum(tiasTraceInfo.getOrderNumber())));
        //处理备车信息，筛选出处于车辆掉或者停车场被激活的车次
        List<DepotInfoDto> spareCar = this.getSpareCar(depotInfo, tiastraceInfoList);
        // 根据是否第二天更新时间戳
        this.changeTimestampIfTomorrow(alarmInfo1);
        if (!StringUtils.isEmpty(alarmInfo.getTrainId()) && !"-1".equals(alarmInfo.getTrainId())) {
            //重新赋值车组号，去掉线路ID
            alarmInfo1.setTrainId(AlarmUtil.getTrainId(alarmInfo1));
        }
        if (!StringUtils.isEmpty(alarmInfo.getOrderNum()) && !"-1".equals(alarmInfo.getOrderNum())) {
            //如果车次号为2位，转Integer再转String
            alarmInfo1.setOrderNum(AlarmUtil.getOrderNum(alarmInfo1.getOrderNum()));
        }
        algorithmData = new AlgorithmData(zipPlanRunGraph, alarmInfo1, spareCar, tiastraceInfoList);
        algorithmData.setCaseCode(alarmInfo1.getCaseCode());
        //通用清客时长
        algorithmData.setClearPassengerTime(Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_CLEAR_TIME)));
        //2022-03-25 09:47:12.016
        algorithmData.setNowTime(DateUtil.getTimeStamp());
        // 加扣车参数
        algorithmData.setHoldTrainList(getHoldTrains(alarmInfo.getTableInfoId()));
        log.info("所有备车信息：{}", JsonUtils.toJSONString(depotInfo));
        return algorithmData;
    }

    public List<HoldOffTrainTimeDto> getHoldTrains(long infoId) {
        if (!BasicCommonCacheUtils.exist(Cache.HOLD_AND_OFF_TRAIN)) {
            return Collections.emptyList();
        }
        Map<Object, Object> holdTrainMap = BasicCommonCacheUtils.hmget(Cache.HOLD_AND_OFF_TRAIN, HoldOffTrainTimeDto.class);
        List<HoldOffTrainTimeDto> list = Lists.newArrayList();
        for (Map.Entry<Object, Object> entry : holdTrainMap.entrySet()) {
            HoldOffTrainTimeDto holdOffTrainTimeDto = (HoldOffTrainTimeDto) entry.getValue();
            if (holdOffTrainTimeDto.getTableInfoId() == infoId) {
                PlatformInfo info = platformInfoService.getPlatformByPId(holdOffTrainTimeDto.getPlatformId());
                holdOffTrainTimeDto.setStopArea(info.getParkArea());
                list.add(holdOffTrainTimeDto);
            }
        }
        return list;
    }

    /**
     * @param depotInfoDto 车辆段信息
     * @param infoList     车次追踪信息
     * @return List<DepotInfoDto> 备车信息
     * @Description 处理备车信息
     * @Author yuelei
     * @Date 2022/3/17 14:08
     */
    public List<DepotInfoDto> getSpareCar(DepotInfoDto depotInfoDto, List<TiasTraceInfo> infoList) {
        log.info("获取数据库所有备车信息：{}", JsonUtils.toJSONString(depotInfoDto));
        //判断是否有备车信息
        if(CollectionUtils.isEmpty(depotInfoDto.getSpareTrainInfoList())){
            log.info("未获取到备车信息");
        }
        //获取所有的车辆段或者停车场
        List<Integer> depotAndPark = admStationService.getDepotAndPark();
        //存储车次追踪对应关系
        Map<String, Integer> map = new HashMap<>();
        infoList.forEach(info -> map.put(info.getTrainId(), info.getStationId()));
        Map<Integer, List<String>> dataMap = new HashMap<>();
        //移除车次追踪不存在的，以及不处于车辆段和停车场的车次
        depotInfoDto.getSpareTrainInfoList().forEach(dto -> {
            String trainId = dto.getTrainId();;
            if (map.containsKey(trainId) && depotAndPark.contains(map.get(trainId))) {
                Integer stationId = map.get(trainId);
                List<String> list = new ArrayList<>();
                if (dataMap.containsKey(stationId)) {
                    list = dataMap.get(stationId);
                }
                list.add(String.valueOf(dto.getTrainId()));
                dataMap.put(stationId, list);
            }
        });
        //组装车辆掉与备车的关系
        List<DepotInfoDto> dataList = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : dataMap.entrySet()) {
            DepotInfoDto newDepotInfoDto = new DepotInfoDto();
            newDepotInfoDto.setStationId(entry.getKey());
            List<BackUpPlanDetailDto> spareCarInfoDtoList = new ArrayList<>();
            entry.getValue().forEach(str -> spareCarInfoDtoList.add(new BackUpPlanDetailDto(str)));
            newDepotInfoDto.setSpareTrainInfoList(spareCarInfoDtoList);
            dataList.add(newDepotInfoDto);
        }
        log.info("筛选出被激活列车：{}", JsonUtils.toJSONString(dataList));
        //停车场、车辆段信息
        if (dataList.size() != depotAndPark.size()) {
            List<Integer> collect = dataList.stream().map(DepotInfoDto::getStationId).collect(Collectors.toList());
            depotAndPark.forEach(id -> {
                //不包含
                if (!collect.contains(id)) {
                    DepotInfoDto dto = new DepotInfoDto();
                    dto.setStationId(id);
                    dataList.add(dto);
                }
            });
        }
        return dataList;
    }

    /**
     * @param alarmInfo 故障信息
     * @return com.tct.model.vo.AlgorithmData
     * @Description 获取算法参数
     * @Author zhangyinglong
     * @Date 2021/5/19 20:01
     */
    public AlgorithmData getSFAlgorithmData(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        //获取算法参数
        AlgorithmData algorithmData;
        //获取运行图
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        //查询ATS数据库中车辆段的所有备车信息
        //查询ATS数据库中车辆段的所有备车信息
        BaseResponse<DepotInfoDto> baseResponse = depotDataClient.getSpareCar();
        if(!baseResponse.getSuccess()){
            throw new BizException(CodeEnum.GET_BACK_CAR_FAIL);
        }
        DepotInfoDto depotInfo = baseResponse.getData();
        log.info("获取到查询ATS数据库中车辆段的所有备车信息{}", depotInfo);
        ConcurrentMap<String, TiasTraceInfo> map = trainTraceCache.asMap();
        List<TiasTraceInfo> tiastraceInfoList = new ArrayList<>(map.values());
        //处理备车信息，筛选出处于车辆掉或者停车场被激活的车次
        List<DepotInfoDto> spareCar = this.getSpareCar(depotInfo, tiastraceInfoList);
        // 根据是否第二天更新时间戳
        this.changeTimestampIfTomorrow(alarmInfo1);
        String planGraph = (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH);
        algorithmData = getSwitchFailreAlgData(alarmInfo1, zipPlanRunGraph, spareCar, tiastraceInfoList, planGraph);
        algorithmData.setClearPassengerTime(Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_CLEAR_TIME)));
        log.info("所有备车信息：{}", JsonUtils.toJSONString(depotInfo));
        return algorithmData;
    }


    public AlgorithmData getAxleCounterAlgorithmData(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        //获取算法参数
        AlgorithmData algorithmData;
        //获取运行图
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        log.info("获取到运行图");
        //查询ATS数据库中车辆段的所有备车信息
        //查询ATS数据库中车辆段的所有备车信息
        BaseResponse<DepotInfoDto> baseResponse = depotDataClient.getSpareCar();
        if(!baseResponse.getSuccess()){
            throw new BizException(CodeEnum.GET_BACK_CAR_FAIL);
        }
        DepotInfoDto depotInfo = baseResponse.getData();
        log.info("获取查询ATS数据库中车辆段的所有备车信息");
        ConcurrentMap<String, TiasTraceInfo> map = trainTraceCache.asMap();
        List<TiasTraceInfo> tiastraceInfoList = new ArrayList<>(map.values());
        //处理备车信息，筛选出处于车辆掉或者停车场被激活的车次
        List<DepotInfoDto> spareCar = this.getSpareCar(depotInfo, tiastraceInfoList);
        // 根据是否第二天更新时间戳
        this.changeTimestampIfTomorrow(alarmInfo1);
        String planGraph = (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH);
        log.info("组装算法参数alarmInfo:【{}】", JsonUtils.toJSONString(alarmInfo1));
        algorithmData = getAxleCounterAlgData(alarmInfo1, zipPlanRunGraph, spareCar, tiastraceInfoList, planGraph);
        //添加折返时间，走数据库获取
        algorithmData.setClearPassengerTime(Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_CLEAR_TIME)));
        algorithmData.setNowTime(DateUtil.getTimeStamp());
        return algorithmData;
    }


    public AlgorithmData getPowerAlgorithmData(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        log.info("获取算法参数传入alarmInfo为:{}", alarmInfo);
        //获取算法参数
        AlgorithmData algorithmData;
        //获取运行图
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        log.info("获取到运行图");
        log.info("获取查询ATS数据库中车辆段的所有备车信息");
        //查询ATS数据库中车辆段的所有备车信息
        //查询ATS数据库中车辆段的所有备车信息
        BaseResponse<DepotInfoDto> baseResponse = depotDataClient.getSpareCar();
        if(!baseResponse.getSuccess()){
            throw new BizException(CodeEnum.GET_BACK_CAR_FAIL);
        }
        DepotInfoDto depotInfo = baseResponse.getData();
        log.info("获取到查询ATS数据库中车辆段的所有备车信息{}", depotInfo);
        ConcurrentMap<String, TiasTraceInfo> map = trainTraceCache.asMap();
        List<TiasTraceInfo> tiastraceInfoList = new ArrayList<>(map.values());
        //处理备车信息，筛选出处于车辆掉或者停车场被激活的车次
        List<DepotInfoDto> spareCar = this.getSpareCar(depotInfo, tiastraceInfoList);
        // 根据是否第二天更新时间戳
        this.changeTimestampIfTomorrow(alarmInfo1);
        // 获取失电区段所有逻辑区段
        List<Integer> sectionList = logicSectionDataService.getLogicIdListByTrackId(alarmInfo1.getTractionSectionId());
        String planGraph = (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH);
        //获取调图冗余参数,时间毫秒
        long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
        int recoverCode = 1;
        // 当正向第四步或者故障恢复-2时算法为中断恢复
        switch (alarmInfo.getExecuteStep()) {
            //1.中断

            //2.中断恢复
            case IidsConstPool.EXECUTE_STEP_0_2:
                recoverCode = 2;
                // 并且故障结束时间为当前时间+1分钟冗余
                if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                    String endAlarmTime = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                    alarmInfo1.setEndAlarmTime(endAlarmTime);
                } else {
                    String endTime = DateUtil.getTimeStamp(new Date().getTime() + setRedundancyTime);
                    BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endTime);
                    alarmInfo1.setEndAlarmTime(endTime);
                }
                break;

            //3.2分钟晚点恢复图
            case IidsConstPool.EXECUTE_STEP_0_1:
                recoverCode = RecoveryCodeEnum.LATE_SWITCH.getCode();
                if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                    String endAlarmTime = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                    alarmInfo1.setEndAlarmTime(endAlarmTime);
                } else {
                    String endTime = DateUtil.getTimeStamp(new Date().getTime() + setRedundancyTime);
                    BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endTime);
                    alarmInfo1.setEndAlarmTime(endTime);
                }
        }
        alarmInfo1.setAlarmState(0);
        algorithmData = new TractionPowerAlgorithmData(zipPlanRunGraph, alarmInfo1, spareCar, tiastraceInfoList, planGraph, recoverCode, sectionList, alarmInfo.getTractionSectionId());
        algorithmData.setNowTime(DateUtil.getTimeStamp(System.currentTimeMillis()));
        algorithmData.setCaseCode(alarmInfo1.getCaseCode());
        //添加折返时间，走数据库获取
        algorithmData.setClearPassengerTime(Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_CLEAR_TIME)));
        //设置扣车参数
        algorithmData.setHoldTrainList(this.getHoldTrains(alarmInfo.getTableInfoId()));
        log.info("所有备车信息：{}", JsonUtils.toJSONString(depotInfo));
        return algorithmData;
    }

    public AlgorithmData getDoorFailureAlgorithmData(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        log.info("获取算法参数传入alarmInfo为:{}", alarmInfo);
        //获取算法参数
        AlgorithmData algorithmData;
        //获取运行图
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        log.info("获取到运行图");
        log.info("获取查询ATS数据库中车辆段的所有备车信息");
        //查询ATS数据库中车辆段的所有备车信息
        //查询ATS数据库中车辆段的所有备车信息
        BaseResponse<DepotInfoDto> baseResponse = depotDataClient.getSpareCar();
        if(!baseResponse.getSuccess()){
            throw new BizException(CodeEnum.GET_BACK_CAR_FAIL);
        }
        DepotInfoDto depotInfo = baseResponse.getData();
        log.info("获取到查询ATS数据库中车辆段的所有备车信息{}", depotInfo);
        ConcurrentMap<String, TiasTraceInfo> map = trainTraceCache.asMap();
        if (CollectionUtils.isEmpty(map)) {
            throw new BizException("车次追踪数据不存在！");
        }
        List<TiasTraceInfo> tiastraceInfoList = new ArrayList<>(map.values());
        //处理备车信息，筛选出处于车辆掉或者停车场被激活的车次
        List<DepotInfoDto> spareCar = this.getSpareCar(depotInfo, tiastraceInfoList);
        // 根据是否第二天更新时间戳
        this.changeTimestampIfTomorrow(alarmInfo1);
        if (!StringUtils.isEmpty(alarmInfo.getOrderNum()) && !"-1".equals(alarmInfo.getOrderNum())) {
            //如果车次号为2位，转Integer再转String
            alarmInfo1.setOrderNum(AlarmUtil.getOrderNum(alarmInfo1.getOrderNum()));
        }
        String planGraph = "";
        int recoverCode = RecoveryCodeEnum.LATE_BREAK.getCode();
        // 当正向第四步或者故障恢复-2时算法为中断恢复
        if (alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE)) {
            if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4) {
                //中断
                planGraph = (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH);
                recoverCode = RecoveryCodeEnum.BREAK.getCode();
            } else if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_5) {
                //中断到恢复需要原图
                planGraph = (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH);
                recoverCode = RecoveryCodeEnum.BREAK_RECOVERY.getCode();
            }
        } else if (alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE)) {
            if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_6) {
                //中断
                planGraph = (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH);
                recoverCode = RecoveryCodeEnum.BREAK.getCode();
            } else if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_7) {
                alarmInfo1.setAlarmState(AlarmStateEnum.TRAIN_DOOR_STATION_DROP_LINE.getCode());
                //中断到恢复需要原图
                planGraph = (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH);
                recoverCode = RecoveryCodeEnum.BREAK_RECOVERY.getCode();
            }
        }
        algorithmData = new DoorFailureAlgorithmData(zipPlanRunGraph, alarmInfo1, spareCar, tiastraceInfoList, planGraph, recoverCode);
        algorithmData.setNowTime(DateUtil.getTimeStamp(System.currentTimeMillis()));
        algorithmData.setCaseCode(alarmInfo1.getCaseCode());
        algorithmData.setClearPassengerTime(Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_CLEAR_TIME)));
        //添加扣车信息
        algorithmData.setHoldTrainList(this.getHoldTrains(alarmInfo.getTableInfoId()));
        log.info("所有备车信息：{}", JsonUtils.toJSONString(depotInfo));
        return algorithmData;
    }

    public AlgorithmData getDoorFailureSlowAlgorithmData(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        log.info("获取算法参数传入alarmInfo为:{}", alarmInfo);
        //获取算法参数
        AlgorithmData algorithmData;
        //获取运行图
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        log.info("获取到运行图");
        log.info("获取查询ATS数据库中车辆段的所有备车信息");
        //查询ATS数据库中车辆段的所有备车信息
        //查询ATS数据库中车辆段的所有备车信息
        BaseResponse<DepotInfoDto> baseResponse = depotDataClient.getSpareCar();
        if(!baseResponse.getSuccess()){
            throw new BizException(CodeEnum.GET_BACK_CAR_FAIL);
        }
        DepotInfoDto depotInfo = baseResponse.getData();
        log.info("获取到查询ATS数据库中车辆段的所有备车信息{}", depotInfo);
        ConcurrentMap<String, TiasTraceInfo> map = trainTraceCache.asMap();
        if (CollectionUtils.isEmpty(map)) {
            throw new BizException("车次追踪数据不存在！");
        }
        List<TiasTraceInfo> tiastraceInfoList = new ArrayList<>(map.values());
        //处理备车信息，筛选出处于车辆掉或者停车场被激活的车次
        List<DepotInfoDto> spareCar = this.getSpareCar(depotInfo, tiastraceInfoList);
        // 根据是否第二天更新时间戳
        this.changeTimestampIfTomorrow(alarmInfo1);
        algorithmData = new DoorFailureAlgorithmData(zipPlanRunGraph, alarmInfo1, spareCar, tiastraceInfoList, null, RecoveryCodeEnum.LATE_SLOW_DOWN.getCode());
        algorithmData.setNowTime(DateUtil.getTimeStamp(System.currentTimeMillis()));
        //添加扣车信息
        algorithmData.setHoldTrainList(this.getHoldTrains(alarmInfo.getTableInfoId()));
        log.info("所有备车信息：{}", JsonUtils.toJSONString(depotInfo));
        return algorithmData;
    }

    public AlgorithmData getLockAlgorithmData(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        log.info("获取算法参数传入alarmInfo为:{}", alarmInfo);
        //获取算法参数
        AlgorithmData algorithmData;
        //获取运行图
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        log.info("获取到运行图");
        log.info("获取查询ATS数据库中车辆段的所有备车信息");
        //查询ATS数据库中车辆段的所有备车信息
        //查询ATS数据库中车辆段的所有备车信息
        BaseResponse<DepotInfoDto> baseResponse = depotDataClient.getSpareCar();
        if(!baseResponse.getSuccess()){
            throw new BizException(CodeEnum.GET_BACK_CAR_FAIL);
        }
        DepotInfoDto depotInfo = baseResponse.getData();
        log.info("获取到查询ATS数据库中车辆段的所有备车信息{}", depotInfo);
        ConcurrentMap<String, TiasTraceInfo> map = trainTraceCache.asMap();

        List<TiasTraceInfo> tiastraceInfoList = new ArrayList<>(map.values());
        //处理备车信息，筛选出处于车辆掉或者停车场被激活的车次
        List<DepotInfoDto> spareCar = this.getSpareCar(depotInfo, tiastraceInfoList);
        // 根据是否第二天更新时间戳
        this.changeTimestampIfTomorrow(alarmInfo1);

        // 获取集中区对应的扣车区域
        List<Integer> parkList = new ArrayList<>();
        // 获取故障集中区的集中站车站id(前端多选传入以逗号隔开)
        List<String> list = Arrays.asList(alarmInfo.getAlarmConStation().split(","));

        // 集中区上所有计轴编号(修改为联锁区段内所有计轴编号)
        List<Integer> axisIdList = new ArrayList<>();
        //根据联锁id找到所有集中区id
        List<Integer> rtuIdList = mapLinkService.getRtuIdsByCiId(Integer.valueOf(alarmInfo.getAlarmConStation()));
        //根据集中区id查找到所有的计轴id
        rtuIdList.forEach(t -> {
            List<Integer> axisIdListTemp = axisInfoService.getAxisIdInfoByCiId(t);
            axisIdListTemp.forEach(a -> {
                axisIdList.add(a);
            });
        });
        String planGraph = (String) BasicCommonCacheUtils.get(Cache.PLAN_GRAPH);
        int recoverCode = 1;
        //获取调图冗余参数,时间毫秒
        long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));

        //1.中断

        //2.中断恢复
        if (IidsConstPool.EXECUTE_STEP_0_3 == alarmInfo.getExecuteStep()) {
            recoverCode = 2;
            // 并且故障结束时间为当前时间+1分钟冗余
            if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                String endAlarmTime = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                alarmInfo1.setEndAlarmTime(endAlarmTime);
            } else {
                String endTime = DateUtil.getTimeStamp(new Date().getTime() + setRedundancyTime);
                BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endTime);
                alarmInfo1.setEndAlarmTime(endTime);
            }
        }
        //3. 2分钟晚点恢复图
        if (IidsConstPool.EXECUTE_STEP_0_1 == alarmInfo.getExecuteStep()) {
            recoverCode = RecoveryCodeEnum.LATE_SWITCH.getCode();
            if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                String endAlarmTime = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                alarmInfo1.setEndAlarmTime(endAlarmTime);
            } else {
                String endTime = DateUtil.getTimeStamp(new Date().getTime() + setRedundancyTime);
                BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endTime);
                alarmInfo1.setEndAlarmTime(endTime);
            }
        }

        alarmInfo1.setAlarmState(0);
        algorithmData = new LockAlgorithmData(zipPlanRunGraph, alarmInfo1, spareCar, tiastraceInfoList, planGraph, recoverCode, parkList, axisIdList, Integer.valueOf(alarmInfo.getAlarmConStation()));
        algorithmData.setNowTime(DateUtil.getTimeStamp(System.currentTimeMillis()));
        algorithmData.setCaseCode(alarmInfo1.getCaseCode());
        algorithmData.setClearPassengerTime(Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_CLEAR_TIME)));
        algorithmData.setHoldTrainList(getHoldTrains(alarmInfo1.getTableInfoId()));
        log.info("所有备车信息：{}", JsonUtils.toJSONString(depotInfo));
        return algorithmData;
    }


    /**
     * 根据不同的场景进行算法参数组装
     *
     * @param alarmInfo1        告警信息
     * @param zipPlanRunGraph   当前运行图
     * @param depotInfo         数据库中车辆段的所有备车信息
     * @param tiastraceInfoList 车次追踪信息
     * @param oldPlanGraph      原运行图
     * @return com.tct.model.vo.AlgorithmData
     * @author liyunlong
     * @date 2022/3/9 14:10
     */
    private AlgorithmData getSwitchFailreAlgData(AlarmInfo alarmInfo1, String zipPlanRunGraph, List<DepotInfoDto> depotInfo, List<TiasTraceInfo> tiastraceInfoList, String oldPlanGraph) {
        AlgorithmData algorithmData;
        int alarmTypeDetail = alarmInfo1.getAlarmTypeDetail();
        int executeStep = alarmInfo1.getExecuteStep();
        Boolean autoReport = Objects.isNull(alarmInfo1.getAutoReport()) ? Boolean.FALSE : alarmInfo1.getAutoReport();
        Integer recoveryCode = null;
        String slowlyStartTime = null;
        String compressOldTrainGraphString = null;
        String slowlyEndTime = null;
        switch (executeStep) {
            case 1:
                // 中断
            case 2:
                if (SwitchFailureTypeEnum.MIDDLE_FAILURE_SINGLE.getCode() == alarmTypeDetail) {
                    recoveryCode = RecoveryCodeEnum.SINGLE_BREAK.getCode();
                } else if (SwitchFailureTypeEnum.BEHIND_FAILURE_CHANGE.getCode() == alarmTypeDetail) {
                    recoveryCode = RecoveryCodeEnum.SLOWLY_CHANGE.getCode();
                    slowlyStartTime = DateUtil.getTimeStamp(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss.SSS");
                    // 60分钟时间设置
                    Integer switchFailureTime = Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.SWITCH_FAILURE_THIRTY_PRRVIEW_TIME));
                    slowlyEndTime = DateUtil.getTimeStamp(System.currentTimeMillis() + switchFailureTime, "yyyy-MM-dd" + " HH:mm:ss.SSS");
                } else {
                    recoveryCode = RecoveryCodeEnum.BREAK.getCode();
                }
                compressOldTrainGraphString = oldPlanGraph;
                break;
            // 中断恢复或中断缓行
            case 3:
                // 中断恢复
                if (alarmTypeDetail == 1201) {
                    recoveryCode = RecoveryCodeEnum.BREAK_RECOVERY.getCode();
                    compressOldTrainGraphString = oldPlanGraph;
                }
                // 中断缓行
                else {
                    // 终端道岔故障缓行
                    if (SwitchFailureTypeEnum.BEHIND_FAILURE_NOT_HAS_FRONT_TURN.getCode() == alarmTypeDetail
                            || SwitchFailureTypeEnum.FRONT_FAILURE_NOT_HAS_BEHIND_TURN.getCode() == alarmTypeDetail) {
                        recoveryCode = RecoveryCodeEnum.TERMINAL_SLOWLY.getCode();
                    }
                    // 由中断进入缓行
                    else if (!BasicCommonCacheUtils.existHash(Cache.SLOWLY_SIGN, alarmInfo1.getSwitchNo())) {
                        recoveryCode = RecoveryCodeEnum.BREAK_SLOWLY.getCode();
                    }
                    // 连续故障缓行
                    else {
                        recoveryCode = RecoveryCodeEnum.SLOWLY_ADJUST.getCode();
                    }
                    slowlyStartTime = DateUtil.getTimeStamp(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss.SSS");
                    // 60分钟时间设置
                    Integer switchFailureTime = Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.SWITCH_FAILURE_THIRTY_PRRVIEW_TIME));
                    slowlyEndTime = DateUtil.getTimeStamp(System.currentTimeMillis() + switchFailureTime, "yyyy-MM-dd" + " HH:mm:ss.SSS");
                    compressOldTrainGraphString = oldPlanGraph;
                }
                break;
            // 中断恢复
            case -2:
                // 晚点
                if (autoReport) {
                    recoveryCode = RecoveryCodeEnum.LATE_SWITCH.getCode();
                } else {
                    recoveryCode = RecoveryCodeEnum.BREAK_RECOVERY.getCode();
                }
                compressOldTrainGraphString = oldPlanGraph;
                break;
            // 缓行恢复
            case 4:
            case -3:
                recoveryCode = RecoveryCodeEnum.SLOWLY_RECOVERY.getCode();
                compressOldTrainGraphString = oldPlanGraph;
                break;
            // 晚点
            case -1:
                recoveryCode = RecoveryCodeEnum.LATE_SWITCH.getCode();
                break;
            default:
        }
        String protectSwitchNo = alarmInfo1.getProtectSwitchNo();
        List<String> protectSwitchNoList = Lists.newArrayList();
        protectSwitchNoList.add(protectSwitchNo);
        algorithmData = new SwitchFailureAlgorithmData(zipPlanRunGraph, alarmInfo1, depotInfo, tiastraceInfoList, compressOldTrainGraphString, recoveryCode, slowlyStartTime, 0, 0, slowlyEndTime, protectSwitchNoList);
        algorithmData.setCaseCode(alarmInfo1.getCaseCode());
        algorithmData.setNowTime(DateUtil.getTimeStamp(System.currentTimeMillis()));
        algorithmData.setHoldTrainList(getHoldTrains(alarmInfo1.getTableInfoId()));
        return algorithmData;
    }

    private AlgorithmData getAxleCounterAlgData(AlarmInfo alarmInfo1, String zipPlanRunGraph, List<DepotInfoDto> depotInfo, List<TiasTraceInfo> tiastraceInfoList, String planGraph) {
        AxleCounterAlgorithmData algorithmData = new AxleCounterAlgorithmData(zipPlanRunGraph, alarmInfo1, depotInfo, tiastraceInfoList, planGraph, 0, "", "", 0);
        setAxleCounterAlgData(algorithmData, alarmInfo1);
        algorithmData.setCaseCode(alarmInfo1.getCaseCode());
        algorithmData.setHoldTrainList(getHoldTrains(alarmInfo1.getTableInfoId()));
        return algorithmData;
    }

    /**
     * 设置计轴算法参数时间
     *
     * @param axleCounterAlgorithmData
     * @param alarmInfo
     */
    private void setAxleCounterAlgData(AxleCounterAlgorithmData axleCounterAlgorithmData, AlarmInfo alarmInfo) {
        String alarmTypeDetail = String.valueOf(alarmInfo.getAlarmTypeDetail());
        if (alarmTypeDetail.equals(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH) ||
                alarmTypeDetail.equals(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH_RESET) ||
                alarmTypeDetail.equals(AlarmTypeConstant.AXLE_COUNTER_SWITCH_RESET)) {
            setAxleCounterAlgorithmTimeNotSwitch(alarmInfo.getExecuteStep(), axleCounterAlgorithmData, alarmInfo);
        }
        if (alarmTypeDetail.equals(AlarmTypeConstant.AXLE_COUNTER_SWITCH)) {
            setAxleCounterAlgorithmTimeSwitch(alarmInfo.getExecuteStep(), axleCounterAlgorithmData, alarmInfo);
        }
    }


    //设置道岔区段计轴故障算法参数时间
    private void setAxleCounterAlgorithmTimeSwitch(int executeStep, AxleCounterAlgorithmData axleCounterAlgorithmData, AlarmInfo alarmInfo) {
        //故障升级后取升级后的计轴
        if (BasicCommonCacheUtils.exist(Cache.ALARM_UPGRADE)) {
            if (alarmInfo.getUpgradeId() != -1) {
                AlarmInfo upgradeAlarmInfo = admAlertInfoUpgradeService.getById(alarmInfo.getUpgradeId());
                axleCounterAlgorithmData.setAxleIdList(new ArrayList<Integer>() {{
                    add(upgradeAlarmInfo.getAxleCounterId());
                }});
            }
        }
        switch (executeStep) {
            case IidsConstPool.EXECUTE_STEP_1:
            case IidsConstPool.EXECUTE_STEP_4:
                axleCounterAlgorithmData.setRecoveryCode(RecoveryCodeEnum.LATE.getCode());
                String endTimeStr;
                //时间差缓存
                if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                    endTimeStr = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                } else {
                    //获取调图冗余参数,时间毫秒
                    long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
                    endTimeStr = DateUtil.getTimeStamp(new Date().getTime() + setRedundancyTime);
                    BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endTimeStr);
                }
                alarmInfo.setEndAlarmTime(endTimeStr);
                break;
            case IidsConstPool.EXECUTE_STEP_5:
            case IidsConstPool.EXECUTE_STEP_3:
                axleCounterAlgorithmData.setRecoveryCode(RecoveryCodeEnum.SLOWLY.getCode());
                Date slowlyEndTime = new Date();
                String slowlyStartTimeStr;
                //缓行开始时间一直不变
                if (BasicCommonCacheUtils.exist(Cache.AXLE_COUNTER_SLOWLY_START)) {
                    slowlyStartTimeStr = (String) BasicCommonCacheUtils.get(Cache.AXLE_COUNTER_SLOWLY_START);
                } else {
                    //获取调图冗余参数,时间毫秒
                    long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
                    slowlyStartTimeStr = DateUtil.getTimeStamp(new Date().getTime() + setRedundancyTime);
                    BasicCommonCacheUtils.set(Cache.AXLE_COUNTER_SLOWLY_START, slowlyStartTimeStr);
                }
                //时间差缓存
                if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                    slowlyStartTimeStr = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                } else {
                    BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, slowlyStartTimeStr);
                }
                axleCounterAlgorithmData.setSlowlyStartTime(slowlyStartTimeStr);
                //缓行周期
                int slowlyTime = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.AXLE_COUNTER_SLOWLY_TIME));
                //缓行结束时间= 当前时间 + 缓行周期
                slowlyEndTime.setTime(slowlyEndTime.getTime() + slowlyTime);
                axleCounterAlgorithmData.setSlowlyEndTime(DateUtil.getStringToDate(slowlyEndTime, "yyyy-MM-dd HH:mm:ss.SSS"));
                alarmInfo.setEndAlarmTime(DateUtil.getStringToDate(slowlyEndTime, "yyyy-MM-dd HH:mm:ss.SSS"));
                break;
            case IidsConstPool.EXECUTE_STEP_0_2:
                axleCounterAlgorithmData.setRecoveryCode(RecoveryCodeEnum.LATE.getCode());
                String endTimeStr1;
                //时间差缓存
                if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                    endTimeStr1 = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                } else {
                    //获取调图冗余参数,时间毫秒
                    long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
                    endTimeStr1 = DateUtil.getTimeStamp(new Date().getTime() + setRedundancyTime);
                    BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endTimeStr1);
                }
                alarmInfo.setEndAlarmTime(endTimeStr1);
                break;
            case IidsConstPool.EXECUTE_STEP_0_3:
                axleCounterAlgorithmData.setRecoveryCode(RecoveryCodeEnum.SLOWLY_RECOVERY.getCode());
                String endTime;
                //时间差缓存
                if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                    endTime = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                } else {
                    //获取调图冗余参数,时间毫秒
                    long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
                    endTime = DateUtil.getTimeStamp(new Date().getTime() + setRedundancyTime);
                    BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endTime);
                }
                axleCounterAlgorithmData.setSlowlyEndTime(endTime);
                alarmInfo.setEndAlarmTime(endTime);
                break;
            default:
                log.info("执行步骤错误！设置算法时间失败！");
                break;
        }
    }

    //设置计轴算法参数时间
    private void setAxleCounterAlgorithmTimeNotSwitch(int executeStep, AxleCounterAlgorithmData axleCounterAlgorithmData, AlarmInfo alarmInfo) {
        switch (executeStep) {
            case IidsConstPool.EXECUTE_STEP_1:
                axleCounterAlgorithmData.setRecoveryCode(RecoveryCodeEnum.LATE.getCode());
                String endTimeStr;
                //时间差缓存
                if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                    endTimeStr = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                } else {
                    //获取调图冗余参数,时间毫秒
                    long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
                    endTimeStr = DateUtil.getTimeStamp(System.currentTimeMillis() + setRedundancyTime);
                    BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endTimeStr);
                }
                alarmInfo.setEndAlarmTime(endTimeStr);
                break;
            case IidsConstPool.EXECUTE_STEP_2:
                axleCounterAlgorithmData.setRecoveryCode(RecoveryCodeEnum.SLOWLY.getCode());
                Date slowlyStartTime = new Date();
                Date slowlyEndTime = new Date();
                String slowlyStartTimeStr;
                //缓行开始时间一直不变
                if (BasicCommonCacheUtils.exist(Cache.AXLE_COUNTER_SLOWLY_START)) {
                    slowlyStartTimeStr = (String) BasicCommonCacheUtils.get(Cache.AXLE_COUNTER_SLOWLY_START);
                } else {
                    //获取调图冗余参数,时间毫秒
                    long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
                    slowlyStartTimeStr = DateUtil.getTimeStamp(slowlyStartTime.getTime() + setRedundancyTime);
                    BasicCommonCacheUtils.set(Cache.AXLE_COUNTER_SLOWLY_START, slowlyStartTimeStr);
                }
                //时间差缓存
                if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                    slowlyStartTimeStr = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                } else {
                    BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, slowlyStartTimeStr);
                }
                axleCounterAlgorithmData.setSlowlyStartTime(slowlyStartTimeStr);
                //缓行周期
                int slowlyTime = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.AXLE_COUNTER_SLOWLY_TIME));
                //缓行结束时间= 当前时间 + 缓行周期
                slowlyEndTime.setTime(slowlyEndTime.getTime() + slowlyTime);
                axleCounterAlgorithmData.setSlowlyEndTime(DateUtil.getStringToDate(slowlyEndTime, "yyyy-MM-dd HH:mm:ss.SSS"));
                alarmInfo.setEndAlarmTime(DateUtil.getStringToDate(slowlyEndTime, "yyyy-MM-dd HH:mm:ss.SSS"));
                break;
            case IidsConstPool.EXECUTE_STEP_0_2:
                axleCounterAlgorithmData.setRecoveryCode(RecoveryCodeEnum.LATE.getCode());
                String endTimeStr1;
                //时间差缓存
                if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                    endTimeStr1 = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                } else {
                    //获取调图冗余参数,时间毫秒
                    long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
                    endTimeStr1 = DateUtil.getTimeStamp(System.currentTimeMillis() + setRedundancyTime);
                    BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endTimeStr1);
                }
                alarmInfo.setEndAlarmTime(endTimeStr1);
                break;
            case IidsConstPool.EXECUTE_STEP_0_3:
                axleCounterAlgorithmData.setRecoveryCode(RecoveryCodeEnum.SLOWLY_RECOVERY.getCode());
                String endTime;
                //时间差缓存
                if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
                    endTime = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
                } else {
                    //获取调图冗余参数,时间毫秒
                    long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
                    endTime = DateUtil.getTimeStamp(System.currentTimeMillis() + setRedundancyTime);
                    BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endTime);
                }
                axleCounterAlgorithmData.setSlowlyEndTime(endTime);
                alarmInfo.setEndAlarmTime(endTime);
                break;
            default:
                log.info("执行步骤错误！设置算法时间失败！");
                break;
        }
    }

    /**
     * 根据是否第二天更改alarmInfo中的startTime和endTime
     *
     * @param alarmInfo 故障信息
     */
    public void changeTimestampIfTomorrow(AlarmInfo alarmInfo) {
//        // 第二天标志
//        String tag = ALG_SECOND_DAY_TAG;
//
//        String startTime = SECOND_DAY_TIME;
//        //运营停运时间
//        String endTime = SysParamKit.getByCode(IidsSysParamPool.UPDATE_GRAPH_TIME);
//
//        //判断时间是否跨天来确定算法参数中的时间是否加"1."
//        if (!StringUtils.isEmpty(alarmInfo.getStartAlarmTime()) && DateUtil.isBelongPeriodTime(alarmInfo.getStartAlarmTime(), startTime, endTime)) {
//            String timeBefore = alarmInfo.getStartAlarmTime();
//            String dateToString = DateUtil.getDateToString(DateUtil.getStringToDate(timeBefore, "yyyy-MM-dd HH:mm:ss.SSS"), "HH:mm:ss.SSS");
//            alarmInfo.setStartAlarmTime(tag.concat(dateToString));
//            log.info("算法参数中开始时间跨天,原始为:{},人工处理为{}", timeBefore, alarmInfo.getStartAlarmTime());
//        }
//        if (!StringUtils.isEmpty(alarmInfo.getEndAlarmTime()) && DateUtil.isBelongPeriodTime(alarmInfo.getEndAlarmTime(), startTime, endTime)) {
//            String timeBefore = alarmInfo.getEndAlarmTime();
//            String dateToString = DateUtil.getDateToString(DateUtil.getStringToDate(timeBefore, "yyyy-MM-dd HH:mm:ss.SSS"), "HH:mm:ss.SSS");
//            alarmInfo.setEndAlarmTime(tag.concat(dateToString));
//            log.info("算法参数中结束时间跨天,原始为:{},人工处理为{}", timeBefore, alarmInfo.getEndAlarmTime());
//        }
    }

    public PrePlanRunGraph getPowerPlanRunGraph(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getPowerAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取预览运行图时,未获取算法参数");
        log.info("获取算法参数成功,准备获取新的运行图");
        //算法自检
        BaseResponse<String> baseResponse = algPowerClient.tractionPowerCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("运行图预览算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> response = algPowerClient.tractionPowerSolver(algorithmData);
        //log.info("算法返回数据:{}", JsonUtils.toJSONString(response));
        if (!response.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = response.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        return GraphDataUtil.getPrePlanRunGraph(algorithmData.getCompressTrainGraphString(), algorithmResult.getCompressTrainGraphString(), algorithmResult.getTrainNumberAdjustDtoList());
    }


    public PrePlanRunGraph getLockPlanRunGraph(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getLockAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取预览运行图时,未获取算法参数");
        log.info("获取算法参数成功,准备获取新的运行图");
        //算法自检
        BaseResponse<String> baseResponse = algPowerClient.tractionPowerCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("运行图预览算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> response = algPowerClient.tractionPowerSolver(algorithmData);
        if (!response.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = response.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        return GraphDataUtil.getPrePlanRunGraph(algorithmData.getCompressTrainGraphString(), algorithmResult.getCompressTrainGraphString(), algorithmResult.getTrainNumberAdjustDtoList());
    }


    public PrePlanRunGraph getAxleCounterPlanRunGraph(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getAxleCounterAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取预览运行图时,未获取算法参数");
        log.info("获取算法参数成功,准备获取新的运行图");
        //算法自检
        BaseResponse<String> baseResponse = algSwitchClient.switchFailureCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("运行图预览算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> response = algSwitchClient.switchFailure(algorithmData);
        if (!response.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = response.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        return GraphDataUtil.getPrePlanRunGraph(algorithmData.getCompressTrainGraphString(), algorithmResult.getCompressTrainGraphString(), algorithmResult.getTrainNumberAdjustDtoList());
    }

    /**
     * @Author yuelei
     * @Desc 车门抬车逻辑
     * @Date 11:29 2022/7/4
     */
    public void offHoldTrainTrainDoor(long infoId) {
        if (BasicCommonCacheUtils.exist(Cache.HOLD_AND_OFF_TRAIN)) {
            Set<Map.Entry<Object, Object>> set = BasicCommonCacheUtils.hmget(Cache.HOLD_AND_OFF_TRAIN, HoldOffTrainTimeDto.class).entrySet();
            for (Map.Entry<Object, Object> m : set) {
                HoldOffTrainTimeDto dto = (HoldOffTrainTimeDto) m.getValue();
                log.info("扣车信息：{}", JsonUtils.toJSONString(dto));
                //不属于当前应急事件，则不抬车
                if (infoId != dto.getTableInfoId()) {
                    log.info("非当前场景，则不抬车");
                    continue;
                }
                //未扣车，则不抬车
                if (dto.getIsHold() == 0) {
                    log.info("未扣车，则不抬车");
                    continue;
                }
                AlarmInfo alarmInfo = admAlertInfoSubService.queryOnlyByInfoId(dto.getTableInfoId());
                //到达抬车时间 开始抬车
                TrainCtrlPrv trainCtrl = alarmUtil.getTrainCtrlPrvForTrainDoor(Integer.parseInt(m.getKey().toString()), 0);
                trainCtrl.setAlarmInfoId(String.valueOf(dto.getTableInfoId()));
                if (trainCtrl.getPlatformId() == -1) {
                    BasicCommonCacheUtils.delMapKey(Cache.HOLD_AND_OFF_TRAIN, (String) m.getKey());
                    log.info("获取的站台未-1，取消抬车操作:{}", m.getKey());
                    continue;
                }
                PlatformInfo platformInfo = alarmUtil.getPlatformInfo(trainCtrl.getPlatformId());
                //推送前端通知
                NotifyParam notifyParam = new NotifyParam();
                notifyParam.setMsgPushEnum(MsgPushEnum.START_OFF_HOLD_TRAIN_MSG);
                notifyParam.setInfoId(alarmInfo.getTableInfoId());
                notifyParam.setPlatformId(trainCtrl.getPlatformId());
                sendNotifyService.sendNotify(notifyParam);
                //下发抬车指令
                AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
                atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.HOLD_TRAIN);
                atsByteCmdData.setParameter(TrainStateEnum.TRAIN_DOOR_CLOSE.getValue());
                atsByteCmdData.setT(JsonUtils.toJSONString(platformInfo));
                atsByteCmdData.setTrainCtrl(trainCtrl);
                log.info("下发抬车指令至ats");
                atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
            }
        }
        BasicCommonCacheUtils.delKey(Cache.HOLD_AND_OFF_TRAIN);
    }

    /**
     * 根据算法返回的停车区域id抬车
     *
     * @param infoId 应急事件id
     * @author liyunlong
     * @date 2022/8/26 9:30
     */
    public void offHoldTrainInStopArea(long infoId) {
        PlatformStopAreaListDto listDto = (PlatformStopAreaListDto) BasicCommonCacheUtils.get(Cache.SWITCH_FAILURE_STOP_AREA_LIST, PlatformStopAreaListDto.class);
        if (Objects.isNull(listDto) || listDto.getPlatformStopAreaList().isEmpty()) {
            log.info("未获取大小交路停车区域信息");
            return;
        }
        // 获取对应站台编号
        List<Integer> platformIdList = platformInfoService.getPlatformIdList(listDto.getPlatformStopAreaList());
        if (BasicCommonCacheUtils.exist(Cache.HOLD_AND_OFF_TRAIN)) {
            Set<Map.Entry<Object, Object>> set = BasicCommonCacheUtils.hmget(Cache.HOLD_AND_OFF_TRAIN, HoldOffTrainTimeDto.class).entrySet();
            for (Map.Entry<Object, Object> m : set) {
                String platformId = (String) m.getKey();
                HoldOffTrainTimeDto dto = (HoldOffTrainTimeDto) m.getValue();
                log.info("扣车信息：{}", JsonUtils.toJSONString(dto));
                // 抬跑小交路停车区域的车
                if (platformIdList.contains(Integer.valueOf(platformId))) {
                    // 不属于当前应急事件，则不抬车
                    if (infoId != dto.getTableInfoId()) {
                        log.info("非当前场景，则不抬车");
                        continue;
                    }
                    // 未扣车，则不抬车
                    if (dto.getIsHold() == 0) {
                        log.info("未扣车，则不抬车");
                        continue;
                    }
                    AlarmInfo alarmInfo = admAlertInfoSubService.queryOnlyByInfoId(dto.getTableInfoId());
                    // 开始抬车
                    TrainCtrlPrv trainCtrl = alarmUtil.getTrainCtrlPrvForTrainDoor(Integer.parseInt(m.getKey().toString()), 0);
                    trainCtrl.setAlarmInfoId(String.valueOf(dto.getTableInfoId()));
                    if (trainCtrl.getPlatformId() == -1) {
                        BasicCommonCacheUtils.delMapKey(Cache.HOLD_AND_OFF_TRAIN, (String) m.getKey());
                        log.info("获取的站台未-1，取消抬车操作:{}", m.getKey());
                        continue;
                    }
                    PlatformInfo platformInfo = alarmUtil.getPlatformInfo(trainCtrl.getPlatformId());
                    //推送前端通知
                    NotifyParam notifyParam = new NotifyParam();
                    notifyParam.setMsgPushEnum(MsgPushEnum.START_OFF_HOLD_TRAIN_MSG);
                    notifyParam.setInfoId(alarmInfo.getTableInfoId());
                    notifyParam.setPlatformId(trainCtrl.getPlatformId());
                    sendNotifyService.sendNotify(notifyParam);
                    //下发抬车指令
                    AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
                    atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.HOLD_TRAIN);
                    atsByteCmdData.setParameter(TrainStateEnum.TRAIN_DOOR_CLOSE.getValue());
                    atsByteCmdData.setT(JsonUtils.toJSONString(platformInfo));
                    atsByteCmdData.setTrainCtrl(trainCtrl);
                    log.info("下发抬车指令至ats");
                    atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
                    BasicCommonCacheUtils.delMapKey(Cache.HOLD_AND_OFF_TRAIN, String.valueOf(platformId));
                }
            }
        }
    }

    public void offHoldTrainCheckEnd(long infoId) {
        if (BasicCommonCacheUtils.exist(Cache.HOLD_AND_OFF_TRAIN)) {
            Set<Map.Entry<Object, Object>> set = BasicCommonCacheUtils.hmget(Cache.HOLD_AND_OFF_TRAIN, HoldOffTrainTimeDto.class).entrySet();
            for (Map.Entry<Object, Object> m : set) {
                HoldOffTrainTimeDto dto = (HoldOffTrainTimeDto) m.getValue();
                log.info("扣车信息：{}", JsonUtils.toJSONString(dto));
                //不属于当前应急事件，则不抬车
                if (infoId != dto.getTableInfoId()) {
                    log.info("非当前场景，则不抬车");
                    continue;
                }
                //未扣车，则不抬车
                if (dto.getIsHold() == 0) {
                    log.info("未扣车，则不抬车");
                    continue;
                }
                AlarmInfo alarmInfo = admAlertInfoSubService.queryOnlyByInfoId(dto.getTableInfoId());
                //到达抬车时间 开始抬车
                TrainCtrlPrv trainCtrl = alarmUtil.getTrainCtrlPrvForTrainDoor(Integer.parseInt(m.getKey().toString()), 0);
                trainCtrl.setAlarmInfoId(String.valueOf(dto.getTableInfoId()));
                if (trainCtrl.getPlatformId() == -1) {
                    BasicCommonCacheUtils.delMapKey(Cache.HOLD_AND_OFF_TRAIN, (String) m.getKey());
                    log.info("获取的站台未-1，取消抬车操作:{}", m.getKey());
                    continue;
                }
                PlatformInfo platformInfo = alarmUtil.getPlatformInfo(trainCtrl.getPlatformId());
                //推送前端通知
                NotifyParam notifyParam = new NotifyParam();
                notifyParam.setMsgPushEnum(MsgPushEnum.START_OFF_HOLD_TRAIN_MSG);
                notifyParam.setInfoId(alarmInfo.getTableInfoId());
                notifyParam.setPlatformId(trainCtrl.getPlatformId());
                sendNotifyService.sendNotify(notifyParam);
                //下发抬车指令
                AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
                atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.HOLD_TRAIN);
                atsByteCmdData.setParameter(TrainStateEnum.TRAIN_DOOR_CLOSE.getValue());
                atsByteCmdData.setT(JsonUtils.toJSONString(platformInfo));
                atsByteCmdData.setTrainCtrl(trainCtrl);
                log.info("下发抬车指令至ats");
                atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
                dto.setIsHold(0);
                BasicCommonCacheUtils.hPut(Cache.HOLD_AND_OFF_TRAIN, (String) m.getKey(), dto);
            }
        }
    }

    /*public void sendHoldTrainNotify(NotifyTypeEnum typeEnum, Integer platformId, long infoId) {
        //通知类型 NotifyTypeEnum
        NotifyEntity notify = new NotifyEntity();
        //通知编码
        notify.setCode(typeEnum.getCode());
        //通知名字
        notify.setName(typeEnum.getName());
        //通知类别
        notify.setType(typeEnum.getType());
        notify.setTableInfoId(infoId);
        //通知内容
        if (platformId == null) {
            notify.setContent(typeEnum.getDescribe());
        } else {
            notify.setPlatformId(platformId);
            if (typeEnum.equals(NotifyTypeEnum.TRAIN_DEPOT_PREPARE_TRAIN)) {
                //通知内容即为名称
                notify.setContent(typeEnum.getName());
            } else {
                notify.setContent(platformInfoService.getPlatformByPId(platformId).getName() + typeEnum.getName());
            }
        }
        NotifyLogDto dto = new NotifyLogDto(infoId, notify.getContent(), 1);
        notifyLogService.insert(dto);
        log.info("插入{}通知成功:notify:{}", typeEnum.getName(), notify);
        //时间戳
        notify.setTimestamp(DateUtil.getTimeStamp());
        appPushService.sendMessage(MsgTypeEnum.LOG_NOTICE, notify);
        LogUtil.info(log, LogUtil.SYSTEM, LogMsgTypeEnum.MSG, "通知push系统", JsonUtils.toJSONString(notify));
        //是否推送和利时开关
        String fireInfoPushSwitch = SysParamKit.getByCode(IidsSysParamPool.FIRE_INFO_PUSH_SWITCH);
        if ("1".equals(fireInfoPushSwitch)) {
            //消息异步推送和利时
            fireInfoPushService.pushFireEvent(notify, platformId);
        }
    }*/


    public PrePlanRunGraph getPlanRunGraph(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取预览运行图时,未获取算法参数");
        log.info("获取算法参数成功,准备获取新的运行图");
        //算法自检
        BaseResponse<String> baseResponse = algorithmClient.check(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("运行图预览算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> response = algorithmClient.apiSolver(algorithmData);
        if (!response.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = response.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        return GraphDataUtil.getPrePlanRunGraph(algorithmData.getCompressTrainGraphString(), algorithmResult.getCompressTrainGraphString(), algorithmResult.getTrainNumberAdjustDtoList());
    }

    public List<AlgStrategyResult> adjustRunGraphAlg(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取运行图调整方案列表时,未获取到算法参数");
        log.info("获取算法参数成功,准备获取运行图策略");
        //获取策略信息
//        BaseResponse<List<AlgStrategyResult>> response = algorithmClient.adjustStrategyUrl(algorithmData);
        BaseResponse<List<AlgStrategyResult>> response =adjustStrategyInterfaceService.adjustStrategyUrl(algorithmData);
        if (!response.getSuccess()) {
            log.error("获取运行图调整方案列表失败,失败信息：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        List<AlgStrategyResult> algorithmResult = response.getData();
        if (algorithmResult.isEmpty()) {
            log.error("请求算法服务失败,结果为空");
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        log.info("请求算法服务获取策略接口成功:{}", algorithmResult);
        algorithmResult.forEach(result -> result.setBeforeTrainGraphString(algorithmData.getCompressTrainGraphString()));
        return algorithmResult;
    }

    public List<AlgStrategyResult> adjustRunGraphSFAlg(AlarmInfo alarmInfo) {
        log.info("获取算法参数传入alarmInfo:【{}】", JsonUtils.toJSONString(alarmInfo));
        AlgorithmData algorithmData = getSFAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取运行图调整方案列表时,未获取到算法参数");
//        BaseResponse<List<AlgStrategyResult>> response = algSwitchClient.switchFailureAdjustStrategy(algorithmData);
         BaseResponse<List<AlgStrategyResult>> response =adjustStrategyInterfaceService.switchFailureAdjustStrategy(algorithmData);
        List<AlgStrategyResult> algorithmResult = response.getData();
        if (algorithmResult.isEmpty()) {
            log.error("请求算法服务失败,结果为空");
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        algorithmResult.forEach(result -> result.setBeforeTrainGraphString(algorithmData.getCompressTrainGraphString()));
        return algorithmResult;
    }


    public List<AlgStrategyResult> adjustRunGraphAxleCounterAlg(AlarmInfo alarmInfo) {
        log.info("获取算法参数传入alarmInfo:【{}】", JsonUtils.toJSONString(alarmInfo));
        AlgorithmData algorithmData = getAxleCounterAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取运行图调整方案列表时,未获取到算法参数");
        log.info("获取算法参数成功,准备获取运行图策略");
//        BaseResponse<List<AlgStrategyResult>> response = algSwitchClient.switchFailureAdjustStrategy(algorithmData);
        BaseResponse<List<AlgStrategyResult>> response = adjustStrategyInterfaceService.switchFailureAdjustStrategy(algorithmData);
        if (!response.getSuccess()) {
            log.error("获取运行图调整方案列表失败,失败信息：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        List<AlgStrategyResult> algorithmResult = response.getData();
        if (algorithmResult.isEmpty()) {
            log.error("请求算法服务失败,结果为空");
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        log.info("请求算法服务获取策略接口成功");
        algorithmResult.forEach(result -> result.setBeforeTrainGraphString(algorithmData.getCompressTrainGraphString()));
        return algorithmResult;
    }


    public List<AlgStrategyResult> adjustRunGraphPowerAlg(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getPowerAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取运行图调整方案列表时,未获取到算法参数");
        log.info("获取算法参数成功,准备获取运行图策略");
        BaseResponse<List<AlgStrategyResult>> response;
//        response = algPowerClient.tractionPowerAdjustStrategy(algorithmData);
          response=adjustStrategyInterfaceService.switchFailureAdjustStrategy(algorithmData);
        List<AlgStrategyResult> algorithmResult = response.getData();
        if (algorithmResult.isEmpty()) {
            log.error("请求算法服务失败,结果为空");
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        log.info("请求算法服务获取策略接口成功:{}", algorithmResult);
        algorithmResult.forEach(result -> result.setBeforeTrainGraphString(algorithmData.getCompressTrainGraphString()));
        return algorithmResult;
    }

    /**
     * @Author yuelei
     * @Desc 获取车门调整方案
     * @Date 16:09 2022/6/18
     */
    public List<AlgStrategyResult> adjustRunGraphDoorAlg(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getDoorFailureAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取运行图调整方案列表时,未获取到算法参数");
        log.info("获取算法参数成功,准备获取运行图策略");
        BaseResponse<List<AlgStrategyResult>> response;
//        response = algPowerClient.tractionPowerAdjustStrategy(algorithmData);
        response=adjustStrategyInterfaceService.switchFailureAdjustStrategy(algorithmData);
        List<AlgStrategyResult> algorithmResult = response.getData();
        if (algorithmResult.isEmpty()) {
            log.error("请求算法服务失败,结果为空");
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        log.info("请求算法服务获取策略接口成功:{}", algorithmResult);
        algorithmResult.forEach(result -> result.setBeforeTrainGraphString(algorithmData.getCompressTrainGraphString()));
        return algorithmResult;
    }

    /**
     * @Author yuelei
     * @Desc 车门无法关闭-缓行算法
     * @Date 16:37 2022/8/2
     */
    public List<AlgStrategyResult> adjustRunGraphDoorSlowAlg(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getDoorFailureSlowAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取运行图调整方案列表时,未获取到算法参数");
        log.info("获取算法参数成功,准备获取运行图策略");
        BaseResponse<List<AlgStrategyResult>> response;
//        response = algPowerClient.tractionPowerAdjustStrategy(algorithmData);
          response=adjustStrategyInterfaceService.switchFailureAdjustStrategy(algorithmData);
        if (!response.getSuccess()) {
            log.error("获取运行图调整方案列表失败,失败信息：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        List<AlgStrategyResult> algorithmResult = response.getData();
        if (algorithmResult.isEmpty()) {
            log.error("请求算法服务失败,结果为空");
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        log.info("请求算法服务获取策略接口成功:{}", algorithmResult);
        algorithmResult.forEach(result -> result.setBeforeTrainGraphString(algorithmData.getCompressTrainGraphString()));
        return algorithmResult;
    }


    public List<AlgStrategyResult> adjustRunGraphLockAlg(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getLockAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取运行图调整方案列表时,未获取到算法参数");
        log.info("获取算法参数成功,准备获取运行图策略");
        BaseResponse<List<AlgStrategyResult>> response;
//        response = algPowerClient.tractionPowerAdjustStrategy(algorithmData);
        response=adjustStrategyInterfaceService.switchFailureAdjustStrategy(algorithmData);
        if (!response.getSuccess()) {
            log.error("获取运行图调整方案列表失败,失败信息：{}", response.getMessage());
            if (response.getMessage().equals(ALG_FAULT_MESSAGE)) {
                appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.ERROR_MSG, "0", ALG_FAULT_MESSAGE));
            }
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        List<AlgStrategyResult> algorithmResult = response.getData();
        if (algorithmResult.isEmpty()) {
            log.error("请求算法服务失败,结果为空");
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        log.info("请求算法服务获取策略接口成功:{}", algorithmResult);
        algorithmResult.forEach(result -> result.setBeforeTrainGraphString(algorithmData.getCompressTrainGraphString()));
        return algorithmResult;
    }

    /**
     * 对alarmInfo处理后,进行算法自检.适用于扣车前的自检
     *
     * @param alarmInfo 故障信息
     * @return 算法自检结果
     */
    public BaseResponse<String> checkAlg(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        String strategy = String.valueOf(alarmInfo1.getAlarmTypeDetail());
        IPrePlanRunGraphStrategy graphDataStrategy = graphDataStrategyHashMap.get(strategy);
        if (graphDataStrategy == null) {
            log.error("未找到故障类型对应的运行图预览策略类");
            throw new BizException(CodeEnum.ALARM_TYPE_UNHANDLED);
        }
        alarmInfo1.setAlarmState(graphDataStrategy.getAlarmState(alarmInfo1));
        //先查platformId，防止alarmInfo里没存
        String platformId = alarmInfo.getPlatformId();
        alarmInfo1.setStopAreaNumber(stopRegionDataService.getStopAreaByPlatformId(Integer.parseInt(platformId)));
        AlgorithmData algorithmData = getAlgorithmData(alarmInfo1);
        Assert.notNull(algorithmData, "算法自检时,获取算法参数失败");
        //调用算法自检
        return algorithmClient.check(algorithmData);
    }


    public BaseResponse<String> powerCheckAlg(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        alarmInfo1.setAlarmState(tractionPowerStrategy.getAlarmState(alarmInfo1));

        AlgorithmData algorithmData = getPowerAlgorithmData(alarmInfo1);
        Assert.notNull(algorithmData, "接触网失电算法算法自检时,获取算法参数失败");
        //调用算法自检
        return algPowerClient.tractionPowerCheck(algorithmData);
    }

    public BaseResponse<String> trainDoorCheckAlg(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        AlgorithmData algorithmData = getDoorFailureAlgorithmData(alarmInfo1);
        Assert.notNull(algorithmData, "车门无法打开故障算法自检时,获取算法参数失败");
        //调用算法自检
        return algPowerClient.tractionPowerCheck(algorithmData);
    }

    public BaseResponse<String> lockCheckAlg(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);

        AlgorithmData algorithmData = getLockAlgorithmData(alarmInfo1);
        Assert.notNull(algorithmData, "联锁双机算法算法自检时,获取算法参数失败");
        //调用算法自检
        return algPowerClient.tractionPowerCheck(algorithmData);
    }


    public BaseResponse<String> largePassFlowCheckAlg(AlarmInfo alarmInfo) {
        LargePassFlowAlgorithmParam algorithmData = getAlgorithmDataLargePassFlow(alarmInfo);
        Assert.notNull(algorithmData, "大客流算法自检时,获取算法参数失败");
        if (log.isDebugEnabled()) {
            log.debug("大客流算法自检时故障信息:{}", JsonUtils.toJSONString(alarmInfo));
            log.debug("大客流算法自检时算法参数：{}", JsonUtils.toJSONString(algorithmData));
        }
        //调用算法自检
        return algorithmClient.largePassFlowCheck(algorithmData);
    }

    /**
     * 道岔故障算法自检
     *
     * @param alarmInfo 告警信息
     * @return com.tct.iids.restful.BaseResponse<java.lang.String>
     * @author liyunlong
     * @date 2022/1/11 10:36
     */

    public BaseResponse<String> switchFailureCheckAlg(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        int alarmType = alarmInfo1.getAlarmType();
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
        if (BasicCommonCacheUtils.existHash(Cache.SWITCH_FAILURE_TEN_TIME, alarmInfo.getSwitchNo())) {
            Long pushTime = (Long) BasicCommonCacheUtils.hGet(Cache.SWITCH_FAILURE_TEN_TIME, alarmInfo.getSwitchNo());
            // 调30分钟图
            Integer switchFailureTime =
                    Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.SWITCH_FAILURE_PRRVIEW_TIME));
            alarmInfo1.setEndAlarmTime(DateUtil.getTimeStamp(pushTime + switchFailureTime, "yyyy-MM-dd HH:mm:ss.SSS"));
        } else if (BasicCommonCacheUtils.existHash(Cache.SWITCH_FAILURE_TWENTY_TIME, alarmInfo.getSwitchNo())) {
            Long pushTime = (Long) BasicCommonCacheUtils.hGet(Cache.SWITCH_FAILURE_TWENTY_TIME,
                    alarmInfo.getSwitchNo());
            // 调60分钟图
            Integer switchFailureTime =
                    Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.SWITCH_FAILURE_THIRTY_PRRVIEW_TIME));
            alarmInfo1.setEndAlarmTime(DateUtil.getTimeStamp(pushTime + switchFailureTime, "yyyy-MM-dd HH:mm:ss.SSS"));
        } else {
            //获取运行图策略，用于预览运行图
            alarmInfo1.setEndAlarmTime(graphDataStrategy.getEndAlarmTime(alarmInfo));
        }
        AlgorithmData algorithmData = getSFAlgorithmData(alarmInfo1);
        Assert.notNull(algorithmData, "算法自检时,获取算法参数失败");
        if (log.isDebugEnabled()) {
            log.debug("算法自检时故障信息:{}", JsonUtils.toJSONString(alarmInfo1));
            log.debug("算法自检时算法参数：{}", JsonUtils.toJSONString(algorithmData));
        }
        //调用算法自检
        BaseResponse<String> stringBaseResponse = algSwitchClient.switchFailureCheck(algorithmData);
        if (!stringBaseResponse.getSuccess()) {
            if (stringBaseResponse.getMessage().equals(ALG_FAULT_MESSAGE)) {
                appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.ERROR_MSG, "0", ALG_FAULT_MESSAGE));
            }
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        return stringBaseResponse;
    }


    public boolean changeGraphLargePassFlow(AlarmInfo alarmInfo) {
        LargePassFlowAlgorithmParam algorithmData = getAlgorithmDataLargePassFlow(alarmInfo);
        Assert.notNull(algorithmData, "大客流调整运行图时,获取算法参数失败");
        //算法自检
        BaseResponse<String> baseResponse = algorithmClient.largePassFlowCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            log.info("大客流算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("大客流算法自检通过:{}", JsonUtils.toJSONString(baseResponse));
        }

        log.info("获取算法参数成功,准备调整大客流运行图");

        //调用运行图更新
        BaseResponse<AlgorithmResult> result = algorithmClient.largePassFlowGraphUrl(algorithmData);
        if (!result.getSuccess()) {
            log.error("准备调整大客流运行图，调用算法异常：{}", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = result.getData();
        if (algorithmResult == null) {
            log.error("调整大客流运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        List<TrainNumberAdjust> trainNumberAdjusts = tiasDataConvert.dtosToEntitys(algorithmResult.getTrainNumberAdjustDtoList());
        //新建车次调整对象
        List<TrainNumberAdjust> trainNumberAdjustNew = new ArrayList<>();
        for (TrainNumberAdjust trainNumberAdjust : trainNumberAdjusts) {
            trainNumberAdjust.setId(UidGeneratorUtils.getUID());
            trainNumberAdjust.setTAdmAlertInfoId(alarmInfo.getTableInfoId());
            log.info("大客流加开信息:{}", JsonUtils.toJSONString(trainNumberAdjust));
            //TODO 加开车次回库时间,存储至redis,下次加开时,需要判断是否回库
            String trainOrder = trainNumberAdjust.getTrainOrder();
            String largePassengerFlowBackTime = trainNumberAdjust.getLargePassengerFlowBackTime();
            //TODO 计算过期时间
            String key = String.format(Cache.LARGE_PASS_FLOW_BACK_TIME_CACHE, trainOrder);
            if (!StringUtils.isEmpty(largePassengerFlowBackTime)) {
                long expireTime = getExpireTime(largePassengerFlowBackTime);
                BasicCommonCacheUtils.set(key, largePassengerFlowBackTime, expireTime, TimeUnit.MILLISECONDS);
            }
            trainNumberAdjustNew.add(trainNumberAdjust);
        }
        BasicCommonCacheUtils.hPut(Cache.TRAIN_NUMBER_ADJUST, String.valueOf(alarmInfo.getTableInfoId()), trainNumberAdjustNew, 10 * 60 * 60L, TimeUnit.SECONDS);
        //压缩运行图
        String zipGraph = algorithmResult.getCompressTrainGraphString();
        //入口
        planRunGraphService.updatePlanRunGraph(zipGraph);
        log.info("调整计划运行图加载ATS数据库成功");
        //推送前端通知
        NotifyParam notifyParam = new NotifyParam();
        notifyParam.setMsgPushEnum(MsgPushEnum.SEND_CHANGE_TRAIN_GRAPH_MSG);
        notifyParam.setInfoId(alarmInfo.getTableInfoId());
        sendNotifyService.sendNotify(notifyParam);
        //向tias系统发送 更新运行图指令
        AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
        TrainCtrlPrv trainCtrlPrv = new TrainCtrlPrv();
        trainCtrlPrv.setAlarmInfoId(String.valueOf(alarmInfo.getTableInfoId()));
        atsByteCmdData.setTrainCtrl(trainCtrlPrv);
        atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.CHANGE_TRAIN_GRAPH);
        atsByteCmdData.setParameter(TrainStateEnum.CHANGE_GRAPH.getValue());
        log.info("下发更新运行图指令至ats");
        atsByteCommandFlatClient.sendCommandTrain(atsByteCmdData);
        //大客流调整运行图时,插入运行图调整对比
        AlgorithmData algorithmData1 = new AlgorithmData();
        algorithmData1.setCompressTrainGraphString(algorithmData.getCompressTrainGraphString());
        log.info("开始处理车辆调整");
        return true;
    }

    /**
     * @param largePassengerFlowBackTime 回库时间,算法返回时若是第二天则会在时间前加1.
     * @return long
     * @Description
     * @Author zhangyinglong
     * @Date 2021/7/11 18:28
     */
    public static long getExpireTime(String largePassengerFlowBackTime) {
        // 如果为空返回0
        if (largePassengerFlowBackTime == null) {
            return 0L;
        }
        // 算法返回时若是第二天则会在时间前加1.
        LocalDateTime now = LocalDateTime.now();
        if (largePassengerFlowBackTime.contains(ALG_SECOND_DAY_TAG)) {
            now = now.plusDays(1);
            largePassengerFlowBackTime = largePassengerFlowBackTime.replace("1.", "");
        }
        String[] time = largePassengerFlowBackTime.split("\\:");
        LocalDateTime backLocalDateTime = now.withHour(Integer.parseInt(time[0])).withMinute(Integer.parseInt(time[1])).withSecond(Integer.parseInt(time[2]));
        return LocalDateTimeUtils.getTwoTimeDiffSecond(backLocalDateTime, LocalDateTime.now());
    }


    public List<AlgStrategyResult> adjustRunGraphAlgLargePassFlow(AlarmInfo alarmInfo) {
        LargePassFlowAlgorithmParam algorithmData = getAlgorithmDataLargePassFlow(alarmInfo);
        Assert.notNull(algorithmData, "获取大客流运行图调整方案列表时,获取算法参数失败");
        log.info("获取算法参数成功,准备获取运行图策略");

        if (log.isDebugEnabled()) {
            //将算法参数通过logback存入文件中，方便直接给
            logger.info(JsonUtils.toJSONString(algorithmData));
        }
        if (log.isDebugEnabled()) {
            log.debug("大客流策略信息时算法参数：{}", JsonUtils.toJSONString(algorithmData));
        }
        //获取策略信息
//        BaseResponse<List<AlgStrategyResult>> response = algorithmClient.largePassFlowStrategyUrl(algorithmData);
         BaseResponse<List<AlgStrategyResult>> response =adjustStrategyInterfaceService.largePassFlowStrategyUrl(algorithmData);
        if (!response.getSuccess()) {
            log.error("获取大客流调整后运行图和调整车次列表，调用算法异常,失败信息：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        List<AlgStrategyResult> algStrategyResultArrayList = response.getData();
        if (algStrategyResultArrayList.isEmpty()) {
            log.error("请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        } else {
            log.info("请求算法服务获取大客流策略接口成功:{}", algStrategyResultArrayList);
        }
        algStrategyResultArrayList.forEach(result -> result.setBeforeTrainGraphString(algorithmData.getCompressTrainGraphString()));
        return algStrategyResultArrayList;
    }


    public PrePlanRunGraph getPlanRunGraphLargePassFlow(AlarmInfo alarmInfo) {
        LargePassFlowAlgorithmParam algorithmData = getAlgorithmDataLargePassFlow(alarmInfo);
        Assert.notNull(algorithmData, "获取大客流预览运行图时,获取算法参数失败");
        log.info("获取算法参数成功,准备获取新的运行图");
        if (log.isDebugEnabled()) {
            log.debug("大客流时算法参数：{}", JsonUtils.toJSONString(algorithmData));
        }
        //算法自检
        BaseResponse<String> baseResponse = algorithmClient.largePassFlowCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            log.info("大客流运行图预览算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("大客流算法自检通过:{}", JsonUtils.toJSONString(baseResponse));
        }

        //获取新的运行图
        BaseResponse<AlgorithmResult> response = algorithmClient.largePassFlowGraphUrl(algorithmData);
        if (!response.getSuccess()) {
            log.error("获取大客流调整后运行图和调整车次列表，调用算法异常,失败信息：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = response.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        return GraphDataUtil.getPrePlanRunGraph(algorithmData.getCompressTrainGraphString(), algorithmResult.getCompressTrainGraphString(), algorithmResult.getTrainNumberAdjustDtoList());
    }

    public LargePassFlowAlgorithmParam getAlgorithmDataLargePassFlow(AlarmInfo alarmInfo) {
        //获取运行图
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        //查询ATS数据库中车辆段的所有备车信息
        //查询ATS数据库中车辆段的所有备车信息
        BaseResponse<DepotInfoDto> baseResponse = depotDataClient.getSpareCar();
        if(!baseResponse.getSuccess()){
            throw new BizException(CodeEnum.GET_BACK_CAR_FAIL);
        }
        DepotInfoDto depotInfo = baseResponse.getData();
        ConcurrentMap<String, TiasTraceInfo> map = trainTraceCache.asMap();
        if (CollectionUtils.isEmpty(map)) {
            throw new BizException("车次追踪数据不存在！");
        }
        List<TiasTraceInfo> tiastraceInfoList = new ArrayList<>(map.values());
        tiastraceInfoList.forEach(tiasTraceInfo -> tiasTraceInfo.setOrderNumber(AlarmUtil.getOrderNum(tiasTraceInfo.getOrderNumber())));
        //处理备车信息，筛选出处于车辆掉或者停车场被激活的车次
        List<DepotInfoDto> spareCar = this.getSpareCar(depotInfo, tiastraceInfoList);
        LargePassFlowAlgorithmParam algorithmData = new LargePassFlowAlgorithmParam(zipPlanRunGraph, spareCar);
        algorithmData.setCaseCode(alarmInfo.getCaseCode());
        // 根据是否第二天更新时间戳
        this.changeTimestampIfTomorrow(alarmInfo);
        algorithmData.setLargePassFlowDateTime(alarmInfo.getStartAlarmTime());
        algorithmData.setStationId(alarmInfo.getStationId());
        algorithmData.setIsUp(alarmInfo.getUpDown() == IidsConstPool.TRAIN_UP);
        return algorithmData;
    }

    /**
     * 获取终端站折返道岔故障(具备本站折返),发生故障时,停在道岔故障区间的车次信息
     *
     * @param alarmInfo 告警信息
     * @author liyunlong
     * @date 2022/8/2 9:27
     */
    public void getTrainsInfo(AlarmInfo alarmInfo) {
        // 算法自检
        BaseResponse<String> baseResponse = this.switchFailureCheckAlg(alarmInfo);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            AuxiliaryDecision auxiliaryDecision = new AuxiliaryDecision();
            auxiliaryDecision.setTableInfoId(alarmInfo.getTableInfoId());
            waiveAidDecisionAdmHandler.handle(auxiliaryDecision);
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        }
        AlgorithmData algorithmData = this.getSFAlgorithmData(alarmInfo);
        AlarmInfo info = algorithmData.getAlarmInfo();
        // 道岔故障扣车15分钟
        int switchFailureHoldTime = NumConstant.FIFTEEN;
        Date startAlarmTime = DateUtil.getStringToDate(alarmInfo.getStartAlarmTime(), "yyyy-MM-dd HH:mm:ss.SSS");
        startAlarmTime.setTime(startAlarmTime.getTime() + switchFailureHoldTime);
        info.setEndAlarmTime(DateUtil.getStringToDate(startAlarmTime, "yyyy-MM-dd HH:mm:ss.SSS"));
        algorithmData.setAlarmInfo(info);
        // 扣车时，打印算法入参
        BaseResponse<FolloweTrainsDto> result = algSwitchClient.switchFailureGetFollowingTrains(algorithmData);
        if (!result.getSuccess()) {
            if (result.getMessage().equals(ALG_FAULT_MESSAGE)) {
                appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.ERROR_MSG, "0", ALG_FAULT_MESSAGE));
            }
            AuxiliaryDecision auxiliaryDecision = new AuxiliaryDecision();
            auxiliaryDecision.setTableInfoId(alarmInfo.getTableInfoId());
            waiveAidDecisionAdmHandler.handle(auxiliaryDecision);
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        FolloweTrainsDto followeTrainsDto = result.getData();
        if (followeTrainsDto == null) {
            log.error("获取故障区车次时信息失败,算法结果为空,算法返回异常信息:【{}】", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        } else {
            log.info("获取故障区车次信息:{}", followeTrainsDto);
            List<String> trainNumberList = followeTrainsDto.getTrainNumberList();
            Integer nextStopRegionId = followeTrainsDto.getNextStopRegionId();
            if (!CollectionUtils.isEmpty(trainNumberList) && !Objects.isNull(nextStopRegionId)) {
                log.debug("道岔故障-具备本站折返区间列车行驶至车站数据。车次信息:【{}】,下一站停车区域id:【{}】", trainNumberList, nextStopRegionId);
                BasicCommonCacheUtils.hPut(Cache.TRAIN_NUMBER_LIST, alarmInfo.getSwitchNo(), trainNumberList);
                BasicCommonCacheUtils.hPut(Cache.NEXT_STOP_REGION_ID, alarmInfo.getSwitchNo(), nextStopRegionId);
            }
        }
    }

    public BaseResponse<String> axleCounterCheckAlg(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        AlgorithmData algorithmData = getAxleCounterAlgorithmData(alarmInfo1);
        Assert.notNull(algorithmData, "算法自检时,获取算法参数失败");
        if (log.isDebugEnabled()) {
            log.debug("算法自检时故障信息:{}", JsonUtils.toJSONString(alarmInfo1));
        }
        //调用算法自检
        return algSwitchClient.switchFailureCheck(algorithmData);
    }


    public void sendHoldTrainForPower(AlarmInfo alarmInfo, List<Integer> stopAreaIdList) {
        //算法自检
        BaseResponse<String> baseResponse = this.powerCheckAlg(alarmInfo);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        log.info("扣车时传入告警参数为：{}", JsonUtils.toJSONString(alarmInfo));
        AlgorithmData algorithmData = this.getPowerAlgorithmData(alarmInfo);
        // 扣车时，打印算法入参
        if (log.isDebugEnabled()) {
            log.debug("扣车时算法返回参数：{}", JsonUtils.toJSONString(algorithmData));
        }
        if (algorithmData == null) {
            throw new BizException("未获取到算法参数");
        }
        log.info("获取算法参数成功,准备获取受影响车次、扣车站台、站台抬车时间信息");
        BaseResponse<FolloweTrainsDto> result = algPowerClient.getFollowingTrainsForPower(algorithmData);
        FolloweTrainsDto followeTrainsDto = result.getData();
        if (followeTrainsDto == null) {
            log.error("获取受影响车次、扣车站台、站台抬车时间信息失败,算法结果为空,算法返回异常信息:【{}】", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        } else {
            log.info("获取受影响车次、扣车站台、站台抬车时间信息:{}", followeTrainsDto);
        }
        //获取受影响车次列表
        List<Integer> adjustTrainNumbers = followeTrainsDto.getAdjustTrainNumbers();
        log.info("受影响车次列表: " + Arrays.toString(adjustTrainNumbers.toArray()));
        //获取受影响车站列表
        List<Integer> stationIdList = this.getStationList(followeTrainsDto, stopAreaIdList);
        log.info("======故障类型:{},车次号:{},受影响车站列表:{}", alarmInfo.getAlarmTypeDetail(), alarmInfo.getOrderNum(), stationIdList.toArray());
        log.info("扣车站台停车区域Id: " + Arrays.toString(stationIdList.toArray()));
        // 扣车车次与时间map
        Map<Integer, String> downStationTime = this.getDownStationTime(followeTrainsDto, stopAreaIdList);
        log.info("接触网失电扣车关系映射:{}", downStationTime);
        // 抬车车次与时间map
        Map<Integer, String> upStationTime = this.getUpStationTime(followeTrainsDto, stopAreaIdList);
        log.info("接触网失电抬车关系映射:{}", upStationTime);
        //存入扣车时间和抬车时间
        for (Map.Entry<Integer, String> m : downStationTime.entrySet()) {
            BasicCommonCacheUtils.hPut(Cache.HOLD_AND_OFF_TRAIN_TIME, String.valueOf(m.getKey()), new HoldOffTrainTimeDto(alarmInfo.getTableInfoId(), m.getValue(), upStationTime.get(m.getKey()), 0, false));
            log.info("停车区域:" + m.getKey() + " 扣车时间为:" + m.getValue() + "抬车时间为；" + upStationTime.get(m.getKey()));
        }
    }

    /***
     * @Author yuelei
     * @Desc 获取中断折返点停车区域ID
     * @Date 16:04 2022/7/19
     */
    public List<Integer> sendHoldTrainForDoorTrain(AlarmInfo alarmInfo) {
        //算法自检
        BaseResponse<String> baseResponse = this.trainDoorCheckAlg(alarmInfo);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        log.info("扣车时传入告警参数为：{}", JsonUtils.toJSONString(alarmInfo));
        AlgorithmData algorithmData = this.getDoorFailureAlgorithmData(alarmInfo);
        // 扣车时，打印算法入参
        if (log.isDebugEnabled()) {
            log.debug("扣车时算法返回参数：{}", JsonUtils.toJSONString(algorithmData));
        }
        if (algorithmData == null) {
            throw new BizException("未获取到算法参数");
        }
        log.info("获取算法参数成功,准备获取受影响车次、扣车站台、站台抬车时间信息");
        BaseResponse<List<Integer>> result = algPowerClient.getRetraceStopArea(algorithmData);
        List<Integer> data = result.getData();
        if (data.isEmpty()) {
            throw new BizException("未获取到折返停车区域");
        }
        //获取折返点停车区域
        log.info("获取到折返点停车区域：{}", data);
        return data;
    }

    public void sendHoldTrainForLock(AlarmInfo alarmInfo, List<Integer> stopAreaIdList) {
        //算法自检
        BaseResponse<String> baseResponse = this.lockCheckAlg(alarmInfo);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        log.info("扣车时传入告警参数为：{}", JsonUtils.toJSONString(alarmInfo));
        AlgorithmData algorithmData = this.getLockAlgorithmData(alarmInfo);
        // 扣车时，打印算法入参
        if (log.isDebugEnabled()) {
            log.debug("扣车时算法返回参数：{}", JsonUtils.toJSONString(algorithmData));
        }
        if (algorithmData == null) {
            throw new BizException("未获取到算法参数");
        }
        log.info("获取算法参数成功,准备获取受影响车次、扣车站台、站台抬车时间信息");
        BaseResponse<FolloweTrainsDto> result = algPowerClient.getFollowingTrainsForPower(algorithmData);
        FolloweTrainsDto followeTrainsDto = result.getData();
        if (followeTrainsDto == null) {
            log.error("获取受影响车次、扣车站台、站台抬车时间信息失败,算法结果为空,算法返回异常信息:【{}】", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        } else {
            log.info("获取受影响车次、扣车站台、站台抬车时间信息:{}", followeTrainsDto);
        }
        //获取受影响车次列表
        List<Integer> adjustTrainNumbers = followeTrainsDto.getAdjustTrainNumbers();
        log.info("受影响车次列表: " + Arrays.toString(adjustTrainNumbers.toArray()));
        //获取受影响车站列表
        List<Integer> stationIdList = this.getStationList(followeTrainsDto, stopAreaIdList);
        log.info("======故障类型:{},车次号:{},受影响车站列表:{}", alarmInfo.getAlarmTypeDetail(), alarmInfo.getOrderNum(), stationIdList.toArray());
        log.info("扣车站台停车区域Id: " + Arrays.toString(stationIdList.toArray()));
        // 扣车车次与时间map
        Map<Integer, String> downStationTime = this.getDownStationTime(followeTrainsDto, stopAreaIdList);
        log.info("接触网失电扣车关系映射:{}", downStationTime);
        // 抬车车次与时间map
        Map<Integer, String> upStationTime = this.getUpStationTime(followeTrainsDto, stopAreaIdList);
        log.info("接触网失电抬车关系映射:{}", upStationTime);
        //存入扣车时间和抬车时间
        for (Map.Entry<Integer, String> m : downStationTime.entrySet()) {
            BasicCommonCacheUtils.hPut(Cache.HOLD_AND_OFF_TRAIN_TIME, String.valueOf(m.getKey()), new HoldOffTrainTimeDto(alarmInfo.getTableInfoId(), m.getValue(), upStationTime.get(m.getKey()), 0, false));
            log.info("停车区域:" + m.getKey() + " 扣车时间为:" + m.getValue() + "抬车时间为；" + upStationTime.get(m.getKey()));
        }
    }

    public void sendHoldTrainForSignalElec(AlarmInfo alarmInfo, List<Integer> stopAreaIdList) {
        //算法自检
        BaseResponse<String> baseResponse = this.lockCheckAlg(alarmInfo);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        log.info("扣车时传入告警参数为：{}", JsonUtils.toJSONString(alarmInfo));
        AlgorithmData algorithmData = this.getLockAlgorithmData(alarmInfo);
        // 扣车时，打印算法入参
        if (log.isDebugEnabled()) {
            log.debug("扣车时算法返回参数：{}", JsonUtils.toJSONString(algorithmData));
        }
        if (algorithmData == null) {
            throw new BizException("未获取到算法参数");
        }
        log.info("获取算法参数成功,准备获取受影响车次、扣车站台、站台抬车时间信息");
        BaseResponse<FolloweTrainsDto> result = algPowerClient.getFollowingTrainsForPower(algorithmData);
        FolloweTrainsDto followeTrainsDto = result.getData();
        if (followeTrainsDto == null) {
            log.error("获取受影响车次、扣车站台、站台抬车时间信息失败,算法结果为空,算法返回异常信息:【{}】", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        } else {
            log.info("获取受影响车次、扣车站台、站台抬车时间信息:{}", followeTrainsDto);
        }
        //获取受影响车次列表
        List<Integer> adjustTrainNumbers = followeTrainsDto.getAdjustTrainNumbers();
        log.info("受影响车次列表: " + Arrays.toString(adjustTrainNumbers.toArray()));
        //获取受影响车站列表
        List<Integer> stationIdList = this.getStationList(followeTrainsDto, stopAreaIdList);
        log.info("======故障类型:{},车次号:{},受影响车站列表:{}", alarmInfo.getAlarmTypeDetail(), alarmInfo.getOrderNum(), stationIdList.toArray());
        log.info("扣车站台停车区域Id: " + Arrays.toString(stationIdList.toArray()));
        // 扣车车次与时间map
        Map<Integer, String> downStationTime = this.getDownStationTime(followeTrainsDto, stopAreaIdList);
        log.info("接触网失电扣车关系映射:{}", downStationTime);
        // 抬车车次与时间map
        Map<Integer, String> upStationTime = this.getUpStationTime(followeTrainsDto, stopAreaIdList);
        log.info("接触网失电抬车关系映射:{}", upStationTime);
        //存入扣车时间和抬车时间
        for (Map.Entry<Integer, String> m : downStationTime.entrySet()) {
            BasicCommonCacheUtils.hPut(Cache.HOLD_AND_OFF_TRAIN_TIME, String.valueOf(m.getKey()), new HoldOffTrainTimeDto(alarmInfo.getTableInfoId(), m.getValue(), upStationTime.get(m.getKey()), 0, false));
            log.info("停车区域:" + m.getKey() + " 扣车时间为:" + m.getValue() + "抬车时间为；" + upStationTime.get(m.getKey()));
        }
    }

    /**
     * @param followeTrainsDto 扣车列表
     * @param stopAreaIdList   不扣停车区域ID集合
     * @return java.util.Map<java.lang.Integer, java.lang.String>
     * @Description 获取抬车时间
     * @Author yuelei
     * @Date 2021/9/30 9:59
     */
    private Map<Integer, String> getUpStationTime(FolloweTrainsDto followeTrainsDto, List<Integer> stopAreaIdList) {
        Map<Integer, String> upStationTime = followeTrainsDto.getUpStationTime();
        if (CollectionUtils.isEmpty(stopAreaIdList)) {
            return upStationTime;
        }
        for (Integer stopAreaId : stopAreaIdList) {
            // 通过key移除
            upStationTime.keySet().removeIf(key -> key.equals(stopAreaId));
        }
        return upStationTime;
    }

    /**
     * 获取扣车
     *
     * @param followeTrainsDto 算法结果
     * @param stopAreaIdList   不扣停车区域ID集合
     * @return 扣车map
     */
    private Map<Integer, String> getDownStationTime(FolloweTrainsDto followeTrainsDto, List<Integer> stopAreaIdList) {
        Map<Integer, String> downStationTime = followeTrainsDto.getDownStationTime();
        if (CollectionUtils.isEmpty(stopAreaIdList)) {
            return downStationTime;
        }
        for (Integer stopAreaId : stopAreaIdList) {
            // 通过key移除
            downStationTime.keySet().removeIf(key -> key.equals(stopAreaId));
        }
        return downStationTime;
    }


    /**
     * @param followeTrainsDto 扣车信息
     * @return java.util.List<java.lang.Integer>
     * @Description 获取受影响车次集合
     * @Author yuelei
     * @date 2021/9/7 14:31
     */
    private List<Integer> getStationList(FolloweTrainsDto followeTrainsDto, List<Integer> stopAreaIdList) {
        List<Integer> stationIdList = followeTrainsDto.getStationIdList();
        if (CollectionUtils.isEmpty(stopAreaIdList)) {
            return stationIdList;
        }
        stationIdList.removeAll(stopAreaIdList);
        return stationIdList;
    }

    /**
     * 放弃第一次推荐指令,适用于算法报错时
     */
    public void waiveAidDecision(AlarmInfo alarmInfo) {
        log.info("算法不能处理该故障, 进行放弃第一次推荐指令");
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 0);
        //删除redis缓存
        RedisKit.deleteADMRedisKey();
    }

    public PrePlanRunGraph getDoorPlanRunGraph(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getDoorFailureAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取预览运行图时,未获取算法参数");
        log.info("获取算法参数成功,准备获取新的运行图");
        //算法自检
        BaseResponse<String> baseResponse = algSwitchClient.switchFailureCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("运行图预览算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> response = algSwitchClient.switchFailure(algorithmData);
        if (!response.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = response.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        return GraphDataUtil.getPrePlanRunGraph(algorithmData.getCompressTrainGraphString(), algorithmResult.getCompressTrainGraphString(), algorithmResult.getTrainNumberAdjustDtoList());
    }

    public PrePlanRunGraph getDoorSlowPlanRunGraph(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getDoorFailureSlowAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取预览运行图时,未获取算法参数");
        log.info("获取算法参数成功,准备获取新的运行图");
        //算法自检
        BaseResponse<String> baseResponse = algSwitchClient.switchFailureCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("运行图预览算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> response = algSwitchClient.switchFailure(algorithmData);
        if (!response.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = response.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        return GraphDataUtil.getPrePlanRunGraph(algorithmData.getCompressTrainGraphString(), algorithmResult.getCompressTrainGraphString(), algorithmResult.getTrainNumberAdjustDtoList());
    }

    public PrePlanRunGraph getSFPlanRunGraph(AlarmInfo alarmInfo) {
        AlgorithmData algorithmData = getSFAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "获取预览运行图时,未获取算法参数");
        log.info("获取算法参数成功,准备获取新的运行图");
        //算法自检
        BaseResponse<String> baseResponse = algSwitchClient.switchFailureCheck(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                this.waiveAidDecision(alarmInfo);
            }
            log.info("运行图预览算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> response = algSwitchClient.switchFailure(algorithmData);
        if (!response.getSuccess()) {
            log.error("准备获取新的运行图，调用算法异常：{}", response.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = response.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        return GraphDataUtil.getPrePlanRunGraph(algorithmData.getCompressTrainGraphString(), algorithmResult.getCompressTrainGraphString(), algorithmResult.getTrainNumberAdjustDtoList());
    }

}
