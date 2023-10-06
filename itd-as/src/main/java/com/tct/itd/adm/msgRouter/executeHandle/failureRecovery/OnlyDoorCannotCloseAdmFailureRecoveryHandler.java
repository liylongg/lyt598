package com.tct.itd.adm.msgRouter.executeHandle.failureRecovery;

import com.google.common.collect.Lists;

import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.msgRouter.executeHandle.WaiveAidDecisionAdmHandler;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;

import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.common.constant.AlertMsgConst;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.AppPushServiceUtil;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.UidGeneratorUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;

/**
 * @Description 单车门无法关闭-执行故障恢复推荐指令
 * @Author yuelei
 * @Date 2021/8/12 19:17
 **/

@Slf4j
@Component
public class OnlyDoorCannotCloseAdmFailureRecoveryHandler {

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
    private AdmAlertInfoService admAlertInfoService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private WaiveAidDecisionAdmHandler waiveAidDecisionAdmHandler;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        //重新赋值执行步骤
        alarmInfo.setExecuteStep(auxiliaryDecision.getExecuteStep());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        long detailId = UidGeneratorUtils.getUID();
        int executeStep = auxiliaryDecision.getExecuteStep();
        //根据故障子类型获取描述
        String alarmDescribe = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
        //创建推荐指令指令对象
        AdmIdea admIdea = AdmIdea.getAdmIdeaForAlertDetailBox(alarmInfo, auxiliaryDecision);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //操作结果
        String optResult = executeStep != IidsConstPool.EXECUTE_STEP_0_99 ? "方案已执行" : "方案已放弃";
        alarmInfoOprtService.insertAdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "系统产生故障恢复推荐指令", "1", "故障恢复：" + alarmDescribe);
        long boxId = UidGeneratorUtils.getUID();
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, optResult, JsonUtils.toJSONString(admIdea));
        //推荐指令状态
        if (executeStep != IidsConstPool.EXECUTE_STEP_0_99) {
            //执行推荐指令，调用对应handle处理
            aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
            //标识故障已恢复
            alarmInfoOprtService.failureRecovery(alarmInfo.getTableInfoId());
            // 如果正在预览运行图,此时列车故障恢复了,向客户端推送消息告知列车故障已经恢复，无需运行图预览
            AppPushServiceUtil.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.FAULT_RECOVERY, "0",
                    "列车故障已恢复,无需运行图预览"));

            //修改应急事件状态为已执行
            alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), boxId,1);
            //如果执行故障恢复，则更新数据库状态
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
            if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_1){
                alarmInfo.setEndLife(false);
                admAlertInfoSubService.updateById(alarmInfo);
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(31,33));
            }else{
                admAlertInfoSubService.updateExecuteEndAndStep(alarmInfo);
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(32,34));
            }
            //后面有推送前端简单信息的服务，code是208003，只用添加改变WebNoticeDto的noticeCode就行
            appPushService.sendWebNoticeMessageToAny(
                    new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
        } else { //执行放弃故障恢复推荐指令
            waiveAidDecisionAdmHandler.handle(auxiliaryDecision);
            //修改应急事件状态为已放弃
            admAlertInfoService.updateStatusById(alarmInfo.getTableInfoId(), AlertMsgConst.AID_GIVE_UP);
        }
    }
}
