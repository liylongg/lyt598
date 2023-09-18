package com.tct.itd.adm.runGraph.stategy;


import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.constant.SwitchFailureStrategyConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @classname: SFMidSingleStrategyTwo
 * @description: 道岔故障-中间道岔故障-影响单行第二次推荐指令预览图策略类
 * @author: liyunlong
 * @date: 2021/12/29 11:29
 */
@Slf4j
@Service
public class SFMidSingleStrategyTwo extends SwitchFailureStrategy{

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
        return SwitchFailureStrategyConstant.SWITCH_FAILURE_MIDDLE_SINGLE_TWO;
    }
}
