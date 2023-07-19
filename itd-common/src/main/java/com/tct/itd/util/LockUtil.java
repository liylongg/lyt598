package com.tct.itd.util;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author kangyi
 * @description 锁工具类
 * @date 2022年 11月25日 09:02:19
 */
public class LockUtil {

    /**
     * 发送消息记录锁key
     */
    public static final ReentrantLock SEND_COMMAND_INFO_KEY = new ReentrantLock();

    /**
     * mvc拦截器锁
     */
    public static final ReentrantLock MVC_INTERCEPTOR = new ReentrantLock();

    /**
     * 调度命令号锁
     */
    public static final ReentrantLock CMD_INSERT_LOCK = new ReentrantLock();


}
