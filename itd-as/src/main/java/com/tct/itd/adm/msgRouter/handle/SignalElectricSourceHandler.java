package com.tct.itd.adm.msgRouter.handle;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.handle.failureRecovery.FailureRecoveryStrategy;
import com.tct.itd.adm.msgRouter.handle.failureRecovery.FailureRecoveryStrategyFactory;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.runGraph.stategy.SignalElectricSourceStrategy;
import com.tct.itd.adm.service.*;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.basedata.dfsread.service.base.MapLinkBaseService;
import com.tct.itd.basedata.dfsread.service.base.MapNbHoldStationBaseService;
import com.tct.itd.basedata.dfsread.service.handle.AxisInfoService;
import com.tct.itd.basedata.dfsread.service.handle.PlatformInfoService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.AxisInfoDto;
import com.tct.itd.common.dto.Info;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.constant.NumStrConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.enums.PlanRunGraphEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.hub.service.PlanRunGraphService;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.UidGeneratorUtils;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description : 信号电源故障-推送推荐指令
 * @Author : zhoukun
 * @Date : Created in 2022/4/19
 */
@Slf4j
@Component
public class SignalElectricSourceHandler implements AlarmInfoMessageHandler {

    private static final Integer ZERO_NUM = 0;
    private static final Integer ONE_NUM = 1;
    private static final Integer TWO_NUM = 2;
    private static final String ONE = "1";
    private static final String STATEMENT = "车组号:%s,表号:%s,车次号:%s,所在计轴区段:%s";
    private static final String NULL_STATEMENT = "故障发生时, 联锁区段内无列车";

    @Resource
    private AdmCommonMethodService admCommonMethodService;
    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AdmAlertDetailService admAlertDetailService;
    @Resource
    private AdmAlertDetailBoxService admAlertDetailBoxService;
    @Resource
    private AdmAlertInfoService admAlertInfoService;
    @Resource
    private PlanRunGraphService planRunGraphService;
    @Resource
    private SignalElectricSourceStrategy signalElectricSourceStrategy;
    @Resource
    private AxisInfoService axisInfoService;
    @Resource
    private PlatformInfoService platformInfoService;
    @Resource
    private AdmStationService admStationService;
    @Resource
    private MapLinkBaseService mapLinkService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource
    private MapNbHoldStationBaseService mapNbHoldStationBaseService;

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        if (0 == admCommonMethodService.pushConfirmToCenter(alarmInfo)) {
            return;
        }

        //删除调图冗余时间
        BasicCommonCacheUtils.delKey(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
        //点击解除故障按钮，推送故障恢复推荐指令
        if (alarmInfo.getAlarmState() != ONE_NUM) {
            FailureRecoveryStrategy strategy = FailureRecoveryStrategyFactory.getStrategy(alarmInfo);
            strategy.pushRecoveryAdm(alarmInfo);
            return;
        }
        alarmFlowchartService.setExecuteFlag(alarmInfo,1);
        log.info("信号电源故障故障,录入参数:{}", alarmInfo);
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_1);
        long detailId = aidDecisionExecService.doExecAidDecision(alarmInfo);
        //故障发生时将故障区段内列车存入缓存
        String key = String.format(Cache.SIGNAL_ELECTRIC_TRAIN_IN_SECTION, alarmInfo.getTableInfoId());
        BasicCommonCacheUtils.set(key, getTrainForSignalElectric(alarmInfo), 36000L, TimeUnit.SECONDS);
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), ONE_NUM, ZERO_NUM);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = new AdmIdea(alarmInfo.getTableInfoId(), alarmInfo.getTrainId(), alarmInfo.getOrderNum(), alarmInfo.getStartAlarmTime(), alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(), alarmInfo.getAlarmTypeDetail(), stepList, alarmInfo.getStationId(), alarmInfo.getAlarmState(), alarmInfo.getExecuteStep());

        //设置故障子类型文本信息,前端显示
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        //插入推荐指令数据
        long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        //设置故障结束时间
        alarmInfo.setEndAlarmTime(signalElectricSourceStrategy.getEndAlarmTime(alarmInfo));
        Integer ciId = Integer.valueOf(alarmInfo.getAlarmConStation());
        List<Integer> startPlatFormForHoldTrainList =
                mapNbHoldStationBaseService.findStartPlatFormForHoldTrain(ciId);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(startPlatFormForHoldTrainList)) {
            log.error("连锁扣车的起始扣车站台id为空,不进行连锁故障上报!");
            return;
        }
        List<String> platformIdList = Lists.newArrayList();
        for (Integer platformId : startPlatFormForHoldTrainList) {
            // 65535不处理
            if (Integer.valueOf(NumStrConstant.INVALID).equals(platformId)) {
                continue;
            }
            platformIdList.add(String.valueOf(platformId));
        }
        alarmInfo.setPlatformId(StringUtils.join(platformIdList, "/"));
        //告警信息存入子表
        admAlertInfoSubService.insert(alarmInfo);
        log.info("第一次推荐指令推送内容admInfo：{}", JsonUtils.toJSONString(admInfo));

        // 获取运行图,此时运行图是原图,用于故障中断恢复时使用
        String zipPlanRunGraph = (String)planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        BasicCommonCacheUtils.set(Cache.PLAN_GRAPH, zipPlanRunGraph);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmFlowchartService.setExecuteFlag(alarmInfo,2);
        //故障恢复预览按钮可用(只有自动恢复)
        admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), 1);
        // 显示故障续报按钮
        admAlertInfoService.updateReportById(alarmInfo.getTableInfoId(), 1);
    }

    private String getTrainForSignalElectric(AlarmInfo alarmInfo) {
        List<String> stringList = new LinkedList<>();
        Integer ciId = Integer.valueOf(alarmInfo.getAlarmConStation());
        // 从车次追踪里获取当前所有在故障区段的列车的车次号
        for (Map.Entry<String, TiasTraceInfo> entry : trainTraceCache.asMap().entrySet()) {
            TiasTraceInfo tiasTraceInfo = entry.getValue();
            // 遍历 合适条件的车 车组号+、
            // 车次追踪里逻辑区段转计轴区段, 计轴区段转link
            AxisInfoDto axisInfo = axisInfoService.getAxisInfoByAxisId(tiasTraceInfo.getPhysicsSectionType());
            Integer physicalCiId= mapLinkService.getCiIdByRtuId(axisInfo.getConStationId());
            if (ciId.equals(physicalCiId)) {
                String s1 = String.format(STATEMENT, tiasTraceInfo.getTrainId(), tiasTraceInfo.getServerNumber(), tiasTraceInfo.getOrderNumber(), axisInfo.getAxisName());
                stringList.add(s1);
            }
        }
        String s = StringUtils.join(stringList, ";");
        if ("".equals(s)) {
            s = NULL_STATEMENT;
        }
        return s;


    }

    /**
     * 推送第二次推荐指令
     */
    public void pushSecondAdm(long tableInfoId) {
        //获取存活的alarmInfo
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(tableInfoId);
        if (alarmInfo == null) {
            throw new BizException("未获取到处于生命周期的alarmInfo");
        }
        alarmFlowchartService.setExecuteFlag(alarmInfo,15);
        alarmFlowchartService.setExecuteFlag(alarmInfo,16);
        log.info("信号故障, 推送第二次推荐指令, alarmInfo参数:{}", alarmInfo);
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_2);
        long detailId = UidGeneratorUtils.getUID();
        AdmAlertDetail alertDetail = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令",
                new Date(), "系统产生运行图推荐指令", ONE, ONE_NUM, System.currentTimeMillis());
        admAlertDetailService.insert(alertDetail);
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), TWO_NUM, ZERO_NUM);
        // 组装推荐指令内容
        getAidEntities(entities, alarmInfo);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        long boxId = UidGeneratorUtils.getUID();
        ///向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推荐指令入库
        AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
        admAlertDetailBoxService.insert(alertDetailBox);
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmInfo.setTableBoxId2(boxId);
        //设置状态未执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
        // 禁用故障续报按钮
        admAlertInfoService.updateReportById(alarmInfo.getTableInfoId(), 2);
        admAlertInfoSubService.updateById(alarmInfo);
        log.info("信号电源故障,生成第二次推荐指令提示成功,alarmInfo{}", alarmInfo);
    }


    private void getAidEntities(List<AidDesSubStepEntity> entities, AlarmInfo alarmInfo) {
        AidDesSubStepEntity aidDesSubStepEntity1 = entities.get(ONE_NUM);
        AidDesSubStepEntity aidDesSubStepEntity2 = entities.get(TWO_NUM);

//        List<Integer> stationList = platformInfoService.getStationIdsByManageCi(Integer.valueOf(alarmInfo.getAlarmConStation()));
        //根据联锁id获取集中区id
        List<Integer> rtuIdList= mapLinkService.getRtuIdsByCiId(Integer.valueOf(alarmInfo.getAlarmConStation()));
        List<Integer> stationList=new ArrayList<>();
        rtuIdList.forEach(r->{
            List<Integer> stationListTemp = platformInfoService.getStationIdsByManageCi(r);
            stationListTemp.forEach(s->{
                stationList.add(s);
            });
        });

        List<String> stationNameList = stationList.stream().map((t) ->
                        admStationService.selectByStationId(t).getStationName())
                .collect(Collectors.toList());
        //第二条推荐指令
        aidDesSubStepEntity1.setSubStepContent(String.format(aidDesSubStepEntity1.getSubStepContent(), StringUtils.join(stationNameList, ",")));
        //第三条推荐指令
        String key = String.format(Cache.SIGNAL_ELECTRIC_TRAIN_IN_SECTION, alarmInfo.getTableInfoId());
        String trainInfo = (String) BasicCommonCacheUtils.get(key);
        if (trainInfo == null) {
            log.error("信号电源未获取到故障发生时区段列车缓存!");
        }
        aidDesSubStepEntity2.setSubStepContent(String.format(aidDesSubStepEntity2.getSubStepContent(), trainInfo));

        entities.set(ONE_NUM, aidDesSubStepEntity1);
        entities.set(TWO_NUM, aidDesSubStepEntity2);
    }

    @Override
    public String channel() {
        return  AlarmTypeConstant.SIGNAL_ELECTRIC;
    }
}