package com.tct.itd.adm.msgRouter.handle.failureRecovery;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.runGraph.PrePlanRunGraphContext;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.AdmRunGraphCases;
import com.tct.itd.common.dto.Info;
import com.tct.itd.common.dto.RecoveryTrainDto;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Description : 终端站折返道岔故障(具备本站折返)的推送故障恢复
 * @Author : zhangjiarui
 * @Date : Created in 2021/12/28
 */
@Slf4j
@Component("pushRecovery1201")
public class SwitchFaultBackHasPreRecoveryStrategy implements FailureRecoveryStrategy {

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AppPushService appPushService;

    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;

    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private PrePlanRunGraphContext prePlanRunGraphContext;


    @Resource
    private SwitchFailureRecovery switchFailureRecovery;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Override
    public void pushRecoveryAdm(AlarmInfo alarmInfo) {
        Optional.ofNullable(alarmInfo).orElseThrow(() -> new BizException("获得alarmInfo为空!"));
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo,"该故障生命周期已结束");
        // 第一次推荐指令时
        if (alarmInfo.getExecuteStep() == 1) {
            log.info("道岔故障,executeStep=1,推送故障恢复推荐指令");
            switchFaultFirstRecovery(alarmInfo);
        // 第2/3次推荐指令时
        } else {
            log.info("道岔故障,executeStep != 1,推送故障恢复推荐指令");
            switchFaultNotFirstRecovery(alarmInfo);
        }
    }

    /**
     * 道岔故障第一次推荐指令故障恢复
     *
     * @param alarmInfo
     */
    private void switchFaultFirstRecovery(AlarmInfo alarmInfo) {
        log.info("道岔故障,类型:{},开始推送第一次推荐指令时进行故障恢复,未晚点,alarmInfo:{}", alarmInfo.getAlarmTypeDetail(), alarmInfo);
        int step = -1;
        // 查询推荐指令
        List<AidDesSubStepEntity> entities;
        // 调整运行图
        if (BasicCommonCacheUtils.exist(Cache.SWITCH_FAILURE_RECOVERY_FIRST_CHANGE_GRAPH)) {
            entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 1);
        } else {
            entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 0);
        }
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        //获取调度命令对象
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        //赋值执行单元信息
        admIdea.setExecuteStep(-1);
        admIdea.setDispose(stepList);
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        //推送弹窗显示时长
        Integer countDown = Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.COUNT_DOWN));
        admIdea.setShowSecond(countDown);
        // 故障恢复
        alarmInfo.setExecuteStep(NumConstant.NEGATIVE_ONE);
        alarmInfo.setExecuteEnd(NumConstant.ZERO);
        admAlertInfoSubService.updateById(alarmInfo);
        int subCode = alarmInfo.getAlarmTypeDetail();
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode((subCode)));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(9, 11));
    }

    /**
     * 道岔故障第2/3次推荐指令故障恢复策略
     */
    private void switchFaultNotFirstRecovery(AlarmInfo alarmInfo) {
        log.info("道岔故障,类型:{},第{}次推荐指令时进行故障恢复,alarmInfo:{}", alarmInfo.getAlarmTypeDetail(), alarmInfo.getExecuteStep(), alarmInfo);
        // 故障恢复,将重复推送运行图预览方案的时间参数去掉
        BasicCommonCacheUtils.delKey(Cache.SWITCH_TEN_PREVIEW_SIGN);
        BasicCommonCacheUtils.delKey(Cache.SWITCH_FAILURE_TEN_TIME);
        BasicCommonCacheUtils.delKey(Cache.SWITCH_TWENTY_PREVIEW_SIGN);
        BasicCommonCacheUtils.delKey(Cache.SWITCH_FAILURE_TWENTY_TIME);
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        int step = 4;
        if (alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE)) {
            step = 3;
        }
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 0);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        int executeStep = alarmInfo.getExecuteStep();
        Integer executeEnd = alarmInfo.getExecuteEnd();
        // 弹出推荐指令但是未执行故障恢复
        if (NumConstant.TWO.equals(executeStep) && NumConstant.ZERO.equals(executeEnd)) {
            switchFailureRecovery.recoveryWithoutExecute(alarmInfo,entities);
            return;
        }
        if (executeStep == 2 || executeStep == -2) {
            alarmInfo.setExecuteStep(-2);
        } else {
            alarmInfo.setExecuteStep(-3);
        }
        admAlertInfoSubService.updateById(alarmInfo);
        // 调用算法获取调整方案列表
        AdmRunGraphCases admRunGraphCases = null;
        try {
            admRunGraphCases = prePlanRunGraphContext.listPreviewRunGraph(alarmInfo);
        } catch (Exception e) {
            log.error("道岔故障恢复调用算法获取调整方案列表出错,异常信息:【{}】", e.getMessage());
            // 获取运行图预览方案失败,直接结束故障流程。
            ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
        }
        stepList = getStepList(admRunGraphCases, stepList);
        //获取调度命令对象
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        if (executeStep == 2 || executeStep == -2) {
            admIdea.setExecuteStep(-2);
        } else {
            admIdea.setExecuteStep(-3);
        }
        admIdea.setDispose(stepList);
        //赋值执行单元信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        //推送弹窗显示时长,通过系统参数配置
        Integer countDown = Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.COUNT_DOWN));
        admIdea.setShowSecond(countDown);
        int subCode = alarmInfo.getAlarmTypeDetail();
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode((subCode)));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(),Lists.newArrayList(16,17));
    }


    /**
     * 重新组装stepList
     * @author liyunlong
     * @date 2022/2/19 14:01
     * @param admRunGraphCases 运行如预览方案
     * @param stepList 推荐指令执行步骤
     * @return java.util.List<java.lang.String>
     */
    private List<DisposeDto> getStepList(AdmRunGraphCases admRunGraphCases, List<DisposeDto> stepList) {
        List<DisposeDto> newStepList = Lists.newArrayList();
        List<RecoveryTrainDto> recoveryTrainDtoList = null;
        if (Objects.nonNull(admRunGraphCases)) {
            // 恢复正常折返车次号
            recoveryTrainDtoList = admRunGraphCases.getAdmRunGraphCases().get(0).getRecoveryTrainDtoList();
        } else {
            log.error("道岔故障通过算法获取运行图方案列表为空!");
        }
        String recoveryNumber;
        if (recoveryTrainDtoList.size() == NumConstant.ONE) {
            recoveryNumber = recoveryTrainDtoList.get(0).getRecoveryTrainNumber();
        } else {
            recoveryNumber =
                    StringUtils.join(recoveryTrainDtoList.stream().map(s -> s.getRecoveryTrainNumber() + "(" + s.getUpDown() + ")").collect(Collectors.toList()), "、");
        }
        BasicCommonCacheUtils.set(Cache.RECOVERY_TRAIN_NUMBER, recoveryNumber);
        String finalRecoveryTrainNumber = recoveryNumber;
        stepList.forEach(s -> {
            if (s.getStep().contains("%s")) {
                s.setStep(String.format(s.getStep(), finalRecoveryTrainNumber));
                newStepList.add(s);
                return;
            }
            newStepList.add(s);
        });
        stepList = newStepList;
        return stepList;
    }
}