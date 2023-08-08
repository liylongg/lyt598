package com.tct.itd.common.service;


import com.tct.itd.dto.TrafficParamClassDTO;
import com.tct.itd.dto.TrafficParameterXmlDTO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class TrafficParamClassService   {
    @Resource
    TrafficParameterService parameterService;

    public List<TrafficParamClassDTO> getAll(){
        List<TrafficParamClassDTO> trafficParamClassDTOS=new ArrayList<>();
        Map<String,List<TrafficParameterXmlDTO>> map=parameterService.getSysParamCacheList();
        Iterator<Map.Entry<String, List<TrafficParameterXmlDTO>>> entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String,List<TrafficParameterXmlDTO>> entry = entries.next();
            for (TrafficParameterXmlDTO s :  entry.getValue()) {
                if (s.getHide().equals("1")){
                    TrafficParamClassDTO dto=new TrafficParamClassDTO();
                    dto.setClassification(s.getGroupCode());
                    dto.setName(s.getGroupName());
                    trafficParamClassDTOS.add(dto);
                }
            }
        }
        return trafficParamClassDTOS;

    }

}
