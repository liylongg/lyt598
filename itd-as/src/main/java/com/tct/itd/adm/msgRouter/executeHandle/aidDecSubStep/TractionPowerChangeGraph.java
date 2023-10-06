package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AdmCommonMethodService;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.constant.StringConstant;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description : 接触网失电调整运行图
 * @Author : zhangjiarui
 * @Date : Created in 2022/3/9
 */
@Slf4j
@Service("tractionPowerChangeGraph")
public class TractionPowerChangeGraph implements AidDecSubStepHandler {
    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AdmCommonMethodService admCommonMethodService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        log.info("接触网失电调图时alarmInfo:{}", alarmInfo);
        aidDecisionExecService.changeGraphForPower(alarmInfo);
        //置灰预览图按钮
        admCommonMethodService.disablePreviewButton(alarmInfo);
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
        //第二次推荐指令（非供电设备故障）调图完成后弹出弹窗
           if (BasicCommonCacheUtils.exist(Cache.TRIAL_LINE_CHARGING_FLAG)){
               //查询99弹窗推荐指令内容第二条
               List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), IidsConstPool.EXECUTE_STEP_99, 0);
               String subStepContent = entities.get(1).getSubStepContent();
               // 执行完后立即推出弹窗确认是否试送电成功
               appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.POWER_POP,
                       "0", subStepContent));
               //缓存是否有试送电成功弹框标志
               BasicCommonCacheUtils.set(Cache.TRACTION_POWER_POP_FLAG, StringConstant.TRIAL_LINE_CHARGING_POP);
           }
          //删除缓存
        BasicCommonCacheUtils.delKey(Cache.TRIAL_LINE_CHARGING_FLAG);

    }
}