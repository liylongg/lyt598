package com.tct.itd.util;

import com.tct.itd.common.cache.Cache;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author kangyi
 * @description 调度命令号工具
 * @date 2022年 09月23日 09:25:00
 */
@Slf4j
public class CommandCodeUtil {

    //保存调度命令

    //调度命令号锁超时时长
    private static final Long OUT_TIME = 2L;

    //调度命令最小值
    private static final String COMMAND_CODE_MIN = "1";
    //调度命令最大值
    private static final String COMMAND_CODE_MAX = "99";

    //获取命令号
    public static String getCommandCode() {
        try {
            if (LockUtil.CMD_INSERT_LOCK.tryLock(OUT_TIME, TimeUnit.SECONDS)) {
                Object o = BasicCommonCacheUtils.get(Cache.DISPATCH_COMMAND_CODE);
                //没有缓存直接初始化
                if (Objects.isNull(o)) {
                    return COMMAND_CODE_MIN;
                }
                String oriCode = (String) o;
                //最大值直接初始化
                if (COMMAND_CODE_MAX.equals(oriCode)) {
                    return COMMAND_CODE_MIN;
                }
                //命令号+1
                return String.valueOf(Integer.parseInt(oriCode) + 1);
            }
        } catch (Exception e) {
            log.error("获取调度命令号异常", e);
        } finally {
            if (LockUtil.CMD_INSERT_LOCK.isHeldByCurrentThread()) {
                LockUtil.CMD_INSERT_LOCK.unlock();
            }
        }
        return COMMAND_CODE_MIN;
    }
}
