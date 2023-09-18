package com.tct.itd.adm.runGraph.stategy;


import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.constant.SwitchFailureStrategyConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @classname: SFTerminalBackHasPreStrategyTwo
 * @description: 终端站折返道岔故障，具备本站折返-第二次推荐指令运行图预览策略
 * @author: liyunlong
 * @date: 2021/12/21 17:03
 */
@Slf4j
@Service
public class SFTerminalBackHasPreStrategyTwo extends SwitchFailureStrategy{

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        return super.getAlarmState(alarmInfo);
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        // 道岔故障调30分钟站前折返图
        int switchFailureTime = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.SWITCH_FAILURE_HAS_BACK_TIME));
        return DateUtil.getTimeStamp(System.currentTimeMillis() + switchFailureTime);
    }

    @Override
    public String strategy() {
        return SwitchFailureStrategyConstant.SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE_TWO;
    }
}
