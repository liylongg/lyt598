package com.tct.itd.adm.msgRouter.handle;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.*;
import com.tct.itd.adm.msgRouter.executeHandle.WaiveAidDecisionAdmHandler;
import com.tct.itd.adm.msgRouter.handle.failureRecovery.FailureRecoveryStrategy;
import com.tct.itd.adm.msgRouter.handle.failureRecovery.FailureRecoveryStrategyFactory;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.runGraph.stategy.TractionPowerStrategy;
import com.tct.itd.adm.service.*;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.basedata.dfsread.service.base.MapLogicalBaseService;
import com.tct.itd.basedata.dfsread.service.base.MapStationBaseService;
import com.tct.itd.basedata.dfsread.service.base.MapTrackBaseService;
import com.tct.itd.basedata.dfsread.service.handle.LogicSectionDataService;
import com.tct.itd.basedata.dfsread.service.handle.PlatformInfoService;
import com.tct.itd.client.AlgSwitchClient;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.dto.*;
import com.tct.itd.constant.StringConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.enums.PlanRunGraphEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.hub.service.PlanRunGraphService;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * @Description : 接触网失电故障-推送推荐指令
 * @Author : zhangjiarui
 * @Date : Created in 2022/3/7
 */
@Slf4j
@Component
public class TractionPowerHandler implements AlarmInfoMessageHandler {
    /**
     * 数字常量 0
     */
    private static final int ZERO_NUM = 0;
    private static final int ONE_NUM = 1;
    private static final int TWO_NUM = 2;
    private static final int THREE_NUM = 3;
    private static final int FOUR_NUM = 4;

    private static final String ONE = "1";
    private static final String TWO = "2";
    private static final String NULL = "无电区段不存在列车";

    private static final String statement = "车组号:%s,表号:%s,车次号:%s";

    @Resource
    private AdmCommonMethodService admCommonMethodService;
    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private MapTrackBaseService mapTrackService;
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
    private TractionPowerStrategy tractionPowerStrategy;
    @Resource
    private AdmAlertInfoService admAlertInfoService;
    @Resource
    private PlanRunGraphService planRunGraphService;
    @Resource
    private MapLogicalBaseService mapLogicalService;
    @Resource
    private LogicSectionDataService logicSectionDataService;
    @Resource
    private PlatformInfoService platformInfoService;
    @Resource
    private MapStationBaseService stationBaseService;
    @Resource
    private AlgSwitchClient algSwitchClient;
    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;
    @Resource
    private WaiveAidDecisionAdmHandler waiveAidDecisionAdmHandler;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        if (0 == admCommonMethodService.pushConfirmToCenter(alarmInfo)) {
            return;
        }
        //删除调图冗余时间
        BasicCommonCacheUtils.delKey(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
        //删除是否供电设备弹框/是否有试送电成功弹框标志
        BasicCommonCacheUtils.delKey(Cache.TRACTION_POWER_POP_FLAG);
        //删除推送故障确认弹框标志（删除了之后才能继续推该弹框）
        BasicCommonCacheUtils.delKey(StringConstant.CONFIRM_FAULT_RECOVERY_POP);
        //删除试送电弹框缓存
        BasicCommonCacheUtils.delKey(Cache.TRIAL_LINE_CHARGING_FLAG);

        //点击解除故障按钮，推送故障恢复推荐指令
        if (alarmInfo.getAlarmState() != ONE_NUM) {
            FailureRecoveryStrategy strategy = FailureRecoveryStrategyFactory.getStrategy(alarmInfo);
            strategy.pushRecoveryAdm(alarmInfo);
            return;
        }
        //点亮流程图：接触网失电
        alarmFlowchartService.setExecuteFlag(alarmInfo, 1);
        //设置扣车站台集合
        setHoldTrainPlatforms(alarmInfo);
        log.info("失电故障-接触网失电故障,录入参数:{}", alarmInfo);
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(ONE_NUM);
        long detailId = aidDecisionExecService.doExecAidDecision(alarmInfo);
        //查询故障决策指令-列车进站过程中站台门打开
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), ONE_NUM, ZERO_NUM);
        // 替换推荐指令上的占位符
        this.getAidEntities(alarmInfo.getTractionSectionId(), entities);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = new AdmIdea(alarmInfo.getTableInfoId(), alarmInfo.getTrainId(), alarmInfo.getOrderNum(), alarmInfo.getStartAlarmTime(), alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(), alarmInfo.getAlarmTypeDetail(), stepList, alarmInfo.getStationId(), alarmInfo.getAlarmState(), alarmInfo.getExecuteStep());
        // 接触网失电区域字段
        admIdea.setTractionSectionId(alarmInfo.getTractionSectionId());
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescribeByCode(admIdea.getAlarmType()));
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
        alarmInfo.setEndAlarmTime(tractionPowerStrategy.getEndAlarmTime(alarmInfo));
        //告警信息存入子表
        admAlertInfoSubService.insert(alarmInfo);
        log.info("第一次推荐指令推送内容admInfo：{}", JsonUtils.toJSONString(admInfo));
        // 获取运行图,此时运行图是原图,用于道岔故障中断恢复时使用
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        BasicCommonCacheUtils.set(Cache.PLAN_GRAPH, zipPlanRunGraph);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmFlowchartService.setExecuteFlag(alarmInfo, 2);
    }


    /**
     * 获取算法返回扣车站台集合
     *
     * @author kangyi
     * @date 2022/8/24 15:06
     */
    private void setHoldTrainPlatforms(AlarmInfo alarmInfo) {
        // 获取失电区段所有逻辑区段
        List<Integer> sectionList = logicSectionDataService.getLogicIdListByTrackId(alarmInfo.getTractionSectionId());
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        AlgorithmData algorithmData = new TractionPowerAlgorithmData(zipPlanRunGraph, alarmInfo, null, null,
                null, 1, sectionList, alarmInfo.getTractionSectionId());
        algorithmData.setNowTime(DateUtil.getTimeStamp());
        BaseResponse<DetainTrainStopArea> detainTrainStopArea = algSwitchClient.getDetainTrainStopArea(algorithmData);
        if (detainTrainStopArea.getCode() != 200) {
            throw new BizException("调用算法获取扣车停车区域出错:{}", detainTrainStopArea.getMessage());
        }
        List<Integer> stopAreaIdList = detainTrainStopArea.getData().getStopAreaId();
        if (CollectionUtils.isEmpty(stopAreaIdList)) {
            log.debug("调用算法获取扣车停车区域为空");
            return;
        }
        log.info("算法返回开始扣车的停车区域id:【{}】", JsonUtils.toJSONString(stopAreaIdList));
        List<String> platformIdList = Lists.newArrayList();
        stopAreaIdList.forEach(s -> platformIdList.add(String.valueOf(platformInfoService.getPlatformIdByStopArea(s))));
        String plarformId = StringUtils.join(platformIdList, "/");
        alarmInfo.setPlatformId(plarformId);
        log.info("设置扣车站台集合plarformId{}", plarformId);
    }


    private void getAidEntities(Integer sectionId, List<AidDesSubStepEntity> entities) {
        // 替换推荐指令上的占位符
        TractionPowerDto powerDto = mapTrackService.getTractionPowerById(sectionId);
        AidDesSubStepEntity aidDesSubStepEntity0 = entities.get(ZERO_NUM);
        AidDesSubStepEntity aidDesSubStepEntity1 = entities.get(ONE_NUM);
        //第一条推荐指令
        aidDesSubStepEntity0.setSubStepContent(String.format(aidDesSubStepEntity0.getSubStepContent(), powerDto.getSectionName()));
        //第二条推荐指令
        aidDesSubStepEntity1.setSubStepContent(String.format(aidDesSubStepEntity1.getSubStepContent(), powerDto.getSectionName()));

        entities.set(ZERO_NUM, aidDesSubStepEntity0);
        entities.set(ONE_NUM, aidDesSubStepEntity1);
    }

    /**
     * 判断推送第二次推荐指令还是第三次
     *
     * @param choice 前端选择
     */
    public void judgeStep(String choice,Integer code) {
        //获取存活的alarmInfo
        AlarmInfo alarmInfo = admAlertInfoSubService.getInfoInLife();
        if (alarmInfo == null) {
            throw new BizException("未获取到处于生命周期的alarmInfo");
        }
        log.info("前端选择为{}", choice);
        //1.第一次推荐指令弹出，但未执行时，弹出确认恢复弹框
         //点击是，放弃应急事件流程，点否，继续倒计时
        if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_1
                && alarmInfo.getExecuteEnd() == ZERO_NUM){
             //是
            if (ONE.equals(choice)){
                AuxiliaryDecision auxiliaryDecision = new AuxiliaryDecision();
                auxiliaryDecision.setTableInfoId(alarmInfo.getTableInfoId());
                auxiliaryDecision.setExecuteStep(alarmInfo.getExecuteStep());
                waiveAidDecisionAdmHandler.handle(auxiliaryDecision);
                appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(MsgPushEnum.POWER_CLOSE_AID_DES_STEP_POP.getCode(),
                        "0", StringConstant.PUSH_CLOSE_AID_DES_STEP_POP));
                admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), 2);
            }
            //否
            if (TWO.equals(choice)){
                admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), 2);
            }
        }
        //2.第二次推荐指令弹出，但未执行时，弹出确认恢复弹框
         //点击是，推送推荐指令流程，点否，继续倒计时
        else if ((alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_2 ||alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_3)
                && alarmInfo.getExecuteEnd() == ZERO_NUM){
            //是
            if (ONE.equals(choice)){
                FailureRecoveryStrategy strategy = FailureRecoveryStrategyFactory.getStrategy(alarmInfo);
                //推送故障恢复
                strategy.pushRecoveryAdm(alarmInfo);
                admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), 2);
            }
            //否
            if (TWO.equals(choice)){
                admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), 2);

            }
        }
       //3. 存在是否供电设备弹框/是否试送电成功弹框时，弹出确认恢复弹框
        else if (BasicCommonCacheUtils.exist(StringConstant.CONFIRM_FAULT_RECOVERY_POP)
                 &&code.equals(MsgPushEnum.POWER_CONFIRM_RECOVER_POP.getCode())){
            //是
            if (ONE.equals(choice)){
                FailureRecoveryStrategy strategy = FailureRecoveryStrategyFactory.getStrategy(alarmInfo);
                //推送故障恢复
                strategy.pushRecoveryAdm(alarmInfo);
                admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), 2);
            }
            //否
            if (TWO.equals(choice)) {
                admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), 2);
            }
            return;
        }
        // 推送第二次推荐指令
        else if (ONE_NUM == alarmInfo.getExecuteStep()
                && alarmInfo.getExecuteEnd() == ONE_NUM
                &&code.equals(MsgPushEnum.POWER_POP_MSG.getCode())) {
            //删除是否供电设备弹框标志
            BasicCommonCacheUtils.delKey(Cache.TRACTION_POWER_POP_FLAG);
            //删除推送故障确认弹框标志（删除了之后才能继续推该弹框）
            BasicCommonCacheUtils.delKey(StringConstant.CONFIRM_FAULT_RECOVERY_POP);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 8);
            this.pushSecondAdm(choice);
        }
        // 推送第三次推荐指令
        else if (TWO_NUM == alarmInfo.getExecuteStep()
                && alarmInfo.getExecuteEnd() == ONE_NUM
                &&code.equals(MsgPushEnum.POWER_POP_MSG.getCode())) {
            //删除是否试送电成功弹框标志
            BasicCommonCacheUtils.delKey(Cache.TRACTION_POWER_POP_FLAG);
            //删除推送故障确认弹框标志（删除了之后才能继续推该弹框）
            BasicCommonCacheUtils.delKey(StringConstant.CONFIRM_FAULT_RECOVERY_POP);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 30);
            this.pushThirdAdm(choice);
        }
    }

    /**
     * 从车次追踪里获取当前所有在失电区段的列车的车次号并组装成合适格式
     *
     * @param alarmInfo 告警信息
     * @return 结果
     */
    public String getTrainForPower(AlarmInfo alarmInfo) {
        List<String> stringList = new LinkedList<>();
        TractionPowerDto powerDto = mapTrackService.getTractionPowerById(alarmInfo.getTractionSectionId());
        // 从车次追踪里获取当前所有在失电区段的列车的车次号
        for (Map.Entry<String, TiasTraceInfo> entry : trainTraceCache.asMap().entrySet()) {
            TiasTraceInfo tiasTraceInfo = entry.getValue();
            // 遍历 合适条件的车 车组号+、
            // 车次追踪里逻辑区段转link, 该link是否包含于linkList
            Integer link = mapLogicalService.getLinkIdByLogicId(tiasTraceInfo.getLogicalSectionId());

            if (powerDto.getLinkList().contains(link)) {
                String s1 = String.format(statement, tiasTraceInfo.getTrainId(), tiasTraceInfo.getServerNumber(), tiasTraceInfo.getOrderNumber());
                stringList.add(s1);
            }
        }
        return StringUtils.join(stringList, ";");
    }

    /**
     * 推送接触网失电第二次推荐指令
     */
    public void pushSecondAdm(String choice) {
        //获取存活的alarmInfo
        AlarmInfo alarmInfo = admAlertInfoSubService.getInfoInLife();
        if (alarmInfo == null) {
            throw new BizException("未获取到处于生命周期的alarmInfo");
        }
        log.info("失电故障-接触网失电故障, 推送第二次推荐指令, alarmInfo参数:{}", alarmInfo);
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(TWO_NUM);
        long detailId = UidGeneratorUtils.getUID();
        AdmAlertDetail alertDetail = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令",
                new Date(), "系统产生运行图推荐指令", ONE, ONE_NUM, System.currentTimeMillis());
        admAlertDetailService.insert(alertDetail);

        List<AidDesSubStepEntity> entities = new ArrayList<>();
        if (ONE.equals(choice)) {
            alarmFlowchartService.setExecuteFlag(alarmInfo, 9);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 11);
            // 是供电设备故障
            entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), TWO_NUM, ZERO_NUM);
            String s = this.getTrainForPower(alarmInfo);
            if ("".equals(s)) {
                s = NULL;
            } else {
                s = "建议通知列车:".concat(s);
            }
            //处理第一条推荐指令
            AidDesSubStepEntity aidDesSubStepEntity0 = entities.get(ZERO_NUM);
            aidDesSubStepEntity0.setSubStepContent(String.format(aidDesSubStepEntity0.getSubStepContent(), s));
            entities.set(ZERO_NUM, aidDesSubStepEntity0);
            //处理第三条推荐指令
            AidDesSubStepEntity aidDesSubStepEntity2 = entities.get(TWO_NUM);
            aidDesSubStepEntity2.setSubStepContent(
                    aidDesSubStepEntity2.getSubStepContent().replaceAll(ReplaceNameConstant.TRACTION_SECTION_NAME, alarmInfo.getAlarmSite()));
            entities.set(TWO_NUM, aidDesSubStepEntity2);
        }
        if (TWO.equals(choice)) {
            // 非供电设备故障
            alarmFlowchartService.setExecuteFlag(alarmInfo, 10);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 12);
            entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), TWO_NUM, ONE_NUM);
            String s = this.getTrainForPower(alarmInfo);
            if ("".equals(s)) {
                s = NULL;
            } else {
                s = "建议通知列车:".concat(s);
            }
            AidDesSubStepEntity aidDesSubStepEntity0 = entities.get(TWO_NUM);
            aidDesSubStepEntity0.setSubStepContent(String.format(aidDesSubStepEntity0.getSubStepContent(), s));
            entities.set(TWO_NUM, aidDesSubStepEntity0);
            //非供电设备设置一个标识，判断调完图后推送是否试送电弹窗
            BasicCommonCacheUtils.set(Cache.TRIAL_LINE_CHARGING_FLAG, "1");
        }
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        long boxId = UidGeneratorUtils.getUID();
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        // 接触网失电区域字段
        admIdea.setTractionSectionId(alarmInfo.getTractionSectionId());
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推荐指令入库
        AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
        admAlertDetailBoxService.insert(alertDetailBox);
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmInfo.setTableBoxId2(boxId);
        //设置状态未执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());

        admAlertInfoSubService.updateById(alarmInfo);
        log.info("接触网失电,生成第二次推荐指令提示成功,alarmInfo{}", alarmInfo);
    }

    /**
     * 推送接触网失电第三次推荐指令
     */
    public void pushThirdAdm(String choice) {
        //获取存活的alarmInfo
        AlarmInfo alarmInfo = admAlertInfoSubService.getInfoInLife();
        if (alarmInfo == null) {
            throw new BizException("未获取到处于生命周期的alarmInfo");
        }
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        log.info("失电故障-接触网失电故障, 推送第三次推荐指令, alarmInfo参数:{}", alarmInfo);
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(THREE_NUM);
        long detailId = UidGeneratorUtils.getUID();
        AdmAlertDetail alertDetail = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第三次推荐指令",
                new Date(), "系统产生第三次推荐指令", ONE, ONE_NUM, System.currentTimeMillis());
        admAlertDetailService.insert(alertDetail);
        List<AidDesSubStepEntity> entities = new ArrayList<>();
        TractionPowerDto powerDto = mapTrackService.getTractionPowerById(alarmInfo.getTractionSectionId());
        if (ONE.equals(choice)) {
            // 试送电成功
            alarmFlowchartService.setExecuteFlag(alarmInfo, 31);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 33);
            entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), THREE_NUM, ZERO_NUM);

            AidDesSubStepEntity aidDesSubStepEntity3 = entities.get(TWO_NUM);
            //第三条推荐指令
            aidDesSubStepEntity3.setSubStepContent(String.format(aidDesSubStepEntity3.getSubStepContent(), powerDto.getSectionName()));
            entities.set(TWO_NUM, aidDesSubStepEntity3);
        }
        if (TWO.equals(choice)) {
            // 试送电不成功
            alarmFlowchartService.setExecuteFlag(alarmInfo, 32);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 42);
            entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), THREE_NUM, ONE_NUM);
            // 第一条推荐指令
            AidDesSubStepEntity aidDesSubStepEntity0 = entities.get(ZERO_NUM);
            // 获取当前在无电区段的列车
            String trainForPower = this.getTrainForPower(alarmInfo);
            if ("".equals(trainForPower)) {
                trainForPower = NULL;
            }
            List<String> names = new ArrayList<>();
            List<Integer> stations = powerDto.getStationIdList();
            for (Integer station : stations) {
                names.add(stationBaseService.getStaionNameByStaionId(station));
            }
            // 拼接可变字符
            String s = String.format(aidDesSubStepEntity0.getSubStepContent(), StringUtils.join(names, ","), trainForPower);
            aidDesSubStepEntity0.setSubStepContent(s);
            entities.set(ZERO_NUM, aidDesSubStepEntity0);
        }
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        long boxId = UidGeneratorUtils.getUID();
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        // 接触网失电区域字段
        admIdea.setTractionSectionId(alarmInfo.getTractionSectionId());
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推荐指令入库
        AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
        admAlertDetailBoxService.insert(alertDetailBox);
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmInfo.setTableBoxId2(boxId);
        //设置状态未执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());

        admAlertInfoSubService.updateById(alarmInfo);
        log.info("接触网失电,生成第三次推荐指令提示成功,alarmInfo{}", alarmInfo);
    }

    /**
     * 推送接触网失电第四次推荐指令
     *
     * @param infoId infoId
     */
    public void pushFourthAdm(Long infoId) {
        //获取存活的alarmInfo
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        if (alarmInfo == null) {
            throw new BizException("未获取到处于生命周期的alarmInfo");
        }
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        log.info("失电故障-接触网失电故障, 推送第四次推荐指令, alarmInfo参数:{}", alarmInfo);
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(FOUR_NUM);
        long detailId = UidGeneratorUtils.getUID();
        AdmAlertDetail alertDetail = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第四次推荐指令",
                new Date(), "系统产生运行图推荐指令", ONE, ONE_NUM, System.currentTimeMillis());
        admAlertDetailService.insert(alertDetail);
        //获取并拼接推荐指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), FOUR_NUM, ZERO_NUM);
        String s = this.getTrainForPower(alarmInfo);
        if ("".equals(s)) {
            s = NULL;
        }
        AidDesSubStepEntity aidDesSubStepEntity0 = entities.get(ZERO_NUM);
        aidDesSubStepEntity0.setSubStepContent(String.format(aidDesSubStepEntity0.getSubStepContent(), s));
        entities.set(ZERO_NUM, aidDesSubStepEntity0);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        long boxId = UidGeneratorUtils.getUID();
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        // 接触网失电区域字段
        admIdea.setTractionSectionId(alarmInfo.getTractionSectionId());
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推荐指令入库
        AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
        admAlertDetailBoxService.insert(alertDetailBox);
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmInfo.setTableBoxId2(boxId);
        //设置状态未执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());

        admAlertInfoSubService.updateById(alarmInfo);
        log.info("接触网失电,生成第二次推荐指令提示成功,alarmInfo{}", alarmInfo);
        //置灰故障恢复预览按钮
        admAlertInfoService.updateAllowFailoverById(infoId, 2);
//        // 续报按钮置灰
//        admAlertInfoService.updateReportById(infoId, TWO_NUM);
    }

    @Override
    public String channel() {
        return  AlarmTypeConstant.TRACTION_POWER;
    }
}