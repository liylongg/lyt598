package com.tct.itd.init.config.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @Description : ip配置表
 * @Author : zhoukun
 * @Date : Created in 2022/05/19
 */
@Data
public class DeviceConfigExcelModel  {
    /**
     * 设备类型
     */
    @ExcelProperty(index = 0)
    private String type;
    /**
     * 设备名称
     */
    @ExcelProperty(index = 1)
    private String name;

    /**
     * 车站id
     */
    @ExcelProperty(index = 2)
    private String stationId;

    /**
     * 设备源
     */
    @ExcelProperty(index = 3)
    private String source;

    /**
     * 长连接-紫
     */
    @ExcelProperty(index = 4)
    private String portLong;
    /**
     * 短连接-紫
     */
    @ExcelProperty(index = 5)
    private String portShort;

    /**
     * ip-紫
     */
    @ExcelProperty(index = 6)
    private String ipAddressP;

   /**
    * 长连接-黄
    */
   @ExcelProperty(index = 7)
   private String portLongY;
   /**
    * 短连接-黄
    */
   @ExcelProperty(index = 8)
   private String portShortY;

    /**
     * ip-黄
     */
    @ExcelProperty(index = 9)
    private String ipAddressY;
}