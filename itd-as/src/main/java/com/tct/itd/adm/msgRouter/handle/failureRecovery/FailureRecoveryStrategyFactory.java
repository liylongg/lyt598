package com.tct.itd.adm.msgRouter.handle.failureRecovery;

import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * @Description : 推送故障恢复推荐指令的Context封装角色
 * @Author : zhangjiarui
 * @Date : Created in 2021/12/23
 */
@Slf4j
public class FailureRecoveryStrategyFactory {
    private static final String PUSH_RECOVERY = "pushRecovery";

    public static FailureRecoveryStrategy getStrategy(AlarmInfo alarmInfo){
        Optional.ofNullable(alarmInfo).orElseThrow(() -> new BizException("FailureRecoveryStrategy获得alarmInfo为空!"));
        // 推送故障恢复的类component以如下所示命名
        String beanName = PUSH_RECOVERY.concat(String.valueOf(alarmInfo.getAlarmTypeDetail()));

        return (FailureRecoveryStrategy) SpringContextUtil.getBean(beanName);
    }
}