package com.tct.itd.adm.msgRouter.executeHandle.failureRecovery;

import com.google.common.collect.Lists;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @Description :道岔故障-所有故障恢复handler
 * @Author : zhangjiarui
 * @Date : Created in 2021/12/23
 */
@Slf4j
@Component
public class SwitchFaultAdmFailureRecoveryHandler {

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Resource
    private AdmCommonMethodService admCommonMethodService;

    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AppPushService appPushService;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource
    private AdmAlertInfoService admAlertInfoService;

    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //根据故障子类型获取描述
        String alarmDescribe = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
        long detailId = UidGeneratorUtils.getUID();
        alarmInfoOprtService.insertAdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "系统产生故障恢复推荐指令", "1", "故障恢复：" + alarmDescribe);
        //创建推荐指令指令对象
        AdmIdea admIdea = AdmIdea.getAdmIdeaForAlertDetailBox(alarmInfo, auxiliaryDecision);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        admIdea.setSwitchName(alarmInfo.getSwitchName());
        //推荐指令状态
        String decisionState = "方案已执行";
        //执行故障恢复推荐指令
        if (auxiliaryDecision.getExecuteStep() != IidsConstPool.EXECUTE_STEP_0_99) {
            //置灰预览图按钮
            admCommonMethodService.disablePreviewButton(alarmInfo);
            //执行推荐指令，调用对应handle处理
            aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
            //标识故障已恢复
            alarmInfoOprtService.failureRecovery(alarmInfo.getTableInfoId());
            log.info("道岔故障,故障解除:{}", alarmInfo);
            alarmInfo.setExecuteStep(auxiliaryDecision.getExecuteStep());
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
            // 第一步推荐指令的故障恢复(晚点小于120s)
            if (auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_1 && Boolean.TRUE.equals(!BasicCommonCacheUtils.exist(Cache.SWITCH_FAILURE_RECOVERY_FIRST_CHANGE_GRAPH))) {
                alarmInfo.setEndLife(false);
                // 适配北19新需求
                BasicCommonCacheUtils.delMapKey(Cache.ALREADY_REPORT_SWITCH_FAILURE, alarmInfo.getSwitchNo());
                BasicCommonCacheUtils.delKey(Cache.RECOVERY_SWITCH_FAILURE);
            }
            // 如果是第二、三步推荐指令的故障恢复
            if (auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_2 || auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_3 || BasicCommonCacheUtils.exist(Cache.SWITCH_FAILURE_RECOVERY_FIRST_CHANGE_GRAPH)) {
                alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案", "2", "运行图方案选择");
                // 故障恢复按钮置灰
                admAlertInfoService.updateAllowFailoverById(auxiliaryDecision.getTableInfoId(), NumConstant.TWO);
            }
            // 如果车站未点击确认弹窗，故障就恢复,关闭车站的确认弹窗
            if (alarmInfo.getAlarmTypeDetail() != 1201 && auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_2) {
                // 关闭车站确认弹窗
                // appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(MsgPushEnum.CLOSE_STATION_CONFIRM_NOTICE_MSG.getCode(),
                //         Integer.toString(alarmInfo.getStationId()), "关闭车站的确认弹窗"));
            }
            flushFlowchart(auxiliaryDecision, alarmInfo);
            admAlertInfoSubService.updateById(alarmInfo);
        } else { //如果executeStep = -1，执行放弃故障恢复推荐指令
            decisionState = "方案已放弃";
            //推荐指令入库
            log.info("道岔故障,放弃故障恢复推荐指令:{}", alarmInfo);
            alarmInfo.setEndLife(false);
            admAlertInfoSubService.updateById(alarmInfo);
        }
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(UidGeneratorUtils.getUID(), detailId, decisionState, JsonUtils.toJSONString(admIdea));
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
    }

    /**
     * 刷新流程图
     * @author liyunlong
     * @date 2022/11/14 11:29
     * @param auxiliaryDecision 推荐指令信息
     * @param alarmInfo  告警信息
     */
    private void flushFlowchart(AuxiliaryDecision auxiliaryDecision, AlarmInfo alarmInfo) {
        int alarmTypeDetail = alarmInfo.getAlarmTypeDetail();
        long tableInfoId = alarmInfo.getTableInfoId();
        // 第一步推荐指令的故障恢复(晚点小于120s)
        if (auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_1 && !BasicCommonCacheUtils.exist(Cache.SWITCH_FAILURE_RECOVERY_FIRST_CHANGE_GRAPH)) {
            switch(alarmTypeDetail){
                case 1201:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(22, 23));
                    break;
                case 1202:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(28,29));
                    break;
                case 1203:
                case 1204:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(32,33));
                    break;
                default:
                    break;
            }
        }
        // 第一步推荐指令的故障恢复(晚点大于120s)
        if (auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_1 && Boolean.TRUE.equals(BasicCommonCacheUtils.exist(Cache.SWITCH_FAILURE_RECOVERY_FIRST_CHANGE_GRAPH))) {
            switch(alarmTypeDetail){
                case 1201:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(24, 25, 26));
                    break;
                case 1202:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(30, 31, 32));
                    break;
                case 1203:
                case 1204:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(34, 35, 36));
                    break;
                default:
                    break;
            }
        }
        Boolean isRecovery = Objects.isNull(alarmInfo.getAutoReport()) ? Boolean.FALSE : alarmInfo.getAutoReport();
        // 弹出第二次推荐指令,但未执行推荐指令,故障恢复
        if (auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_2 && Boolean.TRUE.equals(isRecovery)) {
            switch(alarmTypeDetail){
                case 1201:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(24, 25, 26));
                    break;
                case 1202:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(30, 31, 32));
                    break;
                case 1203:
                case 1204:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(34, 35, 36));
                    break;
                default:
                    break;
            }
        }
        // 第二、三次推荐指令故障恢复
        if ((auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_2 && Boolean.FALSE.equals(isRecovery)) || auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_3) {
            switch (alarmTypeDetail) {
                case 1201:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(18, 19, 20, 21));
                    break;
                case 1202:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(24, 25, 26, 27));
                    break;
                case 1203:
                case 1204:
                    alarmFlowchartService.setExecuteFlags(tableInfoId, Lists.newArrayList(28, 29, 30, 31));
                    break;
                default:
                    break;
            }
        }
    }
}