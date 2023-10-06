package com.tct.itd.adm.msgRouter.executeHandle;


import com.google.common.collect.Lists;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.AppPushServiceUtil;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;

/**
 * @ClassName MCMFaultAdmHandler 牵引故障
 * @Description TODO
 * @Author zhoukun
 * @Date 2022/7/20 11:40
 */
@Slf4j
@Component
public class MCMFaultAdmHandler implements AuxiliaryDecisionHandler {
    @Resource
    private AlarmUtil alarmUtil;
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;
    @Resource
    private AdmAlertInfoService admAlertInfoService;
    @Resource
    private AdmCommonMethodService admCommonMethodService;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        int executeStep = auxiliaryDecision.getExecuteStep();
        if (executeStep < 0) {
            doAidDecisionRecovery(auxiliaryDecision);
            return;
        }
        switch (executeStep) {
            //执行扣车推荐指令
            case 1:
                log.info("牵引故障，执行第一次推荐指令:{}", auxiliaryDecision);
                doAidDecisionOne(auxiliaryDecision);
                break;
            //执行运行图推荐指令
            case 2:
                log.info("牵引故障，执行第二次推荐指令:{}", auxiliaryDecision);
                doAidDecisionTwo(auxiliaryDecision);
                break;
            default:
                log.error("推荐指令命令步骤有误,请查看数据是否正确--{}", JsonUtils.toJSONString(auxiliaryDecision));
                throw new RuntimeException("推荐指令命令步骤有误,请查看数据是否正确");
        }
    }

    /**
     * 执行故障恢复辅助决策
     *
     * @param auxiliaryDecision 推荐指令相关信息
     * @author liyunlong
     * @date 2021/10/21 16:09
     */
    private void doAidDecisionRecovery(AuxiliaryDecision auxiliaryDecision) {
        log.info("牵引故障恢复中auxiliaryDecision:{}", auxiliaryDecision);
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        int executeStep = auxiliaryDecision.getExecuteStep();
        //执行故障恢复推荐指令
        if (executeStep == IidsConstPool.EXECUTE_STEP_0_5 || executeStep == IidsConstPool.EXECUTE_STEP_0_6) {
            //更新状态
            admAlertInfoSubService.updateExecuteStep(executeStep, alarmInfo.getTableInfoId());
            //执行推荐指令，调用对应handle处理
            aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        } else {
            log.info("将故障恢复的按钮置灰");
            //将故障恢复的状态为1的置为2，按钮置灰
            admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), IidsConstPool.ALLOW_FAILOVER_2);
            //根据故障子类型获取描述
            String alarmDescribe = AdmAlertDetailTypeService.getDescribeByCode(String.valueOf(alarmInfo.getAlarmTypeDetail()));
//            alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "向相关人员发布故障恢复信息", "0", "故障恢复：" + alarmDescribe);
            long detailId = UidGeneratorUtils.getUID();
            //创建推荐指令指令对象
            AdmIdea admIdea = AdmIdea.getAdmIdeaForAlertDetailBox(alarmInfo, auxiliaryDecision);
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令状态
            String decisionState = "方案已执行";
            log.info("获取牵引故障告警信息:{}", JsonUtils.toJSONString(alarmInfo));
            //执行故障恢复推荐指令
            if (auxiliaryDecision.getExecuteStep() != IidsConstPool.EXECUTE_STEP_0_99) {
                // 牵引区间故障恢复晚点第一次不清除缓存
                if (alarmInfo.getSectionFlag() == 1 && auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_3 && (alarmInfo.getFailureRecoveryStep() != 2)) {
                    alarmInfo.setAlarmState(AlarmStateEnum.LATE_ADJUSTMENT.getCode());
                    alarmInfo.setExecuteStep(auxiliaryDecision.getExecuteStep());
                    alarmInfo.setEndLife(true);
                    log.info("牵引区间故障恢复执行晚点调图第一次不清除缓存");
                } else {
                    // 站台以及牵引区间第二次故障恢复 清除缓存
                    this.clearRedis();
                    log.info("牵引站台或者牵引区间第二次故障恢复执行清除缓存,alarmInfo:{}, auxiliaryDecision.getExecuteStep:{}", alarmInfo, auxiliaryDecision.getExecuteStep());
                    //标识故障已恢复, 牵引区间第一次不执行该操作
                    alarmInfoOprtService.failureRecovery(alarmInfo.getTableInfoId());
                    alarmInfo.setEndLife(false);
                }
                //置灰预览图按钮
                admCommonMethodService.disablePreviewButton(alarmInfo);
                //执行推荐指令，调用对应handle处理
                aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);

                log.info("牵引故障,故障解除:{}", alarmInfo);
                // 如果正在预览运行图,此时列车故障恢复了,向客户端推送消息告知列车故障已经恢复，无需运行图预览
                AppPushServiceUtil.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.FAULT_RECOVERY, "0",
                        "列车故障已恢复,无需运行图预览"));
                //如果执行故障恢复，则更新数据库状态
                alarmInfo.setExecuteStep(auxiliaryDecision.getExecuteStep());
                alarmInfo.setExecuteEnd(1);
                admAlertInfoSubService.updateById(alarmInfo);
                log.info("牵引故障恢复,alarmInfo:{}", alarmInfo);
            } else { //执行放弃故障恢复推荐指令
                decisionState = "方案已放弃";
                //推荐指令入库
                log.info("牵引故障,放弃故障恢复推荐指令:{}", alarmInfo);
            }
            //推荐指令入库
            alarmInfoOprtService.insertAdmAlertDetailBox(UidGeneratorUtils.getUID(), detailId, decisionState, JsonUtils.toJSONString(admIdea));
            alarmInfoOprtService.insertAdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "系统产生故障恢复推荐指令", "1", "故障恢复：" + alarmDescribe);
            //后面有推送前端简单信息的服务，code是208003，只用添加改变WebNoticeDto的noticeCode就行
            appPushService.sendWebNoticeMessageToAny(
                    new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
        }
    }

    /**
     * 清除缓存
     */
    public void clearRedis() {
        //删除受影响的车次列表缓存
        BasicCommonCacheUtils.delKey(Cache.ADM_AFFECTED_TRAIN);
        //清理告警超时缓存
        BasicCommonCacheUtils.delKey(Cache.ALARM_CLOSE_DOOR_TIME_OUT);
    }

    private void doAidDecisionOne(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        log.info("MCM产生故障,执行第一次推荐指令,alarmInfo-" + alarmInfo);
        //获取受影响车次列表 以及抬车信息
        log.info("MCM产生故障,执行第一次推荐指令,请求获取受影响车次、扣车站台、站台抬车时间信息");
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 1);
        log.info("等待监测牵引故障恢复正常");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(2, 3, 4, 5, 6));
    }

    private void doAidDecisionTwo(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        // 更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(), 1);
        // 执行推荐指令，调用对应的handler处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        // 更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案"
                , "2", "运行图方案选择", 1);
        if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 7)) {
            alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(9, 10, 11, 12));
        } else if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 8)) {
            alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(14, 15, 16, 17, 18, 19));
        }

    }

    @Override
    public String channel() {
        return AlarmTypeConstant.MORE_MCM_FAILURE;
    }
}
