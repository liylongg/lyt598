package com.tct.itd.adm.msgRouter.check.handle;

import com.tct.itd.adm.msgRouter.check.router.CheckConfigHandler;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.common.constant.FireInfoPushAlertMsgConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.ParseException;

/***
 * @Description 故障录入校验-故障开始时间不能比系统当前时间晚，误差10s以内,提示客户端时间与系统时间不匹配
 * @Author zhoukun
 * @Date 2022/05/06 17:19
 */
@Slf4j
@Service("startAlarmTimeLaterSystemTime")
public class StartAlarmTimeLaterSystemTimeHandler implements CheckConfigHandler {

    @Override
    public void handle(AlarmInfo alarmInfo) {
        try {

            long times = DateUtil.getTime(DateUtil.getTimeStamp(),alarmInfo.getStartAlarmTime());
            if (times>10){
                log.error("无法录入故障，客户端时间与系统时间不匹配{}", alarmInfo.getStartAlarmTime() + ">" + DateUtil.getTimeStamp());
                //故障录入信息失败推送和利时
                AlarmUtil.sendFireInfoPush(alarmInfo, FireInfoPushAlertMsgConstant.STARTTIME_LATER_SYSTEMIME);
                //抛出异常
                throw new BizException(CodeEnum.STARTTIME_LATER_SYSTEMIME);
            }
        } catch (ParseException e) {
            log.error("计算时间相差秒数出错{}",e.getMessage());
        }
    }
}
