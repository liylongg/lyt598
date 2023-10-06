package com.tct.itd.adm.msgRouter.executeHandle;


import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AxleCounterResetUpdateFlowchartsService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.enums.FlowchartFlagEnum;
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

/**
 * @author kangyi
 * @description 道岔区段计轴故障
 * @date 2022年 01月15日 14:51:46
 */
@Slf4j
@Component
public class AxleCounterSwitchResetAdmHandler implements AuxiliaryDecisionHandler {

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AxleCounterResetUpdateFlowchartsService axleCounterResetUpdateFlowchartsService;
    @Resource
    private AppPushService appPushService;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        log.info("收到执行推荐指令AuxiliaryDecision:{}", JsonUtils.toJSONString(auxiliaryDecision));
        int executeStep = auxiliaryDecision.getExecuteStep();
        if (executeStep < 0) {
            doAidDecisionRecovery(auxiliaryDecision);
            return;
        }
        switch (executeStep) {
            case IidsConstPool.EXECUTE_STEP_1:
                log.info("执行第一次推荐指令:{}", auxiliaryDecision);
                doFirstAuxiliaryDecision(auxiliaryDecision);
                break;
            case IidsConstPool.EXECUTE_STEP_2:
                log.info("执行第二次推荐指令:{}", auxiliaryDecision);
                doSecondAuxiliaryDecision(auxiliaryDecision);
                break;
            default:
                log.error("推荐指令命令步骤有误,请查看数据是否正确--{}", JsonUtils.toJSONString(auxiliaryDecision));
                throw new RuntimeException("推荐指令命令步骤有误,请查看数据是否正确");
        }
        //更新流程图
        axleCounterResetUpdateFlowchartsService.updateNotSwitchReset(auxiliaryDecision.getTableInfoId(), auxiliaryDecision.getExecuteStep());
    }

    /**
     * 执行故障恢复辅助决策
     *
     * @param auxiliaryDecision 推荐指令相关信息
     * @author liyunlong
     * @date 2021/10/21 16:09
     */
    private void doAidDecisionRecovery(AuxiliaryDecision auxiliaryDecision) {
        log.info("收到执行故障恢复推荐指令AuxiliaryDecision:{}", JsonUtils.toJSONString(auxiliaryDecision));
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        String alarmDescribe = AdmAlertDetailTypeService.getDescribeByCode(String.valueOf(alarmInfo.getAlarmTypeDetail()));
        long detailId = UidGeneratorUtils.getUID();
        //创建推荐指令指令对象
        AdmIdea admIdea = AdmIdea.getAdmIdeaForAlertDetailBox(alarmInfo, auxiliaryDecision);
        admIdea.setAxleCounterName(alarmInfo.getAxleCounterName());
        //推荐指令状态
        String decisionState = "方案已执行";
        alarmInfoOprtService.insertAdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "系统产生故障恢复推荐指令", "1", "故障恢复：" + alarmDescribe);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Long boxId = UidGeneratorUtils.getUID();
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, decisionState, JsonUtils.toJSONString(admIdea));
        int tag = 1;
        if (auxiliaryDecision.getExecuteStep() != IidsConstPool.EXECUTE_STEP_0_99) {
            BasicCommonCacheUtils.delKey(Cache.AXLE_COUNTER_SLOWLY_GRAPH_SIGN);
            //执行推荐指令，调用对应handle处理
            aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
            //标识故障已恢复
            alarmInfoOprtService.failureRecovery(alarmInfo.getTableInfoId());
            log.info("计轴故障,故障解除:{}", JsonUtils.toJSONString(alarmInfo));
            //如果执行故障恢复，则更新数据库状态
            alarmInfo.setExecuteEnd(1);
            //更新流程图
            axleCounterResetUpdateFlowchartsService.updateNotSwitchReset(alarmInfo.getTableInfoId(), auxiliaryDecision.getExecuteStep());
        } else { //执行放弃故障恢复推荐指令
            //推荐指令入库
            log.info("放弃故障恢复推荐指令:{}", alarmInfo);
            //如果执行故障恢复，则更新数据库状态
            alarmInfo.setExecuteEnd(2);
            alarmInfo.setEndLife(false);
            BasicCommonCacheUtils.delKey(Cache.AXLE_COUNTER_ADJUST_GRAPH);
            tag = 0;
        }
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), boxId, tag);
        alarmInfo.setExecuteStep(auxiliaryDecision.getExecuteStep());
        Integer changeGraph = (Integer) BasicCommonCacheUtils.hGet(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG);
        if (changeGraph != null && changeGraph.equals(FlowchartFlagEnum.NOT_LATE.getCode())) {
            alarmInfo.setEndLife(false);
        }
        admAlertInfoSubService.updateById(alarmInfo);
        //后面有推送前端简单信息的服务，code是208003，只用添加改变WebNoticeDto的noticeCode就行
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
    }

    /**
     * @param auxiliaryDecision
     * @description 执行第一次推荐指令
     * @date 2022/1/15 14:55
     * @author kangyi
     * @return: void
     */
    private void doFirstAuxiliaryDecision(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        //执行推荐指令，调用对应handle处理
        log.info("执行一次推荐指令,alarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 1);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        //删除已经上报过的计轴故障缓存
        BasicCommonCacheUtils.delMapKey(Cache.ALREADY_REPORT_AXLE_COUNTER, String.valueOf(alarmInfo.getAxleCounterId()));
    }

    /***
     * @description 执行第二次推荐指令
     * @date 2022/1/15 14:58
     * @author kangyi
     * @param auxiliaryDecision
     * @return: void
     */
    private void doSecondAuxiliaryDecision(AuxiliaryDecision auxiliaryDecision) {
        //获取告警信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        log.info("执行二次推荐指令,alarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
        //更新方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(), AlarmInfoEnum.EXECUTE_END_1.getCode());
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新子表状态为已执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
        //更改状态为已执行第二次推荐指令
        alarmInfo.setExecuteStep(auxiliaryDecision.getExecuteStep());
        admAlertInfoSubService.updateById(alarmInfo);
        BasicCommonCacheUtils.set(Cache.AXLE_COUNTER_SLOWLY_GRAPH_SIGN, "0");
    }


    @Override
    public String channel() {
        return AlarmTypeConstant.AXLE_COUNTER_SWITCH_RESET;
    }
}
