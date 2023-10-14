package com.tct.itd.adm.msgRouter.handle;

import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.iconstant.ReplaceNameConstant;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.dto.Info;
import com.tct.itd.common.enums.AlarmSourceEnum;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description 大客流推送推荐指令
 * @Author zhoukun
 * @Date 2022/7/1 15:54
 */
@Slf4j
@Component
public class LargePassengerFlowHandler implements AlarmInfoMessageHandler {

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Resource
    private AppPushService appPushService;

    @Resource
    private AdmCommonMethodService admCommonMethodService;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        alarmFlowchartService.setExecuteFlag(alarmInfo,1);
        //推送中心或车站请求确认或通知车站录入故障的消息
        if (alarmInfo.getLargePassFlowCrowdLevel()==NumConstant.ONE){
            alarmFlowchartService.setExecuteFlag(alarmInfo,2);
        }
        if (alarmInfo.getLargePassFlowCrowdLevel()==NumConstant.TWO){
            alarmFlowchartService.setExecuteFlag(alarmInfo,3);
        }
        if (alarmInfo.getLargePassFlowCrowdLevel()==NumConstant.THREE){
            alarmFlowchartService.setExecuteFlag(alarmInfo,4);
        }
        int ifConfirmed = admCommonMethodService.pushConfirmToCenter(alarmInfo);
        if (0 == ifConfirmed) {
            return;
        }
        log.info("大客流:{}", JsonUtils.toJSONString(alarmInfo));
        int largePassFlowCrowdLevel = alarmInfo.getLargePassFlowCrowdLevel();

        //如果不设置,前端会报错
        alarmInfo.setOrderNum("-1");
        //如果不设置,前端会报错
        alarmInfo.setTrainId("-1");
        List<AidDesSubStepEntity> aidDesSubStep = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), NumConstant.ONE, largePassFlowCrowdLevel);
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(aidDesSubStep, alarmInfo);
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(1);
        Long id = alarmInfo.getTableInfoId();
        //增加大客流中心确认后记录信息需求
//        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), id, "中心车站已确认大客流发生", "0", "上报大客流");
        //赋值全局主键
        Cache.alarmInfoId = id;
        //根据故障子类型获取描述
        String alarmDescribe = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
        log.info("插入故障信息");
        //添加故障信息,生成推荐指令
        String msg = "车站:" + alarmInfo.getAlarmSite() + "发生大客流";
        alarmInfoOprtService.insertAdmAlertInfo(id, alarmDescribe, String.valueOf(alarmInfo.getAlarmTypeDetail()), alarmInfo.getAlarmSite(), msg, "推荐指令已生成", alarmInfo.getStartAlarmTime(), 0, AlarmSourceEnum.MAN_MADE.getName());
        //插入第一条告警信息 上报故障
        String message = getMessage(alarmInfo, alarmDescribe);
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), id, message, "0", "上报" + alarmDescribe);
        //插入第三条告警信息 推荐指令流程
        long detailId = UidGeneratorUtils.getUID();
        alarmInfoOprtService.insertAdmAlertDetail(detailId, id, "系统产生第一次推荐指令", "1", "第一次推荐指令");

        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = new AdmIdea(alarmInfo.getTableInfoId(), alarmInfo.getTrainId(), alarmInfo.getOrderNum(), alarmInfo.getStartAlarmTime(), alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(), alarmInfo.getAlarmTypeDetail(), stepList,
                alarmInfo.getStationId(), alarmInfo.getAlarmState(), alarmInfo.getExecuteStep());
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescribeByCode(admIdea.getAlarmType()));//设置故障类型文本信息
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));//设置故障子类型文本信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(aidDesSubStep));
        //不需要调图
        admIdea.setLargePassFlowChangeGraph(0);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推送至指定车站
        admIdea.setPushTargetStationId(alarmInfo.getStationId());
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        //插入推荐指令数据
        long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        //告警信息存入子表
        admAlertInfoSubService.insert(alarmInfo);
        log.info("推送推荐指令指令至车站:{}", JsonUtils.toJSONString(admInfo));
        //推送推荐指令至车站
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
//        //推送推荐指令至中心
//        admIdea.setPushTargetStationId(0);
//        //设置不可执行
//        admIdea.setExecuteFlag(ExecuteFlagEnum.CAN_NOT_EXECUTED.getExecuteFlag());
//
//        Info admInfoCenter = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
//        log.info("推送推荐指令指令至中心:{}", JsonUtils.toJSONString(admInfoCenter));
//        //推送推荐指令至中心
//        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfoCenter);

        //推送推荐指令至中心
        String subStepContents="";
        List<AidDesSubStepOutDto> subStepContentList=admIdea.getAidDesSubStepDtoList();
        for (AidDesSubStepOutDto a : subStepContentList) {
            subStepContents=subStepContents+a.getParam()+a.getSubStepContent()+"\n";
        }
        appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.NOTICE_POP,
                "0", subStepContents));

        if (alarmInfo.getLargePassFlowCrowdLevel()==1){
            alarmFlowchartService.setExecuteFlag(alarmInfo,5);
        }
        if (alarmInfo.getLargePassFlowCrowdLevel()==2){
            alarmFlowchartService.setExecuteFlag(alarmInfo,6);
        }
        if (alarmInfo.getLargePassFlowCrowdLevel()==3){
            alarmFlowchartService.setExecuteFlag(alarmInfo,7);
        }

    }

    public static String getMessage(AlarmInfo alarmInfo, String alarmDescribe) {
        String msg = String.format("%s发生%s", alarmInfo.getAlarmSite(), alarmInfo.getLargePassFlowCrowdLevel()+"级"+alarmDescribe);
        StringBuilder msgSB = new StringBuilder();
        msgSB.append(msg);
        if (!StringUtils.isEmpty(alarmInfo.getAlarmData())) {
            msgSB.append(",").append(alarmInfo.getAlarmData());
        }
        return msgSB.toString();
    }

    /**
     * 推送第二次推荐指令
     */
    public void pushSecondAdm(AlarmInfo alarmInfo) {
        alarmFlowchartService.setExecuteFlag(alarmInfo,11);
        List<AidDesSubStepEntity> aidDesSubStep = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), 2, 0);
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(aidDesSubStep, alarmInfo);
        //替换%s为上下行
        String upDownStr = alarmInfo.getUpDown() == IidsConstPool.TRAIN_UP ? "上" : "下";
        for (DisposeDto disposeDto : stepList) {
            disposeDto.setStep(disposeDto.getStep().replaceAll("%s", upDownStr));
        }
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(2);
        Long id = UidGeneratorUtils.getUID();
        //赋值全局主键
        Cache.alarmInfoId = id;
        //插入告警信息 推荐指令流程
        long detailId = UidGeneratorUtils.getUID();
        alarmInfoOprtService.insertAdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "系统产生第二次推荐指令", "1", "第二次推荐指令");

        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = new AdmIdea(alarmInfo.getTableInfoId(), alarmInfo.getTrainId(), alarmInfo.getOrderNum(), alarmInfo.getStartAlarmTime(), alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(), alarmInfo.getAlarmTypeDetail(), stepList,
                alarmInfo.getStationId(), alarmInfo.getAlarmState(), alarmInfo.getExecuteStep());
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescribeByCode(admIdea.getAlarmType()));//设置故障类型文本信息
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));//设置故障子类型文本信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(aidDesSubStep));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推送至指定车站
        admIdea.setPushTargetStationId(alarmInfo.getStationId());
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        //插入推荐指令数据
        long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        log.info("推送推荐指令指令至车站:{}", JsonUtils.toJSONString(admInfo));
        //推送推荐指令至车站
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);

//        //推送推荐指令至中心
//        admIdea.setPushTargetStationId(0);
//        //设置不可执行
//        admIdea.setExecuteFlag(ExecuteFlagEnum.CAN_NOT_EXECUTED.getExecuteFlag());
//        Info admInfoCenter = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
//        //推送推荐指令至中心
//        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfoCenter);

        //推送推荐指令至中心
        String subStepContents="";
        List<AidDesSubStepOutDto> subStepContentList=admIdea.getAidDesSubStepDtoList();
        for (AidDesSubStepOutDto a : subStepContentList) {
            subStepContents=subStepContents+a.getParam()+a.getSubStepContent()+"\n";
        }
        appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.NOTICE_POP,
                "0", subStepContents));
    }


    /**
     * 推送第三次推荐指令
     */
    public void pushThirdAdm(AlarmInfo alarmInfo) {
        alarmFlowchartService.setExecuteFlag(alarmInfo,12);
        List<AidDesSubStepEntity> aidDesSubStep = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), 3, 0);
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(aidDesSubStep, alarmInfo);
        //替换%s为上下行
        String upDownStr = alarmInfo.getUpDown() == IidsConstPool.TRAIN_UP ? "上" : "下";
        for (DisposeDto disposeDto : stepList) {
            disposeDto.setStep(disposeDto.getStep().replaceAll("%s", upDownStr));
            disposeDto.setStep(disposeDto.getStep().replaceAll(ReplaceNameConstant.ALARM_STATION, alarmInfo.getAlarmSite()));
        }
        //设置推荐指令步骤
        alarmInfo.setExecuteStep(3);
        Long id = UidGeneratorUtils.getUID();
        //赋值全局主键
        Cache.alarmInfoId = id;
        //插入告警信息 推荐指令流程
        long detailId = UidGeneratorUtils.getUID();
        alarmInfoOprtService.insertAdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "系统产生第三次推荐指令", "1", "第三次推荐指令");

        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = new AdmIdea(alarmInfo.getTableInfoId(), alarmInfo.getTrainId(), alarmInfo.getOrderNum(), alarmInfo.getStartAlarmTime(), alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(), alarmInfo.getAlarmTypeDetail(), stepList,
                alarmInfo.getStationId(), alarmInfo.getAlarmState(), alarmInfo.getExecuteStep());
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescribeByCode(admIdea.getAlarmType()));//设置故障类型文本信息
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));//设置故障子类型文本信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(aidDesSubStep));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        //插入推荐指令数据
        long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        admAlertInfoSubService.updateById(alarmInfo);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("推送推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
    }


    @Override
    public String channel() {
        return AlarmTypeConstant.LARGE_PASSENGER_FLOW;
    }
}
