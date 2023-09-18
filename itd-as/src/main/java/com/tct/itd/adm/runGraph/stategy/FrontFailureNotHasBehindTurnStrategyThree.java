package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.constant.SwitchFailureStrategyConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @classname: FrontFailureNotHasBehindTurnStrategyThree
 * @description: 道岔故障-线路终点站常态化站前折返道岔故障-不具备站后折返第三次推荐指令预览图策略类
 * @author: liyunlong
 * @date: 2021/12/28 18:11
 */
@Slf4j
@Service
public class FrontFailureNotHasBehindTurnStrategyThree extends SwitchFailureStrategy{

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
        return SwitchFailureStrategyConstant.FRONT_FAILURE_NOT_HAS_BEHIND_TURN_THREE;
    }
}
