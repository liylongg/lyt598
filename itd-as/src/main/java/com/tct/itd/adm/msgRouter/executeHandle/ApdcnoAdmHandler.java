package com.tct.itd.adm.msgRouter.executeHandle;


import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.PlatformDoorUpdateFlowchartsService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.enums.FlowchartFlagEnum;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.exception.BizException;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @classname: ApdcnoAdmHandler
 * @description: 整侧站台门无法打开-推荐指令(Apdcno: All platform door can not open)
 * @author: liyunlong
 * @date: 2021/10/8 9:52
 */
@Service
@Slf4j
public class ApdcnoAdmHandler implements AuxiliaryDecisionHandler {

    /**
     * 数字常量1
     */
    public static final int ONE_NUM = 1;
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private PlatformDoorUpdateFlowchartsService platformDoorUpdateFlowchartsService;

    @Resource
    private AppPushService appPushService;

    /**
     * 整侧车门无法打开，执行第一步推荐指令
     * 先将告警信息存入redis,key:ADM_IDEA_EXECUTE,200s超时,--??
     * 然后再从redis取出ADM_IDEA_WAIT(故障信息),从redis删除。--家瑞修改缓存超时问题
     * 更新第一次推荐指令方案状态为已执行
     * 告警信息存入redis,key:ADM_ALARM_INFO,不超时，等待定时任务触发扣车逻辑
     * 发送电子调度命令
     *
     * @param auxiliaryDecision 推荐指令
     * @author liyunlong
     * @date 2021/10/8 10:06
     */
    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        int executeStep = auxiliaryDecision.getExecuteStep();
        if (executeStep < 0) {
            doAidDecisionRecovery(auxiliaryDecision);
            return;
        }
        switch (executeStep) {
            case IidsConstPool.EXECUTE_STEP_1:
                log.info("执行第一次推荐指令:{}", auxiliaryDecision);
                doAuxiliaryDecisionOne(auxiliaryDecision);
                break;
            case IidsConstPool.EXECUTE_STEP_2:
                log.info("执行第二次推荐指令:{}", auxiliaryDecision);
                doAuxiliaryDecisionTwo(auxiliaryDecision);
                break;
            default:
                log.error("推荐指令命令步骤有误,请查看数据是否正确--{}", JsonUtils.toJSONString(auxiliaryDecision));
                throw new BizException("推荐指令命令步骤有误,请查看数据是否正确");
        }
        //更新流程图
        platformDoorUpdateFlowchartsService.APDCOExecute(auxiliaryDecision.getTableInfoId(), auxiliaryDecision.getExecuteStep());
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
        //推荐指令状态
        String decisionState = "方案已执行";
        alarmInfoOprtService.insertAdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "系统产生故障恢复推荐指令", "1", "故障恢复：" + alarmDescribe);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Long boxId = UidGeneratorUtils.getUID();
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(boxId, detailId, decisionState, JsonUtils.toJSONString(admIdea));
        int tag = 1;
        alarmInfo.setExecuteStep(auxiliaryDecision.getExecuteStep());
        if (auxiliaryDecision.getExecuteStep() != IidsConstPool.EXECUTE_STEP_0_99) {
            //执行推荐指令，调用对应handle处理
            aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
            //标识故障已恢复
            alarmInfoOprtService.failureRecovery(alarmInfo.getTableInfoId());
            alarmInfo.setExecuteEnd(1);
            log.info("整侧站台门无法打开,故障解除:{}", JsonUtils.toJSONString(alarmInfo));
            //晚点时间<2min 不调图直接结束生命周期
            Integer flowchartFlag = (Integer) BasicCommonCacheUtils.hGet(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG);
            if (!Objects.isNull(flowchartFlag) && flowchartFlag.equals(FlowchartFlagEnum.NOT_LATE.getCode())) {
                alarmInfo.setEndLife(false);
            }
            //更新流程图
            platformDoorUpdateFlowchartsService.APDCOExecute(alarmInfo.getTableInfoId(), auxiliaryDecision.getExecuteStep());
        } else { //执行放弃故障恢复推荐指令
            //推荐指令入库
            log.info("放弃故障恢复推荐指令:{}", alarmInfo);
            alarmInfo.setExecuteEnd(2);
            tag = 0;
            alarmInfo.setEndLife(false);
        }
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), boxId, tag);
        admAlertInfoSubService.updateById(alarmInfo);
        //后面有推送前端简单信息的服务，code是208003，只用添加改变WebNoticeDto的noticeCode就行
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
    }

    /**
     * 执行第一次推荐指令
     *
     * @param auxiliaryDecision 推荐指令相关信息
     * @author liyunlong
     * @date 2021/10/11 10:22
     */
    private void doAuxiliaryDecisionOne(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        log.info("站台门故障-整侧站台门无法打开,执行第一次推荐指令,alarmInfo:{}", alarmInfo);
        // 更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), ONE_NUM);
        // 执行推荐指令，调用对应的handler处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
    }

    /**
     * 执行第二次推荐指令
     * 先将告警信息存入redis,key:ADM_IDEA_EXECUTE,200s超时,--??
     * 然后再从redis取出ADM_IDEA_WAIT(故障信息),从redis删除。--家瑞修改缓存超时问题
     * 更新第一次推荐指令方案状态为已执行
     *
     * @param auxiliaryDecision 推荐指令相关信息
     * @author liyunlong
     * @date 2021/10/11 10:16
     */
    private void doAuxiliaryDecisionTwo(AuxiliaryDecision auxiliaryDecision) {
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
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.ALL_PLATFORM_DOOR_CANNOT_OPEN;
    }

}
