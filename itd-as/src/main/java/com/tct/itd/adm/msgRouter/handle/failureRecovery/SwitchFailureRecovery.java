package com.tct.itd.adm.msgRouter.handle.failureRecovery;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.Info;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @classname: SwitchFailureRecovery
 * @description: 道岔故障恢复实体类
 * @author: liyunlong
 * @date: 2022/7/21 19:41
 */
@Component
@Slf4j
public class SwitchFailureRecovery {

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AppPushService appPushService;

    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;


    /**
     * 未执行推荐指令故障恢复
     * @author liyunlong
     * @date 2022/7/21 19:14
     * @param alarmInfo 告警信息
     * @param entities  推荐指令指令
     */
    public void recoveryWithoutExecute(AlarmInfo alarmInfo, List<AidDesSubStepEntity> entities) {
        alarmInfo.setExecuteStep(-2);
        // 表示未执行推荐指令,但是故障就恢复,借用此字段
        alarmInfo.setAutoReport(Boolean.TRUE);
        admAlertInfoSubService.updateById(alarmInfo);

        List<AidDesSubStepEntity> entityList = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), -1, 1);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entityList, alarmInfo);
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setExecuteStep(-2);
        admIdea.setDispose(stepList);
        //赋值执行单元信息
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entityList));
        // 推荐指令弹窗倒计时,通过系统参数配置
        Integer countDown = Integer.valueOf(SysParamKit.getByCode(IidsSysParamPool.COUNT_DOWN));
        admIdea.setShowSecond(countDown);
        int subCode = alarmInfo.getAlarmTypeDetail();
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode((subCode)));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        log.info("推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(9, 11));
    }

}
