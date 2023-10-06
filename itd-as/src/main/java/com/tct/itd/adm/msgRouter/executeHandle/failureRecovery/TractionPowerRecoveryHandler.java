package com.tct.itd.adm.msgRouter.executeHandle.failureRecovery;


import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.AlertMsgConst;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;

/**
 * @Description :接触网失电-故障恢复handler
 * @Author : zhangjiarui
 * @Date : Created in 2022-3-11 09:42:31
 */
@Slf4j
@Component
public class TractionPowerRecoveryHandler {
    private static final String TWO = "2";
    private static final int ONE_NUM = 1;

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
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AdmAlertInfoService admAlertInfoService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    public TractionPowerRecoveryHandler() {

    }

    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        BasicCommonCacheUtils.delKey(Cache.TRAIN_POWER_PREVIEW_TIME_RECORD);
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        // 点击执行故障恢复决策后, 关闭牵引弹窗
        appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.POWER_CLOSE_POP,
                "0", ""));
        //根据故障子类型获取描述
        String alarmDescribe = admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
        long detailId = UidGeneratorUtils.getUID();
        alarmInfoOprtService.insertAdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "系统产生故障恢复推荐指令", "1", "故障恢复：" + alarmDescribe);
        //创建推荐指令指令对象
        AdmIdea admIdea = AdmIdea.getAdmIdeaForAlertDetailBox(alarmInfo, auxiliaryDecision);
        admIdea.setTractionSectionId(alarmInfo.getTractionSectionId());

        AlarmInfo alarmInfo1=new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        alarmInfo1.setExecuteStep(-2);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo1));

        //推荐指令状态
        String decisionState = "方案已执行";
        //执行故障恢复推荐指令
        if (auxiliaryDecision.getExecuteStep() != IidsConstPool.EXECUTE_STEP_0_99) {
            //置灰预览图按钮
            admCommonMethodService.disablePreviewButton(alarmInfo);
            //将故障恢复的状态为1的置为2，按钮置灰
            admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), IidsConstPool.ALLOW_FAILOVER_2);
            //执行推荐指令，调用对应handle处理
            aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
            //标识故障已恢复
            alarmInfoOprtService.failureRecovery(alarmInfo.getTableInfoId());
            alarmInfo.setExecuteStep(auxiliaryDecision.getExecuteStep());
            // 如果是第一次故障恢复则结束生命流程
            if (auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_1&&auxiliaryDecision.getAidDesSubStepDtoList().size()!=2) {
                alarmFlowchartService.setExecuteFlag(alarmInfo,56);
                alarmInfo.setEndLife(false);
                alarmFlowchartService.setExecuteFlag(alarmInfo,57);
                // 否则需要推送运行图选择
            }
            else if (auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_1&&auxiliaryDecision.getAidDesSubStepDtoList().size()==2) {
                alarmFlowchartService.setExecuteFlag(alarmInfo,53);
                alarmFlowchartService.setExecuteFlag(alarmInfo,54);
                alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案"
                        , TWO, "运行图方案选择", ONE_NUM);
            }
            else {
                if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 18)){
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 20);
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 21);
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 22);
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 23);

                }
                if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 31)){
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 37);
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 38);
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 39);
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 40);
                }
                if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 32)){
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 46);
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 47);
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 48);
                    alarmFlowchartService.setExecuteFlag(alarmInfo, 49);
                }
                alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案"
                        , TWO, "运行图方案选择", ONE_NUM);
            }
            admAlertInfoSubService.updateById(alarmInfo);
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
            log.info("接触网失电,故障解除:{}", alarmInfo);
        } else { //如果executeStep = -1，执行放弃故障恢复推荐指令
            decisionState = "方案已放弃";
            alarmInfoOprtService.insertAdmAlertDetailBox(UidGeneratorUtils.getUID(), detailId, "方案已放弃", JsonUtils.toJSONString(admIdea));
            //推荐指令入库
            log.info("接触网失电,放弃故障恢复推荐指令:{}", alarmInfo);
            alarmInfo.setEndLife(false);
            admAlertInfoSubService.updateById(alarmInfo);
            //修改应急事件状态为已放弃
            admAlertInfoService.updateStatusById(alarmInfo.getTableInfoId(), AlertMsgConst.AID_GIVE_UP);
            //将故障恢复的状态为1的置为2，按钮置灰
            admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), IidsConstPool.ALLOW_FAILOVER_2);
            //刷新前台页面
            appPushService.sendWebNoticeMessageToAny(
                    new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
        }
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(UidGeneratorUtils.getUID(), detailId, decisionState, JsonUtils.toJSONString(admIdea));
        //后面有推送前端简单信息的服务，code是208003，只用添加改变WebNoticeDto的noticeCode就行
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
    }

}