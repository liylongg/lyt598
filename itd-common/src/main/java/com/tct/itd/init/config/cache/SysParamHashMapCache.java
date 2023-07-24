package com.tct.itd.init.config.cache;


import com.tct.itd.dto.TrafficParameterXmlDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description : 系统参数缓存
 * @Author : zhoukun
 * @Date : Created in 2022/07/20
 *
 */
@Slf4j
public class SysParamHashMapCache {

    public  static ConcurrentHashMap<String, List<TrafficParameterXmlDTO>> SYSPAREM_MAP=new ConcurrentHashMap<>();

    public static void add(String key, List<TrafficParameterXmlDTO> sysParam) {
        SYSPAREM_MAP.put(key, sysParam);
    }

    public static List<TrafficParameterXmlDTO> get(String key) {
        return SYSPAREM_MAP.get(key);
    }

}