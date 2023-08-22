package com.tct.itd;

import com.tct.itd.basedata.dfsread.enums.FsDataEnum;
import com.tct.itd.basedata.dfsread.service.analysis.ReadConfigDataService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.init.InitConfigService;
import com.tct.itd.init.config.service.SysParamService;
import com.tct.itd.init.datasource.DatasourceActuator;
import com.tct.itd.utils.BaseDataCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author kangyi
 * @description
 * @date 2021/11/3
 **/
@Slf4j
@Component
public class ApplicationRunnerImpl implements ApplicationRunner {

    @Resource
    private List<InitConfigService> initConfigServices;
    @Resource
    private DatasourceActuator datasourceActuator;
    @Resource
    private SysParamService sysParamService;
    @Resource
    private ReadConfigDataService readConfigDataService;


    @Override
    public void run(ApplicationArguments args) {
        //读取系统参数
        sysParamService.readSysParamXmlToDto();
        // 动态数据源故障监测
        datasourceActuator.datasourceCycleActuator();
        //读取电子地图数据
        readConfigDataService.ReadFsByPath();
        log.info("缓存数据：{}", BaseDataCacheUtils.hmget(Cache.SYSTEM_PARAM));
        //初始化项目参数
        initConfigServices.forEach(InitConfigService::init);
    }
}
