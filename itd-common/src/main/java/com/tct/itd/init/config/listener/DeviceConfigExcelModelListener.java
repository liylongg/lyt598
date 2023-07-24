package com.tct.itd.init.config.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.github.benmanes.caffeine.cache.Cache;
import com.tct.itd.constant.StringConstant;
import com.tct.itd.dto.DeviceConfigDto;
import com.tct.itd.dto.PusrpleIpAddressDto;
import com.tct.itd.dto.YellowIpAddressDto;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.enums.DeviceConfigEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.init.config.cache.DeviceConfigHashMapCache;
import com.tct.itd.init.config.model.DeviceConfigExcelModel;
import com.tct.itd.model.IpConfig;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.IpUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * @Description : 读取配置表监听器
 * @Author : zhoukun
 * @Date : Created in 2022/05/19
 */
@Slf4j
public class DeviceConfigExcelModelListener extends AnalysisEventListener<DeviceConfigExcelModel> {


    @Override
    public void invoke(DeviceConfigExcelModel m, AnalysisContext analysisContext) {
        List<DeviceConfigDto> deviceConfigDtos = new ArrayList<>();
        //将从excel中读取到的每一行数据处理为对象存入缓存
        DeviceConfigDto deviceConfigDto = handleData(m);
        deviceConfigDtos.add(deviceConfigDto);
        if (DeviceConfigHashMapCache.DEVICECONFIG_MAP.containsKey(Integer.parseInt(m.getType()))) {
            DeviceConfigHashMapCache.DEVICECONFIG_MAP.get(Integer.parseInt(m.getType())).add(deviceConfigDto);
        } else {
            DeviceConfigHashMapCache.DEVICECONFIG_MAP.put(Integer.parseInt(m.getType()), deviceConfigDtos);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        log.info("读取到的配置表数据:{}", JsonUtils.toJSONString(DeviceConfigHashMapCache.DEVICECONFIG_MAP));
        Set set = DeviceConfigHashMapCache.DEVICECONFIG_MAP.entrySet();
//        for(Object key:set){
//            Map.Entry entry = (Map.Entry) key;
//            //读取设备配置表按type存入缓存
//            deviceConfigCache.put((Integer) entry.getKey(), (List<DeviceConfigDto>)entry.getValue());
//            log.info("配置表数据type:{},内容:{}",  JsonUtils.toJSONString(entry.getKey()),JsonUtils.toJSONString(entry.getValue()));
//        }
//        log.info("缓存中的配置表数据:{}", deviceConfigCache);
        //初始化httpIP
        try {
            List<DeviceConfigDto> localDeviceList = DeviceConfigHashMapCache.get(DeviceConfigEnum.BACKGROUND_APPLICATION_SERVER.getType());
            if (CollectionUtils.isEmpty(localDeviceList)) {
                throw new BizException(CodeEnum.GET_ITD_IP_ERROR);
            }
            List<String> ipList = new ArrayList<>();
            localDeviceList.stream().forEach(deviceConfigDto -> {
                if (!ipList.contains(deviceConfigDto.getYellowIpAddressDto().getIpAddress())) {
                    ipList.add(deviceConfigDto.getYellowIpAddressDto().getIpAddress());
                }
                if (!ipList.contains(deviceConfigDto.getPusrpleIpAddressDto().getIpAddress())) {
                    ipList.add(deviceConfigDto.getPusrpleIpAddressDto().getIpAddress());
                }
            });
            if (org.springframework.util.CollectionUtils.isEmpty(ipList)) {
                log.debug("当前无请求ip");
                return;
            }
            IpUtil.HTTP_IP.addAll(ipList);
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(3000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //获取所有智能调度后台服务IP集合
                    //该适配器所在服务器调度后台服务ip集合
                    Set<String> localIpSet = getIpSet(ipList);
                    if (org.springframework.util.CollectionUtils.isEmpty(localIpSet)) {
                        log.debug("本机后台服务ip不存在");
                        return;
                    }
                    List<String> tempIpSet = new ArrayList<>();
                    //ping不通的网络ip删除
                    for (String ip : ipList) {
                        try {
                            if (InetAddress.getByName(ip).isReachable(2000)) {
                                tempIpSet.add(ip);
                            }
                        } catch (IOException e) {
                            log.error("{}不通", ip);
                        }
                    }
                    if (org.springframework.util.CollectionUtils.isEmpty(tempIpSet)) {
                        log.debug("当前无有效的请求ip");
                        return;
                    }
                    if (!ipList.equals(tempIpSet)) {
                        IpUtil.HTTP_IP.clear();
                        IpUtil.HTTP_IP.addAll(tempIpSet);
                    }
                }
            }).start();
        } catch (Exception e) {
            log.error("初始化HTTP_IP失败", e);
        }

    }

    private Set<String> getIpSet(List<String> allIpSet) {
        Set<String> retSet = new HashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresss = ni.getInetAddresses();
                while (addresss.hasMoreElements()) {
                    InetAddress nextElement = addresss.nextElement();
                    String hostAddress = nextElement.getHostAddress();
                    for (String ip : allIpSet) {
                        if (ip.equals(hostAddress)) {
                            retSet.add(ip);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取系统配置的网络IP信息失败！", e);
            return retSet;
        }
        return retSet;
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        log.error("解析失败，但是继续解析下一行:{}", exception.getMessage());

        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException excelDataConvertException = (ExcelDataConvertException) exception;
            log.error("第{}行，第{}列解析异常", excelDataConvertException.getRowIndex(),
                    excelDataConvertException.getColumnIndex());
        }
    }

    public DeviceConfigDto handleData(DeviceConfigExcelModel m) {
        DeviceConfigDto deviceConfigDto = new DeviceConfigDto();
        if (!(m.getType().contains("/") || m.getType().contains("..."))) {
            deviceConfigDto.setType(Integer.parseInt(m.getType()));
        }
        if (!(m.getName().contains("/") || m.getName().contains("..."))) {
            deviceConfigDto.setName(m.getName());
        }
        if (!(m.getSource().contains("/") || m.getSource().contains("..."))) {
            deviceConfigDto.setSource(m.getSource());
        }
        if (!(m.getStationId().contains("/") || m.getStationId().contains("..."))) {
            deviceConfigDto.setStationId(Integer.parseInt(m.getStationId()));
        }

        PusrpleIpAddressDto pusrpleIpAddressDto = new PusrpleIpAddressDto();
        if (!(m.getPortLong().contains("/") || m.getPortLong().contains("..."))) {
            pusrpleIpAddressDto.setPortLong(Integer.parseInt(m.getPortLong()));
        }
        if (!(m.getPortShort().contains("/") || m.getPortShort().contains("..."))) {
            pusrpleIpAddressDto.setPortShort(Integer.parseInt(m.getPortShort()));
        }
        if (!(m.getIpAddressP().contains("/") || m.getIpAddressP().contains("..."))) {
            pusrpleIpAddressDto.setIpAddress(m.getIpAddressP());
        }
        deviceConfigDto.setPusrpleIpAddressDto(pusrpleIpAddressDto);


        YellowIpAddressDto yellowIpAddressDto = new YellowIpAddressDto();
        if (!(m.getPortLongY().contains("/") || m.getPortLongY().contains("..."))) {
            yellowIpAddressDto.setPortLong(Integer.parseInt(m.getPortLongY()));
        }
        if (!(m.getPortShortY().contains("/") || m.getPortShortY().contains("..."))) {
            yellowIpAddressDto.setPortShort(Integer.parseInt(m.getPortShortY()));
        }
        if (!(m.getIpAddressY().contains("/") || m.getIpAddressY().contains("..."))) {
            yellowIpAddressDto.setIpAddress(m.getIpAddressY());
        }
        deviceConfigDto.setYellowIpAddressDto(yellowIpAddressDto);
        return deviceConfigDto;
    }
}