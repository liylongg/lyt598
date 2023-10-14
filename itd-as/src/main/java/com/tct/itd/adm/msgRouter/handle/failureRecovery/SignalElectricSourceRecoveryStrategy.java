package com.tct.itd.adm.msgRouter.handle.failureRecovery;


import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.iconstant.DesSubStepBeanConstant;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.adm.util.HoldTrainUtil;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.Info;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.UidGeneratorUtils;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description : 信号电源故障恢复策略
 * @Author : zhoukun
 * @Date : Created in 2022/4/19
 */
@Slf4j
@Component("pushRecovery1701")
public class SignalElectricSourceRecoveryStrategy implements FailureRecoveryStrategy {
    private static final int ZERO_NUM = 0;
    private static final String ONE = "1";
    private static final int ONE_NUM = 1;
    private static final int TWO_NUM = 2;
    private static final int FIVE_NUM = 5;
    private static final String DELETEHOLDTRAIN ="deleteHoldTrain";

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AdmAlertDetailService admAlertDetailService;
    @Resource
    private HoldTrainUtil holdTrainUtil;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;


    @Override
    public void pushRecoveryAdm(AlarmInfo alarmInfo) {
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo,"该故障生命周期已结束");
        // 第一次推荐指令未执行
        boolean b = (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_1
                && alarmInfo.getExecuteEnd() == ONE_NUM)
                || (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_2
                && alarmInfo.getExecuteEnd() == ZERO_NUM);
        // 第一次推荐指令执行后到第二次推荐指令执行前
        if (b) {
            log.info("信号电源故障, 第一次推荐指令执行后, 推送故障恢复推荐指令");
            alarmFlowchartService.setExecuteFlag(alarmInfo,8);
            powerFirstRecovery(alarmInfo);
        } else {
            log.info("信号电源故障,非第一次推荐指令执行后,推送故障恢复推荐指令");
            alarmFlowchartService.setExecuteFlag(alarmInfo,22);
            powerNotFirstRecovery(alarmInfo);
            alarmFlowchartService.setExecuteFlag(alarmInfo,23);
        }

        // 自动推送故障恢复后, 定标志定时任务不再重复推送
        alarmInfo.setFailureRecoveryStep(TWO_NUM);
        admAlertInfoSubService.updateById(alarmInfo);
    }

    /**
     * 第一次推荐指令执行后第二次执行前故障恢复直接取消扣车, 故障流程结束
     * @param alarmInfo 告警信息
     */
    private void powerFirstRecovery(AlarmInfo alarmInfo) {
        log.info("信号电源,类型:{},开始推送第一次推荐指令时进行故障恢复,未晚点,alarmInfo:{}", alarmInfo.getAlarmTypeDetail(), alarmInfo);
        //推荐指令步骤
        int step = IidsConstPool.EXECUTE_STEP_0_1;
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 0);
        //检查列车是否晚点2分钟
        holdTrainUtil.checkTrainIsLate(alarmInfo);
        //如果晚点不超过2分钟，不调图，剔除调图步骤
        if (!BasicCommonCacheUtils.exist(Cache.TWO_MINUTES_LATE_RECOVERY_CHANGE_GRAPH)){
            log.info("接触网失电第一次推荐指令恢复时，列车晚点未超过两分钟，不调图");
            entities = entities.stream().filter(t -> !t.getBeanName().equals("sendGraphCase")).collect(Collectors.toList());
           //替换为取消扣车缓存
            for (AidDesSubStepEntity e : entities) {
                if (e.getBeanName().equals(DELETEHOLDTRAIN)){
                    e.setBeanName(DesSubStepBeanConstant.DOOR_CANCEL_HOLD_TRAIN);
                }
            }
        }
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        // executeStep为-2, 方便执行故障恢复推荐指令时判断
        admIdea.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_1);
        admIdea.setDispose(stepList);
        //赋值执行单元信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        //推送弹窗显示时长 暂定180s
        admIdea.setShowSecond(180);
        int subCode = alarmInfo.getAlarmTypeDetail();
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode((subCode)));
        AlarmInfo alarmInfo1=new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        alarmInfo1.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_1);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo1));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_1);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
        alarmFlowchartService.setExecuteFlag(alarmInfo,9);
    }

    /**
     * 故障恢复直接进行到推荐指令最后一步
     * @param alarmInfo 告警信息
     */
    private void powerNotFirstRecovery(AlarmInfo alarmInfo) {
        log.info("信号电源,类型:{},开始推送推荐指令时进行故障恢复,未晚点,alarmInfo:{}", alarmInfo.getAlarmTypeDetail(), alarmInfo);
        //推荐指令步骤
        int step = IidsConstPool.EXECUTE_STEP_0_1;
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 0);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        // executeStep为-1
        admIdea.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_3);
        admIdea.setDispose(stepList);
        //赋值执行单元信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        //推送弹窗显示时长 暂定180s，考虑是否修改为倒计时，非固定时间
        admIdea.setShowSecond(180);

        int subCode = alarmInfo.getAlarmTypeDetail();
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode((subCode)));
        AlarmInfo alarmInfo1=new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        alarmInfo1.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_3);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo1));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        long detailId = UidGeneratorUtils.getUID();
        //更新步骤为-3，推送方案列表时会拿到该步骤并判断是否调折返图
        alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_3);
        admAlertInfoSubService.updateById(alarmInfo);
        log.info("推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
    }
}