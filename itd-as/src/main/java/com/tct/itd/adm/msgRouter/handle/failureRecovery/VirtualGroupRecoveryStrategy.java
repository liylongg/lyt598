package com.tct.itd.adm.msgRouter.handle.failureRecovery;

import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.iconstant.DesSubStepBeanConstant;
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
 * @Description: 虚拟编组故障恢复策略
 * @Author: wangsijin
 * @Date: 2023/2/14 014 17:08
 */

@Slf4j
@Component("pushRecovery7")
public class VirtualGroupRecoveryStrategy implements FailureRecoveryStrategy{

    private static final String DELETEHOLDTRAIN ="deleteHoldTrain";

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;

    @Resource
    private HoldTrainUtil holdTrainUtil;

    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AppPushService appPushService;
    
    @Override
    public void pushRecoveryAdm(AlarmInfo alarmInfo) {
        Assert.notNull(alarmInfo,"该故障生命周期已结束");
        alarmFlowchartService.setExecuteFlag(alarmInfo,9);
        log.info("虚拟编组，开始执行故障恢复策略,alarmInfo:{}", alarmInfo);
        //推荐指令步骤
        int step = IidsConstPool.EXECUTE_STEP_0_1;
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 0);
        //检查列车是否晚点2分钟
        holdTrainUtil.checkTrainIsLate(alarmInfo);
        //如果晚点不超过2分钟，不调图，剔除调图步骤
        if (!BasicCommonCacheUtils.exist(Cache.TWO_MINUTES_LATE_RECOVERY_CHANGE_GRAPH)){
            log.info("虚拟编组第一次推荐指令恢复时，列车晚点未超过两分钟，不调图");
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
        //获取调度命令对象
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setExecuteStep(step);
        //赋值执行单元信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
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
    }
}
