package com.tct.itd.common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.dto.TrafficParamDTO;
import com.tct.itd.dto.TrafficParameterDTO;
import com.tct.itd.dto.TrafficParameterXmlDTO;
import com.tct.itd.dto.TrafficParameterXmlList;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.init.config.service.SysParamService;
import com.tct.itd.utils.BaseDataCacheUtils;
import com.tct.itd.utils.ParamValidateUtil;
import com.tct.itd.utils.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TrafficParameterService{
    @Resource
    TrafficParamClassService trafficParamClassService;
    @Resource
    SysParamService sysParamService;
    @Value("${iids.ini.sysParam}")
    public String sysParam;

    //根据code从redis中找对应的value
    public String findValue(String code) {
        Map<String,List<TrafficParameterXmlDTO>> map=getSysParamCacheList();
        Iterator<Map.Entry<String, List<TrafficParameterXmlDTO>>> entries = map.entrySet().iterator();
        String returnValue="";
        while (entries.hasNext()) {
            Map.Entry<String,List<TrafficParameterXmlDTO>> entry = entries.next();
            for (TrafficParameterXmlDTO s :  entry.getValue()) {
                if (s.getCode().equals(code)){
                    returnValue=s.getValue();
                    break;
                }
            }
        }
        return returnValue;

    }

    public Page<TrafficParameterDTO> findByNameAndClass(Integer page, Integer size, String type, String name) {
        List<TrafficParameterDTO> trafficParameterDTOS=new ArrayList<>();
        Map<String,List<TrafficParameterXmlDTO>> map=getSysParamCacheList();
        Iterator<Map.Entry<String, List<TrafficParameterXmlDTO>>> entries = map.entrySet().iterator();
        String returnValue="";
        while (entries.hasNext()) {
            Map.Entry<String,List<TrafficParameterXmlDTO>> entry = entries.next();
            for (TrafficParameterXmlDTO s :  entry.getValue()) {
                if ((type.equals("")||s.getGroupCode().equals(type))
                    &&(name.equals("")||s.getName().contains(name))
                    &&s.getHide().equals("1")){
                    TrafficParameterDTO dto=new TrafficParameterDTO();
                    dto.setName(s.getName());
                    dto.setCode(s.getCode());
                    dto.setValue(s.getValue());
                    dto.setValType(s.getValueType());
                    dto.setClassName(s.getGroupName());
                    dto.setClassification(s.getGroupCode());
                    dto.setAnnotation(s.getAnnotation());
                    trafficParameterDTOS.add(dto);
                }
            }
        }
        int pageNum=(page-1)*size;
        //2.按classification排序
        trafficParameterDTOS.sort(Comparator.comparing(TrafficParameterDTO::getClassification));
        //3.根据page和size封装返回体
        List<TrafficParameterDTO> trafficParameterPageDtos = trafficParameterDTOS.stream().skip(pageNum).limit(size).collect(Collectors.toList());
        Page<TrafficParameterDTO> parameterDTOPageInfo = new Page<>(page,size);
        //总条数
        parameterDTOPageInfo.setTotal(trafficParameterDTOS.size());
        //返回内容
        parameterDTOPageInfo.setRecords(trafficParameterPageDtos);
        //总页数
        parameterDTOPageInfo.setPages(parameterDTOPageInfo.getPages());
        return parameterDTOPageInfo;
    }

    //根据分组code获取对应系统参数
    public Map<String, TrafficParamDTO> findListByClassification(String classification) {
        List<TrafficParamDTO> trafficParamDTOS=new ArrayList<>();
        Map<String,List<TrafficParameterXmlDTO>> map=getSysParamCacheList();
        Iterator<Map.Entry<String, List<TrafficParameterXmlDTO>>> entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String,List<TrafficParameterXmlDTO>> entry = entries.next();
            if (entry.getKey().equals("SYSTEM_PARAM:"+classification)){
                for (TrafficParameterXmlDTO s :  entry.getValue()) {
                    TrafficParamDTO trafficParamDTO=new TrafficParamDTO();
                    trafficParamDTO.setValue(s.getValue());
                    trafficParamDTO.setClassification(s.getGroupCode());
                    trafficParamDTO.setName(s.getName());
                    trafficParamDTO.setAnnotation(s.getAnnotation());
                    trafficParamDTO.setCode(s.getCode());
                    trafficParamDTOS.add(trafficParamDTO);
                }
            }
        }
        Map<String,TrafficParamDTO> trafficParamDTOMap = trafficParamDTOS.stream().collect(Collectors.toMap(TrafficParamDTO::getCode, Function.identity()));
        return trafficParamDTOMap;

    }

    public Integer updateByCode(String name, String annotation, String value, String code, String classification) {
        //根据参数分类类型添加校验
//        validateParam(code, value);
     try {
        //1.将xml读取到内存中
        StringWriter sw = new StringWriter();
        XmlUtil.readXMLToStrByPath(sysParam, sw);
        //2.将xml字符串转换为对象
        List<TrafficParameterXmlDTO> parameterList = new ArrayList<>();
        TrafficParameterXmlList xmlList = (TrafficParameterXmlList) XmlUtil.xmlStrToObject(TrafficParameterXmlList.class, sw.toString());
        //3.修改系统参数
         xmlList.getParameterList().forEach(p->{
             if (p.getCode().equals(code)){
                 p.setAnnotation(annotation);
                 p.setValue(value);
             }
         });
         //4.java对象转换为流输出为xml文件
        String convertToXml = XmlUtil.convertToXml(xmlList);
        BufferedWriter bfw = new BufferedWriter(new FileWriter(sysParam));
        bfw.write(convertToXml);
        bfw.close();
        } catch (Exception e) {
            log.error("系统参数转xml出错:", e);
            throw new BizException("更新系统参数xml失败");
        }finally {
         sysParamService.readSysParamXmlToDto();
        }
         return 1;
    }

    //校验参数值
    private void validateParam(String code, String value) {
        if (StringUtils.isEmpty(code) || StringUtils.isEmpty(value)) {
            return;
        };
        TrafficParamDTO paramDTO=new TrafficParamDTO();
        Map<String,List<TrafficParameterXmlDTO>> map=getSysParamCacheList();
        Iterator<Map.Entry<String, List<TrafficParameterXmlDTO>>> entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String,List<TrafficParameterXmlDTO>> entry = entries.next();
                for (TrafficParameterXmlDTO s :  entry.getValue()) {
                     if (s.getCode().equals(code)){
                         paramDTO.setValType(s.getValueType());
                         paramDTO.setCode(s.getCode());
                         paramDTO.setName(s.getName());
                     }
                }
        }

        if (Objects.isNull(paramDTO) || Objects.isNull(paramDTO.getValType())) {
            return;
        }
        String valType = paramDTO.getValType();
        switch (valType) {
            case "2"://时间类型HH:mm:ss
                if (!ParamValidateUtil.isTimeStr(value)) {
                    throw new BizException(CodeEnum.NOT_TIME_VALUE);
                }
                break;
            case "3"://数字格式
                if (!ParamValidateUtil.IsNumber(value)) {
                    throw new BizException(CodeEnum.NOT_NUMBER_VALUE);
                }
                break;
            case "4"://ip类型
                if (!ParamValidateUtil.isIpStr(value)) {
                    throw new BizException(CodeEnum.NOT_IP_VALUE);
                }
                break;
            default:
                return;
        }
    }

    public Page<TrafficParameterDTO>  getAll(Integer page,Integer size) {
        //1.查询出全部数据
        List<TrafficParameterDTO> trafficParameterDTOS=new ArrayList<>();
        Map<String,List<TrafficParameterXmlDTO>> map=getSysParamCacheList();
        Iterator<Map.Entry<String, List<TrafficParameterXmlDTO>>> entries = map.entrySet().iterator();
        String returnValue="";
        while (entries.hasNext()) {
            Map.Entry<String,List<TrafficParameterXmlDTO>> entry = entries.next();
            for (TrafficParameterXmlDTO s :  entry.getValue()) {
                    TrafficParameterDTO dto=new TrafficParameterDTO();
                    dto.setName(s.getName());
                    dto.setCode(s.getCode());
                    dto.setValue(s.getValue());
                    dto.setValType(s.getValueType());
                    dto.setClassName(s.getGroupName());
                    dto.setClassification(s.getGroupCode());
                    dto.setAnnotation(s.getAnnotation());
                    trafficParameterDTOS.add(dto);

            }
        }
        int pageNum=(page-1)*size;
        //2.按classification排序
        trafficParameterDTOS.sort(Comparator.comparing(TrafficParameterDTO::getClassification));
        //3.根据page和size封装返回体
        List<TrafficParameterDTO> trafficParameterPageDtos=trafficParameterDTOS.stream().skip(pageNum).limit(size).collect(Collectors.toList());
        Page<TrafficParameterDTO> parameterDTOPageInfo=new Page<>();
        //返回内容
        parameterDTOPageInfo.setRecords(trafficParameterPageDtos);
        //每页条数
        parameterDTOPageInfo.setSize(size);
        parameterDTOPageInfo.setTotal(trafficParameterPageDtos.size());
        //总页数
        parameterDTOPageInfo.setPages(parameterDTOPageInfo.getPages());
        return parameterDTOPageInfo;
    }

    /**
     * 获取系统参数全部
     * @return
     */
        public  Map<String,List<TrafficParameterXmlDTO>> getSysParamCacheList() {
            Map<Object,Object> map= BaseDataCacheUtils.hmget(Cache.SYSTEM_PARAM);
            Map<String,List<TrafficParameterXmlDTO>> mapString = new HashMap<>();
            for (Object key : map.keySet()) {
                String k = key.toString();
                List<TrafficParameterXmlDTO>  v = (List<TrafficParameterXmlDTO>)map.get(key);
                mapString.put(k, v);
            }
           return mapString;
    }


}
