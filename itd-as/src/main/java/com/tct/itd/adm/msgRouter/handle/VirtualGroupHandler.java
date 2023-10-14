package com.tct.itd.adm.msgRouter.handle;

import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AidDesSubStepEntity;

import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.handle.failureRecovery.FailureRecoveryStrategy;
import com.tct.itd.adm.msgRouter.handle.failureRecovery.FailureRecoveryStrategyFactory;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.dto.AdmNoticeDto;
import com.tct.itd.common.enums.AlarmSourceEnum;

import com.tct.itd.common.enums.CommandEnum;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.model.Info;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.SpringContextUtil;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @Description: 虚拟编组推送推荐指令
 * @Author: wangsijin
 * @Date: 2023/2/7 007 15:56
 */

@Slf4j
@Component
public class VirtualGroupHandler implements AlarmInfoMessageHandler {

    private static final String ONE = "1";
    private static final String TWO = "2";

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource
    
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AppPushService appPushService;

    @Resource
    private AdmAlertInfoService admAlertInfoService;


    @Override
    public void handle(AlarmInfo alarmInfo) {
        int executeStep = alarmInfo.getExecuteStep();
        switch (executeStep){
            case IidsConstPool.EXECUTE_STEP_99:
                log.info("虚拟编组执行故障信息推送步骤");
                sendFaultMessage(alarmInfo);
                break;
            case IidsConstPool.EXECUTE_STEP_1:
                log.info("虚拟编组执行推荐指令推送步骤");
                sendAidDesSubStep(alarmInfo);
                break;
            default:
                log.error("虚拟编组的推荐指令执行步骤错误，请查看数据是否正确——{}", JsonUtils.toJSONString(alarmInfo));
        }
    }

    //推送故障信息
    public void sendFaultMessage(AlarmInfo alarmInfo){
        alarmFlowchartService.setExecuteFlag(alarmInfo, 1);
        log.info("虚拟编组推送故障信息,录入参数:{}", alarmInfo);
        //推荐指令步骤
        Long id = alarmInfo.getTableInfoId();
        //赋值全局主键
        Cache.alarmInfoId = id;

        //根据故障子类型获取描述
        String alarmDescribe = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
        log.info("插入故障信息");
        //是否允许故障恢复
        int allowFailover = AidDecisionExecService.getAllowFailover(alarmInfo.getAlarmTypeDetail());
        //报警源
        //未设值默认人工
        String source = alarmInfo.getSource() == null ? AlarmSourceEnum.MAN_MADE.getName() : AlarmSourceEnum.getNameByType(alarmInfo.getSource());
        //添加故障信息,故障信息已生成
        alarmInfoOprtService.insertAdmAlertInfo(id, alarmDescribe, String.valueOf(alarmInfo.getAlarmTypeDetail()), alarmInfo.getAlarmSite(), "此处按照需求直接写死即可", "故障信息已生成", alarmInfo.getStartAlarmTime(), allowFailover, source);
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), id,"此处按照需求直接写死即可", "0", "上报" + alarmDescribe, alarmInfo.getStartAlarmTime());
        // 插入第三条告警信息 推荐指令流程
        long detailId = UidGeneratorUtils.getUID();
        alarmInfoOprtService.insertAdmAlertDetail(detailId, id, "系统产生故障信息", "1", "故障信息内容");
        //告警信息存入子表
        admAlertInfoSubService.insert(alarmInfo);
        log.info("推送故障信息至中心");
        // 获取故障子类型的故障名，并添加到对象中
        AdmNoticeDto admNoticeDto = new AdmNoticeDto(alarmInfo,
                admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
        // 推送故障信息
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.ADM_INSERT_ALARM_NOTICE,
                        "0", admNoticeDto));
        alarmFlowchartService.setExecuteFlag(alarmInfo, 2);
    }

    //推送推荐指令
    public void sendAidDesSubStep(AlarmInfo alarmInfo){
        alarmFlowchartService.setExecuteFlag(alarmInfo, 3);
        log.info("虚拟编组推送第一次推荐指令,录入参数:{}", alarmInfo);
        //获取推荐指令执行单元
        List<AidDesSubStepEntity> aidDesSubSteps = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), NumConstant.ONE, NumConstant.ZERO);
        //组装推荐指令执行单元
        List<DisposeDto> stepLists = aidDesSubStepUtils.getStepList(aidDesSubSteps, alarmInfo);
        long detailId = UidGeneratorUtils.getUID();
        alarmInfoOprtService.insertAdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "系统产生第一次推荐指令", "1", "第一次推荐指令");
        //设置推荐指令内容
        AdmIdea admIdea = new AdmIdea(alarmInfo.getTableInfoId(), alarmInfo.getTrainId(), alarmInfo.getOrderNum(), alarmInfo.getStartAlarmTime(), alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(), alarmInfo.getAlarmTypeDetail(), stepLists, alarmInfo.getStationId(), alarmInfo.getAlarmState(), alarmInfo.getExecuteStep());
        //设置故障类型文本信息
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescribeByCode(admIdea.getAlarmType()));
        //设置故障子类型文本信息
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
        //设置推荐指令执行单元对象集合
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(aidDesSubSteps));
        //设置推荐指令标题
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        Long boxId = UidGeneratorUtils.getUID();
        alarmInfo.setTableBoxId(boxId);
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
        //告警信息存入子表
        admAlertInfoSubService.updateById(alarmInfo);
        log.info("推荐指令推送内容admInfo：{}", JsonUtils.toJSONString(admInfo));
        //推送推荐指令
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmFlowchartService.setExecuteFlag(alarmInfo, 5);
    }

    public void judgeStep(String choice){
        //获取存活的alarmInfo
        AlarmInfo alarmInfo = admAlertInfoSubService.getInfoInLife();
        if (alarmInfo == null){
            throw new BizException("未获取到处于生命周期的alarmInfo");
        }
        log.info("前端选择为{}", choice);
        if (TWO.equals(choice)){
            log.info("执行故障恢复策略");
            alarmFlowchartService.setExecuteFlag(alarmInfo, 9);
            FailureRecoveryStrategy strategy = FailureRecoveryStrategyFactory.getStrategy(alarmInfo);
            //执行故障恢复策略
            strategy.pushRecoveryAdm(alarmInfo);
        }else if (ONE.equals(choice)){
            log.info("虚拟编组推送第二次推荐指令,录入参数:{}", alarmInfo);
            alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_2);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 10);
            //获取推荐指令执行单元
            List<AidDesSubStepEntity> aidDesSubSteps = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), NumConstant.TWO, NumConstant.ZERO);
            //组装推荐指令执行单元
            List<DisposeDto> stepLists = aidDesSubStepUtils.getStepList(aidDesSubSteps, alarmInfo);
            long detailId = UidGeneratorUtils.getUID();
            alarmInfoOprtService.insertAdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "系统产生第二次推荐指令", "1", "第二次推荐指令");
            //设置推荐指令内容
            AdmIdea admIdea = new AdmIdea(alarmInfo.getTableInfoId(), alarmInfo.getTrainId(), alarmInfo.getOrderNum(), alarmInfo.getStartAlarmTime(), alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(), alarmInfo.getAlarmTypeDetail(), stepLists, alarmInfo.getStationId(), alarmInfo.getAlarmState(), alarmInfo.getExecuteStep());
            //设置故障类型文本信息
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescribeByCode(admIdea.getAlarmType()));
            //设置故障子类型文本信息
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
            //设置推荐指令执行单元对象集合
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(aidDesSubSteps));
            //设置推荐指令标题
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            admIdea.setShowSecond(200);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            Long boxId = UidGeneratorUtils.getUID();
            alarmInfo.setTableBoxId(boxId);
            //推荐指令入库
            alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, "方案待执行", JsonUtils.toJSONString(admIdea));
            //告警信息存入子表
            admAlertInfoSubService.updateById(alarmInfo);
            log.info("推荐指令推送内容admInfo：{}", JsonUtils.toJSONString(admInfo));
            //推送推荐指令
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 13);
        }else {
            throw new BizException("虚拟编组获得的弹窗选择参数不正确");
        }
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.VIRTUAL_GROUP;
    }
}
