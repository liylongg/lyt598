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
 * @classname: FrontFailureNotHasBehindTurnStrategyThree
 * @description: 道岔故障-线路终点站常态化站前折返道岔故障-不具备站后折返第四次推荐指令预览图策略类
 * @author: liyunlong
 * @date: 2021/12/28 18:11
 */
@Slf4j
@Service
public class FrontFailureNotHasBehindTurnStrategyFour extends SwitchFailureStrategy{

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
        return SwitchFailureStrategyConstant.FRONT_FAILURE_NOT_HAS_BEHIND_TURN_FOUR;
    }
}
