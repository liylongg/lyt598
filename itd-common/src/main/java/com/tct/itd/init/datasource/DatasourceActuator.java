package com.tct.itd.init.datasource;


import com.tct.itd.common.cache.Cache;
import com.tct.itd.constant.StringConstant;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.init.config.cache.DataSourceHashMapCache;
import com.tct.itd.dto.TrafficParameterXmlDTO;
import com.tct.itd.utils.BaseDataCacheUtils;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description : 数据源连接监控定时器
 * @Author : zhoukun
 * @Date : Created in 2022/5/24
 */
@Component
@Slf4j
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
public class DatasourceActuator {
    private final static String TIME_OUT = "&connectTimeout=500";
    @Resource
    private DynamicDataSourceProperties dynamicDataSourceProperties;
    public static Integer ATS_HEART_LOSS = 300;


    @Async
    public void datasourceCycleActuator() {
        // 默认可用则优先使用第一个数据源
        while (true) {
            if (0l==(long)BaseDataCacheUtils.get(StringConstant.DATABASE_MONITOR_SWITCH)){
                return;
            }
            int j = 0;
            for (int i = 0; i < DataSourceHashMapCache.DATASOURCE_MAP.size(); i++) {
                //当监测到数据库可以连接时
                if (testDatasource(DataSourceHashMapCache.get(i))) {
                    if (BaseDataCacheUtils.exist(Cache.DATASOURCE_ACTUATOR)) {
                        AppPushUtil.sendWebNoticeMessageToAny(new WebNoticeDto(ATS_HEART_LOSS, "0",
                                "数据库连接恢复"));
                        BaseDataCacheUtils.delKey(Cache.DATASOURCE_ACTUATOR);
                    }
                    //当监测到数据库恢复连接，并且不是当前数据源时，切换数据源
                    if (!(i+"").equals(BaseDataCacheUtils.get(Cache.HUB_DATASOURCE_KEY).toString())){
                        BaseDataCacheUtils.set(Cache.HUB_DATASOURCE_KEY, i + "");
                    }
                    break;

                } else {
                    //当监测到数据库断开连接，并且为当前数据源时，推送前台提示数据库连接断开
                    if ((i+"").equals(BaseDataCacheUtils.get(Cache.HUB_DATASOURCE_KEY).toString())){
                        AppPushUtil.sendWebNoticeMessageToAny(new WebNoticeDto(ATS_HEART_LOSS, "0",
                                "数据库连接断开"));
                        BaseDataCacheUtils.set(Cache.DATASOURCE_ACTUATOR, 1);
                    }
                    ++j;
                }
            }

//            if (j == DataSourceHashMapCache.DATASOURCE_MAP.size()) {
//                AppPushUtil.sendWebNoticeMessageToAny(new WebNoticeDto(ATS_HEART_LOSS, "0",
//                        "数据库连接断开"));
//                BaseDataCacheUtils.set(Cache.DATASOURCE_ACTUATOR, 1);
//            }
            try {
                Thread.sleep((long)BaseDataCacheUtils.get(StringConstant.SET_DATABASE_MONITOR_INTERVAL));
            } catch (InterruptedException e) {
                log.error("数据源监控异常!", e);
            }
        }

    }


    /**
     * 检测数据源连接可用
     *
     * @param dataSource 数据源
     * @return 是否可用
     */
    private Boolean testHDatasource(HikariDataSource dataSource) {
        // 数据源还没初始化是不检测
        if (dataSource == null) {
            return Boolean.TRUE;
        }
        try {
            dataSource.getConnection();
        } catch (SQLException e) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * 检测数据源连接可用
     *
     * @param dataSource 数据源
     * @return 是否可用
     */
    private Boolean testDatasource(HikariDataSource dataSource) {
        Connection con = null;
        try {
            con = DriverManager.getConnection(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword());
        } catch (SQLException e) {
            log.info("数据源连接异常,切换数据源", e);
            return Boolean.FALSE;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return Boolean.TRUE;
    }
}