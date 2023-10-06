package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description : 接触网失电最后一步恢复调整运行图
 * @Author : zhangjiarui
 * @Date : Created in 2022/3/9
 */
@Slf4j
@Service("tractionPowerRecoveryChangeGraph")
public class TractionPowerRecoveryChangeGraph implements AidDecSubStepHandler {
    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AdmCommonMethodService admCommonMethodService;
    @Resource
    private AppPushService appPushService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        // 故障结束时间为当前时间
        alarmInfo.setEndAlarmTime(DateUtil.getTimeStamp(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss.SSS"));
        log.info("接触网失电恢复调图时alarmInfo:{}", alarmInfo);
        aidDecisionExecService.changeGraphForPower(alarmInfo);
        // 调图完成后置灰运行图预览
        admCommonMethodService.disablePreviewButton(alarmInfo);
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
    }
}