package com.tct.itd.adm.msgRouter.handle.failureRecovery;

import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.iconstant.DesSubStepBeanConstant;
import com.tct.itd.adm.msgRouter.handle.TractionPowerHandler;
import com.tct.itd.adm.msgRouter.service.AppPushService;
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
 * @Description : 接触网失电故障恢复策略
 * @Author : zhangjiarui
 * @Date : Created in 2022/3/7
 */
@Slf4j
@Component("pushRecovery1501")
public class TractionPowerRecoveryStrategy implements FailureRecoveryStrategy {
    private static final int ZERO_NUM = 0;
    private static final int ONE_NUM = 1;
    private static final int TWO_NUM = 2;
    private static final String NULL = "无电区段不存在列车";

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
    private TractionPowerHandler tractionPowerHandler;
    @Resource
    private HoldTrainUtil holdTrainUtil;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    private static final String DELETEHOLDTRAIN ="deleteHoldTrain";

    @Override
    public void pushRecoveryAdm(AlarmInfo alarmInfo) {
        //alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        alarmInfo = admAlertInfoSubService.getInfoInLife();
        // 自动推送故障恢复后, 定标志定时任务不再重复推送
        alarmInfo.setFailureRecoveryStep(TWO_NUM);
        admAlertInfoSubService.updateById(alarmInfo);
        Assert.notNull(alarmInfo,"该故障生命周期已结束");
        boolean b = (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_1
                && alarmInfo.getExecuteEnd() == ONE_NUM)
                || (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_2
                && alarmInfo.getExecuteEnd() == ZERO_NUM);
        // 第一次推荐指令执行后到第二次推荐指令执行前
        if (b) {
            log.info("接触网失电,第一次推荐指令执行后到第二次推荐指令执行前,推送故障恢复推荐指令");
            powerFirstRecovery(alarmInfo);
            // 第2/3次推荐指令时
        } else {
            log.info("接触网失电,非第一次执行后,推送故障恢复推荐指令");
            powerNotFirstRecovery(alarmInfo);
        }

    }
    /**
     * 第一次推荐指令执行后第二次执行前故障恢复直接取消扣车, 故障流程结束
     * @param alarmInfo 告警信息
     */
    private void powerFirstRecovery(AlarmInfo alarmInfo) {
        alarmFlowchartService.setExecuteFlag(alarmInfo,51);
        log.info("接触网失电,类型:{},开始推送第一次推荐指令时进行故障恢复,未晚点,alarmInfo:{}", alarmInfo.getAlarmTypeDetail(), alarmInfo);
        //获取调度命令对象
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo);
        //推荐指令步骤
        int step = IidsConstPool.EXECUTE_STEP_0_1;
        admIdea.setExecuteStep(step);
        //查询故障决策指令-列车进站过程中站台门打开
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
        admIdea.setDispose(stepList);
        //赋值执行单元信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        // 设置牵引供电区段id
        admIdea.setTractionSectionId(alarmInfo.getTractionSectionId());
        //推送弹窗显示时长
        admIdea.setShowSecond(180);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        AlarmInfo alarmInfo1=new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        alarmInfo1.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_1);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo1));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        admAlertInfoSubService.updateById(alarmInfo1);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
        alarmFlowchartService.setExecuteFlag(alarmInfo,52);
    }

    /**
     * 非第一次推荐指令时故障恢复直接进行到推荐指令最后一步
     * @param alarmInfo 告警信息
     */
    private void powerNotFirstRecovery(AlarmInfo alarmInfo) {
         //不同流程进来点亮不同的流程图节点
        if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 11)){
            alarmFlowchartService.setExecuteFlag(alarmInfo, 18);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 19);
        }
        if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 31)){
            alarmFlowchartService.setExecuteFlag(alarmInfo, 35);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 36);
        }
        if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(), 32)){
            alarmFlowchartService.setExecuteFlag(alarmInfo, 44);
            alarmFlowchartService.setExecuteFlag(alarmInfo, 45);
        }
        log.info("接触网失电,类型:{},开始推送非第一次推荐指令时进行故障恢复,未晚点,alarmInfo:{}", alarmInfo.getAlarmTypeDetail(), alarmInfo);
        //推荐指令步骤
        int step = IidsConstPool.EXECUTE_STEP_4;
        //查询故障决策指令-列车进站过程中站台门打开
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 0);
        //获取并拼接推荐指令
        String s = tractionPowerHandler.getTrainForPower(alarmInfo);
        if ("".equals(s)) {
            s = NULL;
        }
        AidDesSubStepEntity aidDesSubStepEntity0 = entities.get(ZERO_NUM);
        aidDesSubStepEntity0.setSubStepContent(String.format(aidDesSubStepEntity0.getSubStepContent(), s));
        entities.set(ZERO_NUM, aidDesSubStepEntity0);

        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        // executeStep为-2
        admIdea.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_2);
        admIdea.setDispose(stepList);
        // 设置牵引供电区段id
        admIdea.setTractionSectionId(alarmInfo.getTractionSectionId());
        //赋值执行单元信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        //推送弹窗显示时长 暂定180s
        admIdea.setShowSecond(180);
        //故障恢复类型——去掉后两位
        int subCode = alarmInfo.getAlarmTypeDetail();
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode((subCode)));
        AlarmInfo alarmInfo1=new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        alarmInfo1.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_2);
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo1));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        // 将故障续报按钮置灰
//        admAlertInfoService.updateReportById(alarmInfo.getTableInfoId(), 2);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);

        //更新步骤为-2，推送方案列表时会拿到该步骤并判断是否调折返图
        alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_2);
        admAlertInfoSubService.updateById(alarmInfo);
        log.info("推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
    }

}