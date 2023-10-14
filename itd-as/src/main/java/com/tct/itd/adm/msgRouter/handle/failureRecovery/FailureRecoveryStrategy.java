package com.tct.itd.adm.msgRouter.handle.failureRecovery;


import com.tct.itd.dto.AlarmInfo;

/**
 * @Description : 推送故障恢复推荐指令的抽象策略角色 Strategy
 * @Author : zhangjiarui
 * @Date : Created in 2021/12/23
 */
public interface FailureRecoveryStrategy {

    /**
     * 推送故障恢复推荐指令
     * @param alarmInfo alarmInfo
     */
    void pushRecoveryAdm(AlarmInfo alarmInfo);
}
