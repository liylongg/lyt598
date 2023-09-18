package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.constant.SwitchFailureStrategyConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @classname: BehindFailureChangeStrategyThree
 * @description: 终端站后道岔故障可换轨，第三次推荐指令运行图策略
 * @author: liyunlong
 * @date: 2023/3/21 10:08
 */
@Slf4j
@Service
public class BehindFailureChangeStrategyThree extends SwitchFailureStrategy{

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        return super.getAlarmState(alarmInfo);
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        // 道岔故障调60分钟中断图
        int switchFailureTime = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.SWITCH_FAILURE_THIRTY_PRRVIEW_TIME));
        return DateUtil.getTimeStamp(System.currentTimeMillis() + switchFailureTime, "yyyy-MM-dd HH:mm:ss.SSS");
    }

    @Override
    public String strategy() {
        return SwitchFailureStrategyConstant.BEHIND_FAILURE_CHANGE_THREE;
    }
}
