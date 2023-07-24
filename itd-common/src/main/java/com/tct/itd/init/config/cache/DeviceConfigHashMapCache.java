package com.tct.itd.init.config.cache;

import com.tct.itd.dto.DeviceConfigDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
/**
 * @Description : ip配置缓存（用户缓存配置excel表数据）
 * @Author : zhoukun
 * @Date : Created in 2022/05/24
 */
@Slf4j
public class DeviceConfigHashMapCache {

    public  static ConcurrentHashMap<Integer,List<DeviceConfigDto>> DEVICECONFIG_MAP=new ConcurrentHashMap<>();

    public static void add(Integer key, List<DeviceConfigDto> deviceConfigDtos) {
        DEVICECONFIG_MAP.put(key, deviceConfigDtos);
    }

    public static List<DeviceConfigDto> get(Integer key) {
        return DEVICECONFIG_MAP.get(key);
    }

}