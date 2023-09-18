package com.tct.itd.adm.runGraph.stategy;


import com.tct.itd.common.cache.Cache;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.constant.SwitchFailureStrategyConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @classname: SFTerminalBackHasPreStrategyThree
 * @description: 终端站折返道岔故障，具备本站折返-第三次推荐指令运行图预览策略
 * @author: liyunlong
 * @date: 2021/12/21 17:03
 */
@Slf4j
@Service
public class SFTerminalBackHasPreStrategyThree extends SwitchFailureStrategy{

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        return super.getAlarmState(alarmInfo);
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        String endAlarmTime;
        if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
            endAlarmTime = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
        } else {
            long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
            endAlarmTime = DateUtil.getTimeStamp(System.currentTimeMillis() + setRedundancyTime);
            BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endAlarmTime);
        }
        return endAlarmTime;
    }

    @Override
    public String strategy() {
        return SwitchFailureStrategyConstant.SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE_THREE;
    }
}
