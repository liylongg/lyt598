package com.tct.itd.adm.msgRouter.router;

import com.tct.itd.dto.AlarmInfo;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;

/**
 * @Description 抽象消息处理器
 * @Author zhaoke
 * @Date 2020/6/2 10:14
 **/
public interface AlarmInfoMessageHandler {
    /**
     * 消息处理方法
     * @param alarmInfo 故障信息
     */
    void handle(AlarmInfo alarmInfo);

    /**
     * 定义故障处理类型
     * @return 故障处理类型
     */
    String channel();
}
