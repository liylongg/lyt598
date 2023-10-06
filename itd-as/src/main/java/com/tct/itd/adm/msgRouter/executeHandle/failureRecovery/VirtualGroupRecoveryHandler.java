package com.tct.itd.adm.msgRouter.executeHandle.failureRecovery;

import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.common.constant.AlertMsgConst;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.exception.BizException;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description: 虚拟编组故障恢复Handler
 * @Author: wangsijin
 * @Date: 2023/2/14 014 11:48
 */

@Slf4j
@Component
public class VirtualGroupRecoveryHandler{

    private static final String DELETEHOLDTRAIN ="deleteHoldTrain";

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AppPushService appPushService;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource
    private AdmAlertInfoService admAlertInfoService;


    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        //获取当前的alarmInfo
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
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
            //执行推荐指令，调用对应handle处理
            aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
            //标识故障已恢复
            alarmInfoOprtService.failureRecovery(alarmInfo.getTableInfoId());
            alarmInfo.setExecuteStep(auxiliaryDecision.getExecuteStep());
            List<AidDesSubStepOutDto> aidDesSubStepDtoList = auxiliaryDecision.getAidDesSubStepDtoList();
            if (CollectionUtils.isEmpty(aidDesSubStepDtoList)){
                throw new BizException("执行故障恢复时接受到的推荐指令内容为空, auxiliaryDecision:{}", JsonUtils.toJSONString(auxiliaryDecision));
            }
            //是否需调图
            boolean ifAdjustGraph = false;
            for (AidDesSubStepOutDto aidDesSubStepOutDto : aidDesSubStepDtoList) {
                if (DELETEHOLDTRAIN.equals(aidDesSubStepOutDto.getBeanName())){
                    ifAdjustGraph = true;
                    break;
                }
            }
            // 如果恢复时间小于2min则直接恢复故障
            if (ifAdjustGraph == false) {
                alarmFlowchartService.setExecuteFlag(alarmInfo,11);
                alarmInfo.setEndLife(false);
                alarmFlowchartService.setExecuteFlag(alarmInfo,14);
            }// 否则需要推送运行图选择
            else{
                alarmFlowchartService.setExecuteFlag(alarmInfo,12);
                alarmFlowchartService.setExecuteFlag(alarmInfo,15);
                alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案"
                        , "2", "运行图方案选择", 1);
            }
            admAlertInfoSubService.updateById(alarmInfo);
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
            log.info("虚拟编组,故障解除:{}", alarmInfo);
        } else { //如果executeStep = -1，执行放弃故障恢复推荐指令
            decisionState = "方案已放弃";
            alarmInfoOprtService.insertAdmAlertDetailBox(UidGeneratorUtils.getUID(), detailId, "方案已放弃", JsonUtils.toJSONString(admIdea));
            //推荐指令入库
            log.info("虚拟编组,放弃故障恢复推荐指令:{}", alarmInfo);
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
        // 更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(),NumConstant.ONE);
        //推荐指令入库
        alarmInfoOprtService.insertAdmAlertDetailBox(UidGeneratorUtils.getUID(), detailId, decisionState, JsonUtils.toJSONString(admIdea));
        //后面有推送前端简单信息的服务，code是208003，只用添加改变WebNoticeDto的noticeCode就行
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
    }

}
