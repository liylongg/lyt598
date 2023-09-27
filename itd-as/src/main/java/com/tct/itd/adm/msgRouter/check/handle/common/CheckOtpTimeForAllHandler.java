package com.tct.itd.adm.msgRouter.check.handle.common;

import com.tct.itd.adm.msgRouter.check.router.CheckConfigHandler;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.SysParamKit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/***
 * @Description 故障录入校验-校验列车早晚点
 * @Author yuelei
 * @Date 2021/12/14 16:02
 */
@Slf4j
@Service("checkOtpTimeForAll")
public class CheckOtpTimeForAllHandler implements CheckConfigHandler {

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        if (StringUtils.hasText(alarmInfo.getTrainId()) && !IidsConstPool.TRAIN_ID_0_1.equals(alarmInfo.getTrainId())) {
            TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent(AlarmUtil.getTrainId(alarmInfo));
            //早晚点偏差时间
            Integer otpTime = tiasTraceInfo.getOtpTime();
            //调图晚点时间影响范围
            Integer otpTime1 = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.OTP_TIME));
            if (otpTime >= otpTime1) {
                throw new BizException(CodeEnum.TRAIN_TOO_LATE_NO_CHANGE_GRAPH);
            }
        }
    }

}
