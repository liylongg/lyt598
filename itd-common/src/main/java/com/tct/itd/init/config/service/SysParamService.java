package com.tct.itd.init.config.service;

import com.tct.itd.common.cache.Cache;
import com.tct.itd.constant.StringConstant;
import com.tct.itd.dto.TrafficParameterXmlDTO;
import com.tct.itd.dto.TrafficParameterXmlList;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.BaseDataCacheUtils;
import com.tct.itd.utils.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author zhoukun
 * @description 读取参数配置xml
 * @since 2022-07-20
 */
@Slf4j
@Service
public class SysParamService {
    @Value("${iids.ini.sysParam}")
    public String sysParam;

    public void readSysParamXmlToDto() {
        log.info("开始读取系统参数");
        Map<String, List<TrafficParameterXmlDTO>> mapCacheList = new HashMap<>();
        StringWriter sw = new StringWriter();
        try {
            //1.读取xml文件转换为字符串
            XmlUtil.readXMLToStrByPath(sysParam, sw);
            //2.将xml字符串转换为对象
            TrafficParameterXmlList list = (TrafficParameterXmlList) XmlUtil.xmlStrToObject(TrafficParameterXmlList.class, sw.toString());
            Map<String, Object> map = new HashMap<>();
            //3.将读出来的xml对象存入Redis缓存
            list.getParameterList().forEach(m -> {
                if (map.containsKey(m.getGroupCode())) {
                    List<TrafficParameterXmlDTO> xmlDTOS = (List<TrafficParameterXmlDTO>) map.get(m.getGroupCode());
                    xmlDTOS.add(m);
                    map.put(m.getGroupCode(), xmlDTOS);
                } else {
                    List<TrafficParameterXmlDTO> parameterList = new ArrayList<>();
                    parameterList.add(m);
                    map.put(m.getGroupCode(), parameterList);
                }
            });
            map.forEach((key,value)->{
                BaseDataCacheUtils.hPut(Cache.SYSTEM_PARAM,key,value);
                for (TrafficParameterXmlDTO dto : (List<TrafficParameterXmlDTO>)value) {
                    if (dto.getCode().equals(StringConstant.SET_DATABASE_MONITOR_INTERVAL)){
                        BaseDataCacheUtils.set(StringConstant.SET_DATABASE_MONITOR_INTERVAL,Long.parseLong(dto.getValue()));
                    }
                    if (dto.getCode().equals(StringConstant.DATABASE_MONITOR_SWITCH)){
                        BaseDataCacheUtils.set(StringConstant.DATABASE_MONITOR_SWITCH,Long.parseLong(dto.getValue()));
                    }
                }

            });
            log.info("系统参数读取成功:{}",map);
        } catch (Exception e) {
            log.error("读取xml异常:", e);
            throw new BizException("读取系统参数错误");
        } finally {
            try {
                sw.close();
            } catch (IOException e) {
                log.error("关流异常:", e);
            }
        }

    }

}
