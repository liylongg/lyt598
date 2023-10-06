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
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author kangyi
 * @description 单档站台门无法打开应急事件执行推荐指令处理类
 * @date 2021/10/9
 **/

@Slf4j
@Component
public class SinglePlatformDoorCannotOpenAdmHandler implements AuxiliaryDecisionHandler {

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private PlatformDoorUpdateFlowchartsService platformDoorUpdateFlowchartsService;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        log.info("收到执行推荐指令AuxiliaryDecision:{}", JsonUtils.toJSONString(auxiliaryDecision));
        switch (auxiliaryDecision.getExecuteStep()) {
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
        platformDoorUpdateFlowchartsService.SPDCOExecute(auxiliaryDecision.getTableInfoId(), auxiliaryDecision.getExecuteStep());
    }

    //执行第二次推荐指令
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
        //晚点时间<2min不调图直接结束故障生命周期
        Integer flowchartFlag = (Integer) BasicCommonCacheUtils.hGet(Cache.FLOWCHART_FLAG, Cache.CHANGE_GRAPH_FLAG);
        if (!Objects.isNull(flowchartFlag) && flowchartFlag.equals(FlowchartFlagEnum.NOT_LATE.getCode())) {
            alarmInfo.setEndLife(false);
        }
        admAlertInfoSubService.updateById(alarmInfo);
        //后面有推送前端简单信息的服务，code是208003，只用添加改变WebNoticeDto的noticeCode就行
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
    }

    //执行第一次推荐指令
    private void doFirstAuxiliaryDecision(AuxiliaryDecision auxiliaryDecision) {
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
        log.info("单档站台门无法打开故障,执行第一次推荐指令,alarmInfo-" + alarmInfo);
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 1);
        //更新子表状态为已执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
        admAlertInfoSubService.updateById(alarmInfo);
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.SINGLE_PLATFORM_DOOR_CANNOT_OPEN;
    }
}
