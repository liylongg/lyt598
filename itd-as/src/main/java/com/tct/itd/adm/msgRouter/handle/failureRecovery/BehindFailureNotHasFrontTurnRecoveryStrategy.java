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
import com.tct.itd.common.cache.Cache;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.AdmRunGraphCases;
import com.tct.itd.common.dto.Info;
import com.tct.itd.common.dto.RecoverySlowlyTrainDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Description : 线路终点站常态化站后道岔故障-不具备站前折返故障恢复
 * @Author : zhangjiarui
 * @Date : Created in 2021/12/28
 */
@Slf4j
@Component("pushRecovery1202")
public class BehindFailureNotHasFrontTurnRecoveryStrategy implements FailureRecoveryStrategy {
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
        // checkSwitchNoHandler.checkSwitchFailureIsRecovery(alarmInfo);
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
        //推荐指令步骤
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
        // 获取道岔故障正向流程的最后一步推荐指令
        int step = 4;
        if (alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE)) {
            step = 3;
        }
        //查询故障决策指令-列车进站过程中站台门打开
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 0);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        int executeStep = alarmInfo.getExecuteStep();
        Integer executeEnd = alarmInfo.getExecuteEnd();
        // 弹出推荐指令但是未执行故障恢复
        if (NumConstant.TWO.equals(executeStep) && NumConstant.ZERO.equals(executeEnd)) {
            switchFailureRecovery.recoveryWithoutExecute(alarmInfo,entities);
            return;
        }
        if (executeStep == 2 || executeStep == -2) {
            admIdea.setExecuteStep(-2);
            alarmInfo.setExecuteStep(-2);
        } else {
            admIdea.setExecuteStep(-3);
            alarmInfo.setExecuteStep(-3);
        }
        admAlertInfoSubService.updateById(alarmInfo);
        // 调用算法获取调整方案列表
        AdmRunGraphCases admRunGraphCases = prePlanRunGraphContext.listPreviewRunGraph(alarmInfo);
        stepList = rebuildStepList(admRunGraphCases, stepList, alarmInfo);
        admIdea.setDispose(stepList);
        //赋值执行单元信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        //推荐指令弹窗时长,由系统参数配置
        Integer countDown = Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.COUNT_DOWN));
        admIdea.setShowSecond(countDown);
        //故障恢复类型——去掉后两位
        int subCode = alarmInfo.getAlarmTypeDetail();
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode((subCode)));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(22, 23));
    }

    private List<DisposeDto> rebuildStepList(AdmRunGraphCases admRunGraphCases,List<DisposeDto> list,AlarmInfo alarmInfo) {
        if (Objects.isNull(admRunGraphCases)) {
            log.error("调用算法获取缓行信息失败");
            return list;
        }
        int executeStep = alarmInfo.getExecuteStep();
        List<RecoveryTrainDto> recoveryTrainDtoList;
        List<RecoverySlowlyTrainDto> recoverySlowlyTrainDtoList;
        String recoveryNumber;
        // 中断恢复
        if (executeStep == -2) {
            recoveryTrainDtoList = admRunGraphCases.getAdmRunGraphCases().get(0).getRecoveryTrainDtoList();
            if (Objects.isNull(recoveryTrainDtoList)) {
                log.info("调用算法返回恢复车次信息为空!");
                return list;
            }
            if (recoveryTrainDtoList.size() == NumConstant.ONE) {
                recoveryNumber = recoveryTrainDtoList.get(0).getRecoveryTrainNumber();
            } else {
                recoveryNumber =
                        StringUtils.join(recoveryTrainDtoList.stream().map(s -> s.getRecoveryTrainNumber() + "(" + s.getUpDown() + ")").collect(Collectors.toList()), "、");
            }
        }
        // -3缓行恢复
        else {
            recoverySlowlyTrainDtoList = admRunGraphCases.getAdmRunGraphCases().get(0).getRecoverySlowlyTrainDtoList();
            if (Objects.isNull(recoverySlowlyTrainDtoList)) {
                log.info("调用算法返回恢复车次信息为空!");
                return list;
            }
            if (recoverySlowlyTrainDtoList.size() == NumConstant.ONE) {
                recoveryNumber = recoverySlowlyTrainDtoList.get(0).getSlowlyRecoveryTrainNumber();
            } else {
                recoveryNumber =
                        StringUtils.join(recoverySlowlyTrainDtoList.stream().map(s -> s.getSlowlyRecoveryTrainNumber() + "(" + s.getUpDown() + ")").collect(Collectors.toList()), "、");
            }

        }
        BasicCommonCacheUtils.hPut(Cache.SLOWLY_RECOVERY_TRAIN_NUMBER, alarmInfo.getSwitchNo(), recoveryNumber);
        List<DisposeDto> newList = new ArrayList<>();
        String finalSlowlyRecoveryTrainNumber = recoveryNumber;
        list.forEach(s -> {
            String step = s.getStep();
            if (step.contains("%s次")) {
                step = step.replace("%s次", finalSlowlyRecoveryTrainNumber + "次");
            }
            s.setStep(step);
            newList.add(s);
        });
        return newList;
    }
}