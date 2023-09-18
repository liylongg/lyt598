package com.tct.itd.adm.runGraph.stategy;


import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.constant.SwitchFailureStrategyConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @classname: BehindFailureNotHasFrontTurnStrategyThree
 * @description: 终端站折返道岔故障, 不具备本站折返, 第三次推荐指令运行图预览策略
 * @author: liyunlong
 * @date: 2021/12/24 10:05
 */
@Slf4j
@Service
public class BehindFailureNotHasFrontTurnStrategyThree extends SwitchFailureStrategy{

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
        return SwitchFailureStrategyConstant.BACK_FAILURE_NOT_HAS_FRONT_TURN_THREE;
    }
}
