package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.dto.AlarmInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Description 列车空调-运行图预览
 * @Author zhangyinglong
 * @Date 2021/6/7 16:38
 */
@Slf4j
@Service
public class BroadcastFailureCanManualStrategy extends AbstractPrePlanRunGraphStrategy {

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        //终点站掉线
        return AlarmStateEnum.TERMINAL_POINT_DROP_LINE.getCode();
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        //故障结束时间为当前时间
        return alarmInfo.getStartAlarmTime();
    }

    @Override
    public String strategy() {
        return AlarmTypeConstant.BROADCAST_FAILURE_CAN_MANUAL;
    }
}
