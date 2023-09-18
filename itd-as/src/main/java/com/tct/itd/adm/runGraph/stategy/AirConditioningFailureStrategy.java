package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @Description 列车空调-运行图预览
 * @Author zhangyinglong
 * @Date 2021/6/7 16:38
 */
@Slf4j
@Service
public class AirConditioningFailureStrategy extends AbstractPrePlanRunGraphStrategy {

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        if (alarmInfo.getAlarmState() == AlarmStateEnum.REVERSE_RAIL.getCode()) {
            return AlarmStateEnum.REVERSE_RAIL.getCode();
        }
        //终点站掉线
        return AlarmStateEnum.TERMINAL_POINT_DROP_LINE.getCode();
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        //故障结束时间为当前时间
        Date startAlarmTime = DateUtil.getStringToDate(alarmInfo.getStartAlarmTime(), "yyyy-MM-dd HH:mm:ss.SSS");
        return DateUtil.getTimeStamp(startAlarmTime.getTime() + 10000, "yyyy-MM-dd HH:mm:ss.SSS");
    }

    @Override
    public String strategy() {
        return AlarmTypeConstant.AIR_CONDITIONING_FAILURE;
    }
}
