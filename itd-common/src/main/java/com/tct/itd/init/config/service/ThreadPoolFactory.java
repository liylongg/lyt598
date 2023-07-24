package com.tct.itd.init.config.service;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.*;

/**
 * @classname: ThreadPoolFactory
 * @description: 线程池工厂
 * @author: liyunlong
 * @date: 2022/8/2 11:17
 */
public class ThreadPoolFactory {

    private static final ScheduledExecutorService scheduleExecutorService;

    private static final ExecutorService executorService;

    static {
        scheduleExecutorService = new ScheduledThreadPoolExecutor(10,
                new BasicThreadFactory.Builder().namingPattern("gw-heart-%d").daemon(true).build());

        executorService = new ThreadPoolExecutor(0, 10,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    }

    private ThreadPoolFactory() {
    }


    public static ExecutorService initThreadPool() {
        return executorService;
    }

    public static ScheduledExecutorService initScheduleExecutor() {
        return scheduleExecutorService;
    }
}
