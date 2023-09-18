package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.constant.SwitchFailureStrategyConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @classname: MiddleFailureSingleStrategyTwo
 * @description: 线路中间站道岔故障(非小交路折返站 - 影响单行)
 * @author: liyunlong
 * @date: 2023/3/1 20:15
 */
@Slf4j
@Service
public class MiddleFailureSingleStrategyTwo extends SwitchFailureStrategy{

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        return super.getAlarmState(alarmInfo);
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        // 道岔故障调30分钟中断图
        int switchFailureTime = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.SWITCH_FAILURE_PRRVIEW_TIME));
        return DateUtil.getTimeStamp(System.currentTimeMillis() + switchFailureTime);
    }

    @Override
    public String strategy() {
        return SwitchFailureStrategyConstant.MIDDLE_FAILURE_SINGLE_TWO;
    }
}
