package com.tct.itd.init.config.service;

import com.alibaba.excel.EasyExcel;
import com.tct.itd.init.config.listener.DeviceConfigExcelModelListener;
import com.tct.itd.init.config.model.DeviceConfigExcelModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @description 读取excel配置表
 * @author zhoukun
 * @since 2022-05-19
 */
@Slf4j
@Service
public class DeviceConfigExcelService  {

    @Value("${iids.ini.deviceConfig}")
    public String devicePath;

    public void readDeviceConfigToDto(){
        try {
            EasyExcel.read(devicePath, DeviceConfigExcelModel.class, new DeviceConfigExcelModelListener()).sheet().headRowNumber(2).doRead();
        }catch (Exception e){
            log.error("设备配置EXCEL文件读取异常:{}",e.getMessage());
        }

    }

}
