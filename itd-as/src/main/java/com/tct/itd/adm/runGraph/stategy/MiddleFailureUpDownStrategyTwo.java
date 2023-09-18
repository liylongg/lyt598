package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.constant.SwitchFailureStrategyConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @classname: MiddleFailureUpDownStrategyTwo
 * @description: 线路中间站道岔故障(非小交路折返 - 影响上下行)第二次推荐指令策略类
 * @author: liyunlong
 * @date: 2023/3/2 16:38
 */
@Slf4j
@Service
public class MiddleFailureUpDownStrategyTwo extends SwitchFailureStrategy{

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
        return SwitchFailureStrategyConstant.MIDDLE_FAILURE_UP_DOWN_TWO;
    }
}
