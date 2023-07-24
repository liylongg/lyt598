package com.tct.itd.init.datasource;


import com.alibaba.excel.EasyExcel;
import com.tct.itd.dto.DeviceConfigDto;
import com.tct.itd.dto.DeviceIpPortDto;
import com.tct.itd.enums.DeviceConfigEnum;
import com.tct.itd.init.config.cache.DataSourceHashMapCache;
import com.tct.itd.init.config.cache.DeviceConfigHashMapCache;
import com.tct.itd.init.config.listener.DeviceConfigExcelModelListener;
import com.tct.itd.init.config.model.DeviceConfigExcelModel;
import com.tct.itd.utils.BaseDataCacheUtils;
import com.tct.itd.utils.DataSourceConfigUtil;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description : 动态数据源配置
 * @Author : zhoukun
 * @Date : Created in 2022/5/24
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
public class DynamicDataSourceConfig {
    @Resource
    private DynamicDataSourceProperties dynamicDataSourceProperties;
    @Value("${iids.ini.deviceConfig}")
    public String devicePath;


    @Bean
    public AbstractRoutingDataSource dataSource(Environment env) {
        // 实现AbstractRoutingDataSource的determineCurrentLookupKey方法,该方法会返回当前要使用的数据源对应的dsKey
        AbstractRoutingDataSource abstractRoutingDataSource = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                if (!BaseDataCacheUtils.exist(com.tct.itd.common.cache.Cache.HUB_DATASOURCE_KEY)) {
                    BaseDataCacheUtils.set(com.tct.itd.common.cache.Cache.HUB_DATASOURCE_KEY, "0");
                    return "0";
                }
                return BaseDataCacheUtils.get(com.tct.itd.common.cache.Cache.HUB_DATASOURCE_KEY).toString();
            }
        };
        // 设置所有数据源
        Map<Object, Object> dataSourceMap = new HashMap<>();
        //获取数据源配置对象
        HikariDataSource datasources = DataSourceConfigUtil.setDataSourceEnvConfig("spring.datasource.", "spring.datasource.hikari.", env);
        //获取到当前服务名
        String serverName = env.getProperty("spring.application.name");
        //从excel中读取数据库服务器ip存入map缓存；
        EasyExcel.read(devicePath, DeviceConfigExcelModel.class, new DeviceConfigExcelModelListener()).sheet().headRowNumber(2).doRead();

        List<DeviceConfigDto> configDtos = new ArrayList<>();
        if (serverName.contains("as")) {
            configDtos = DeviceConfigHashMapCache.get(DeviceConfigEnum.ITD_DATABASE_SERVER.getType());
        }
        if (serverName.contains("gateway")) {
            configDtos = DeviceConfigHashMapCache.get(DeviceConfigEnum.ATS_DATABASE_SERVER.getType());
        }

        List<DeviceIpPortDto> deviceIpPortDtos = new ArrayList<>();
        configDtos.forEach(c -> {
            DeviceIpPortDto dto = new DeviceIpPortDto();
            dto.setPort(c.getPusrpleIpAddressDto().getPortShort());
            dto.setAddress(c.getPusrpleIpAddressDto().getIpAddress());
            DeviceIpPortDto dto2 = new DeviceIpPortDto();
            dto2.setPort(c.getYellowIpAddressDto().getPortShort());
            dto2.setAddress(c.getYellowIpAddressDto().getIpAddress());
            deviceIpPortDtos.add(dto);
            deviceIpPortDtos.add(dto2);
        });

        //替换数据源ip
        if (datasources.getDriverClassName().contains("kingbase")) {
//            if (configDtos.size()==2){
            for (int j = 0; j < deviceIpPortDtos.size(); j++) {
                HikariDataSource datasources2 = new HikariDataSource();
                BeanUtils.copyProperties(datasources, datasources2);
                String newUrl2 = datasources2.getJdbcUrl().replace(datasources2.getJdbcUrl().substring(datasources2.getJdbcUrl().indexOf("//") + 2, datasources2.getJdbcUrl().lastIndexOf("/")),
                        deviceIpPortDtos.get(j).getAddress() + ":" + deviceIpPortDtos.get(j).getPort());
                datasources2.setJdbcUrl(newUrl2);
                dataSourceMap.put(j + "", datasources2);
                DataSourceHashMapCache.add(j, datasources2);
            }
//            }else {
//                //ip转，，，
//                List<String> ipList = deviceIpPortDtos.stream().map(DeviceIpPortDto::getAddress).collect(Collectors.toList());
//                String ipString = ipList.stream().collect(Collectors.joining(","));
//                //port转，，，
//                List<Integer> portList = deviceIpPortDtos.stream().map(DeviceIpPortDto::getPort).collect(Collectors.toList());
//                String portString = portList.stream().map(String::valueOf).collect(Collectors.joining(","));
//                //替换url
//                HikariDataSource datasources2 = new HikariDataSource();
//                BeanUtils.copyProperties(datasources, datasources2);
//                String newUrl2 = datasources2.getJdbcUrl().replace(datasources2.getJdbcUrl().substring(datasources2.getJdbcUrl().indexOf("//") + 2, datasources2.getJdbcUrl().lastIndexOf("/")),
//                        ipString + ":" + portString);
//                datasources2.setJdbcUrl(newUrl2);
//                dataSourceMap.put("0", datasources2);
//                DataSourceHashMapCache.add(0, datasources2);
//            }

        }

        //替换数据源ip
        if (datasources.getDriverClassName().contains("oracle")) {
            for (int j = 0; j < deviceIpPortDtos.size(); j++) {
                //数据源存入map
                HikariDataSource datasources2 = new HikariDataSource();
                BeanUtils.copyProperties(datasources, datasources2);
                String newUrl2 = "";
                newUrl2 = datasources2.getJdbcUrl().replace(datasources2.getJdbcUrl().substring(datasources2.getJdbcUrl().indexOf("@") + 1, datasources2.getJdbcUrl().lastIndexOf(":")),
                        deviceIpPortDtos.get(j).getAddress() + ":" + deviceIpPortDtos.get(j).getPort());

                datasources2.setJdbcUrl(newUrl2);
                dataSourceMap.put(j + "", datasources2);
                DataSourceHashMapCache.add(j, datasources2);
            }
        }

        abstractRoutingDataSource.setTargetDataSources(dataSourceMap);
        // 设置默认数据源 当dsKey找不到对应的数据源或没有设置数据源时, 使用默认数据源
        abstractRoutingDataSource.setDefaultTargetDataSource(dataSourceMap.get("0"));
        // afterPropertiesSet()方法调用时用来将targetDataSources的属性写入resolvedDataSources中的
        abstractRoutingDataSource.afterPropertiesSet();
        return abstractRoutingDataSource;
    }
}