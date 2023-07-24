package com.tct.itd.init.config.cache;

import com.tct.itd.dto.DeviceConfigDto;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description : 数据源缓存(用于切换数据源)
 * @Author : zhoukun
 * @Date : Created in 2022/05/24
 */
@Slf4j
public class DataSourceHashMapCache {

    public  static ConcurrentHashMap<Integer, HikariDataSource> DATASOURCE_MAP=new ConcurrentHashMap<>();

    public static void add(Integer key, HikariDataSource dataSource) {
        DATASOURCE_MAP.put(key, dataSource);
    }

    public static HikariDataSource get(Integer key) {
        return DATASOURCE_MAP.get(key);
    }

}