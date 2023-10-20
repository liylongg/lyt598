package com.tct.itd.adm.util;

import com.tct.itd.common.cache.Cache;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description : 项目Redis中的整体操作
 * @Author : zhangjiarui
 * @Date : Created in 2021/8/26
 */
@Slf4j
public class RedisKit {
    /**
     * 清理redis中推荐指令所有临时产生的缓存,一般故障完全放弃时使用
     */
    public static void  deleteADMRedisKey(){
        //扣车和抬车相关缓存,执行第一步推荐指令存储
        log.info("删除抬车扣车缓存--deleteADMRedisKey");
        BasicCommonCacheUtils.delKey(Cache.HOLD_AND_OFF_TRAIN_TIME);
    }

    /**
     * 生命周期结束时，删除缓存
     */
    public static void endLifeDeleteRedis(){
        BasicCommonCacheUtils.delKey(Cache.END_LIFE);
        BasicCommonCacheUtils.delKey(Cache.HOLD_TRAIN_STATION);
    }
}