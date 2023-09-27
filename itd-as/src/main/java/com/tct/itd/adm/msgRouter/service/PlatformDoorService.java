package com.tct.itd.adm.msgRouter.service;

import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.service.*;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.Info;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.common.enums.FlowchartFlagEnum;
import com.tct.itd.common.enums.StationConfirmEnum;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author kangyi
 * @description 单档站台门故障服务
 * @date 2021/10/20
 **/
@Slf4j
@Service
public class PlatformDoorService {

    @Resource
    private AdmStationService admStationService;

    @Resource
    private AdmAlertDetailService admAlertDetailService;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;

    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;

    @Resource
    private AdmAlertDetailBoxService admAlertDetailBoxService;

    @Resource
    private AppPushService appPushService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Resource
    private PlatformDoorUpdateFlowchartsService platformDoorUpdateFlowchartsService;

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;


    //替换占位符
    public List<AidDesSubStepEntity> replaceEntities(List<AidDesSubStepEntity> entities, AlarmInfo alarmInfo) {
        String stationName = admStationService.selectByStationId(alarmInfo.getStationId()).getStationName();
        List<AidDesSubStepEntity> retList = new ArrayList<>();
        entities.forEach(step -> {
            String subStepContent = step.getSubStepContent();
            String disCmdContent = step.getDisCmdContent();
            if (!StringUtils.isEmpty(subStepContent)) {
                step.setSubStepContent(subStepContent.replaceAll("%s站", stationName));
            }
            if (!StringUtils.isEmpty(disCmdContent)) {
                step.setDisCmdContent(disCmdContent.replaceAll("%s站", stationName));
            }
            retList.add(step);
        });
        return retList;
    }

    //单个站台门无法打开车站确认
    public void stationConfirmSingleOpen(Integer code, AlarmInfo alarmInfo) {
        long detailId = UidGeneratorUtils.getUID();
        alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp());
        AdmAlertDetail alertDetail = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令", new Date(), "系统产生第二次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis());
        admAlertDetailService.insert(alertDetail);
        int stepType = IidsConstPool.DISPOSE_STEP_TYPE_ZERO;
        //晚点时间<2min
        if (!isLate(alarmInfo)) {
            BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG, FlowchartFlagEnum.NOT_LATE.getCode());
            stepType = IidsConstPool.DISPOSE_STEP_TYPE_ONE;
        }
        //根据站台门状态获取不同的第二次推荐指令步骤
        List<AidDesSubStepEntity> entitiesOri = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.DISPOSE_STEP_TWO, stepType);
        //替换站名
        List<AidDesSubStepEntity> entities = replaceEntities(entitiesOri, alarmInfo);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        long boxId = UidGeneratorUtils.getUID();
        //推荐指令步骤执行第二步
        alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_2);
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推荐指令入库
        AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
        admAlertDetailBoxService.insert(alertDetailBox);
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        alarmInfo.setTableBoxId2(boxId);
        admAlertInfoSubService.updateById(alarmInfo);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        //更新流程图
        platformDoorUpdateFlowchartsService.SPDCOStationConfirm(alarmInfo);
    }

    //单档站台门无法关闭车站弹窗确认
    public void stationConfirmSingleClose(Integer code, AlarmInfo alarmInfo) {
        //车站选择结果存放redis
        BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.STATION_CONFIRM_FLAG, code);
        if (Objects.equals(code, StationConfirmEnum.FIRST_CONFIRM.getCode())) {
            long detailId = UidGeneratorUtils.getUID();
            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp());
            AdmAlertDetail alertDetail = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令",
                    new Date(), "系统产生第二次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis());
            admAlertDetailService.insert(alertDetail);
            int stepType = IidsConstPool.DISPOSE_STEP_TYPE_ZERO;
            //晚点时间<2min
            if (!isLate(alarmInfo)) {
                BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG, FlowchartFlagEnum.NOT_LATE.getCode());
                stepType = IidsConstPool.DISPOSE_STEP_TYPE_ONE;
            }
            //根据站台门状态获取不同的第二次推荐指令步骤
            List<AidDesSubStepEntity> entitiesOri = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.DISPOSE_STEP_TWO, stepType);
            //替换站名
            List<AidDesSubStepEntity> entities = replaceEntities(entitiesOri, alarmInfo);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第二步
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_2);
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
            admAlertDetailBoxService.insert(alertDetailBox);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            alarmInfo.setTableBoxId2(boxId);
            admAlertInfoSubService.updateById(alarmInfo);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        } else if (Objects.equals(code, StationConfirmEnum.SECOND_CONFIRM.getCode())) {
            long detailId = UidGeneratorUtils.getUID();
            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp());
            AdmAlertDetail alertDetail = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令",
                    new Date(), "系统产生第二次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis());
            admAlertDetailService.insert(alertDetail);
            int stepType = IidsConstPool.DISPOSE_STEP_TYPE_TWO;
            //根据站台门状态获取不同的第二次推荐指令步骤
            List<AidDesSubStepEntity> entitiesOri = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.DISPOSE_STEP_TWO, stepType);
            //替换站名
            List<AidDesSubStepEntity> entities = replaceEntities(entitiesOri, alarmInfo);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第二步
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_2);
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
            admAlertDetailBoxService.insert(alertDetailBox);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            alarmInfo.setTableBoxId2(boxId);
            admAlertInfoSubService.updateById(alarmInfo);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        }
        //更新流程图
        platformDoorUpdateFlowchartsService.SPDCCStationConfirm(alarmInfo, code);
    }

    //整侧站台门无法打开
    public void stationConfirmAllOpen(Integer code, AlarmInfo alarmInfo) {
        //车站选择结果存放redis
        BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.STATION_CONFIRM_FLAG, code);
        if (Objects.equals(code, StationConfirmEnum.FIRST_CONFIRM.getCode())) {
            //设置故障结束时间
            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp());
            log.info("收到车站选择故障恢复，开始推送故障恢复推荐指令！alarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
            int executeStep = IidsConstPool.EXECUTE_STEP_0_2;
            int stepType = IidsConstPool.DISPOSE_STEP_TYPE_ZERO;
            //晚点时间<2min
            if (!isLate(alarmInfo)) {
                BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG, FlowchartFlagEnum.NOT_LATE.getCode());
                stepType = IidsConstPool.DISPOSE_STEP_TYPE_ONE;
            }
            alarmInfo.setExecuteStep(executeStep);
            //根据站台门状态获取不同的第二次推荐指令步骤
            List<AidDesSubStepEntity> entitiesOri = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), executeStep, stepType);
            //替换站名
            List<AidDesSubStepEntity> entities = replaceEntities(entitiesOri, alarmInfo);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            String desc = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
            admAlertInfoSubService.updateById(alarmInfo);
            alarmInfo.setAlarmTypeDetail(Integer.parseInt(AlarmTypeConstant.ALL_PLATFORM_DOOR_CANNOT_CLOSE));
            //获取推送到前端的
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            //设置推荐指令
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            //设置标题
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            admIdea.setAlarmTypeDetailStr(desc);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            log.info("开始推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
            //推送推荐指令
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        } else if (Objects.equals(code, StationConfirmEnum.SECOND_CONFIRM.getCode())) {
            long detailId = UidGeneratorUtils.getUID();
            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp());
            AdmAlertDetail alertDetail = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令",
                    new Date(), "系统产生第二次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis());
            admAlertDetailService.insert(alertDetail);
            //根据站台门状态获取不同的第二次推荐指令步骤
            List<AidDesSubStepEntity> entitiesOri = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.DISPOSE_STEP_TWO, IidsConstPool.DISPOSE_STEP_TYPE_ZERO);
            //替换站名
            List<AidDesSubStepEntity> entities = replaceEntities(entitiesOri, alarmInfo);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第二步
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_2);
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
            admAlertDetailBoxService.insert(alertDetailBox);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            alarmInfo.setTableBoxId2(boxId);
            admAlertInfoSubService.updateById(alarmInfo);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        } else if (Objects.equals(code, StationConfirmEnum.THIRD_CONFIRM.getCode())) {
            log.info("车站选择列车能发车，开始执行抬车调图");
            BasicCommonCacheUtils.set(Cache.ALREADY_PUSH_PLATFORM_DOOR, "1");
            String endAlarmTime = DateUtil.getTimeStamp();
            alarmInfo.setEndAlarmTime(endAlarmTime);
            //晚点时间<2min 不调图直接结束生命周期
            if (!isLate(alarmInfo)) {
                //取消扣车
                AidDecSubStepHandler doorCancelHoldTrainHandler = (AidDecSubStepHandler) SpringContextUtil.getBean("doorCancelHoldTrain");
                doorCancelHoldTrainHandler.handle(alarmInfo, null);
                alarmInfo.setEndLife(false);
                BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG, FlowchartFlagEnum.NOT_LATE.getCode());
                admAlertInfoSubService.updateById(alarmInfo);
            } else {
                BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG, FlowchartFlagEnum.LATE.getCode());
                admAlertInfoSubService.updateById(alarmInfo);
                AidDecSubStepHandler sendGraphCaseHandler = (AidDecSubStepHandler) SpringContextUtil.getBean("sendGraphCase");
                sendGraphCaseHandler.handle(alarmInfo, null);
            }
        } else {
            log.info("车站选择列车不能发车，系统等待互锁解除信息");
        }
        //更新流程图
        platformDoorUpdateFlowchartsService.APDCOStationConfirm(alarmInfo, code);

    }

    //整侧站台门无法关闭车站确认
    public void stationConfirmAllClose(Integer code, AlarmInfo alarmInfo) {
        //车站选择结果存放redis
        BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.STATION_CONFIRM_FLAG, code);
        if (Objects.equals(code, StationConfirmEnum.FIRST_CONFIRM.getCode())) {
            //设置故障结束时间
            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp());
            log.info("收到车站选择故障恢复，开始推送故障恢复推荐指令！alarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
            int executeStep = IidsConstPool.EXECUTE_STEP_0_2;
            alarmInfo.setExecuteStep(executeStep);
            int stepType = IidsConstPool.DISPOSE_STEP_TYPE_ZERO;
            //晚点时间<2min
            if (!isLate(alarmInfo)) {
                BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG, FlowchartFlagEnum.NOT_LATE.getCode());
                stepType = IidsConstPool.DISPOSE_STEP_TYPE_ONE;
            }
            //根据站台门状态获取不同的第二次推荐指令步骤
            List<AidDesSubStepEntity> entitiesOri = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), executeStep, stepType);
            //替换站名
            List<AidDesSubStepEntity> entities = replaceEntities(entitiesOri, alarmInfo);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            String desc = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
            admAlertInfoSubService.updateById(alarmInfo);
            alarmInfo.setAlarmTypeDetail(Integer.parseInt(AlarmTypeConstant.ALL_PLATFORM_DOOR_CANNOT_CLOSE));
            //获取推送到前端的
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            //设置推荐指令
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            //设置标题
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            admIdea.setAlarmTypeDetailStr(desc);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            log.info("开始推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
            //推送推荐指令
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        } else if (Objects.equals(code, StationConfirmEnum.SECOND_CONFIRM.getCode())) {
            long detailId = UidGeneratorUtils.getUID();
            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp());
            AdmAlertDetail alertDetail = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令",
                    new Date(), "系统产生第二次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis());
            admAlertDetailService.insert(alertDetail);
            int stepType = IidsConstPool.DISPOSE_STEP_TYPE_ZERO;
            //晚点时间<2min
            if (!isLate(alarmInfo)) {
                BasicCommonCacheUtils.hPut(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG, FlowchartFlagEnum.NOT_LATE.getCode());
                stepType = IidsConstPool.DISPOSE_STEP_TYPE_ONE;
            }
            //根据站台门状态获取不同的第二次推荐指令步骤
            List<AidDesSubStepEntity> entitiesOri = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.DISPOSE_STEP_TWO, stepType);
            //替换站名
            List<AidDesSubStepEntity> entities = replaceEntities(entitiesOri, alarmInfo);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第二步
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_2);
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
            admAlertDetailBoxService.insert(alertDetailBox);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            alarmInfo.setTableBoxId2(boxId);
            admAlertInfoSubService.updateById(alarmInfo);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        } else if (Objects.equals(code, StationConfirmEnum.THIRD_CONFIRM.getCode())) {
            long detailId = UidGeneratorUtils.getUID();
            alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp());
            AdmAlertDetail alertDetail = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令",
                    new Date(), "系统产生第二次推荐指令", IidsConstPool.AUXILIARY_DETAIL, IidsConstPool.AUXILIARY_EXCUTE_BUTTON, System.currentTimeMillis());
            admAlertDetailService.insert(alertDetail);
            int stepType = IidsConstPool.DISPOSE_STEP_TYPE_TWO;
            //根据站台门状态获取不同的第二次推荐指令步骤
            List<AidDesSubStepEntity> entitiesOri = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.DISPOSE_STEP_TWO, stepType);
            //替换站名
            List<AidDesSubStepEntity> entities = replaceEntities(entitiesOri, alarmInfo);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第二步
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_2);
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
            admAlertDetailBoxService.insert(alertDetailBox);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            alarmInfo.setTableBoxId2(boxId);
            admAlertInfoSubService.updateById(alarmInfo);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        }
        //更新流程图
        platformDoorUpdateFlowchartsService.APDCCStationConfirm(alarmInfo, code);
    }

    //当前故障车是否晚点
    private boolean isLate(AlarmInfo alarmInfo) {
        TiasTraceInfo traceInfo = trainTraceCache.getIfPresent(alarmInfo.getTrainId());
        if (Objects.isNull(traceInfo)) {
            return false;
        }
        int otpTime = traceInfo.getOtpTime();
        int lateTime = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_LATE_TIME));
        return otpTime * 1000 > lateTime;
    }

}
