package com.tct.itd.adm.msgRouter.service;

import com.tct.itd.common.dto.AdmCheckConfigDto;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description 故障录入校验配置
 * @Author yuelei
 * @Date 2021/12/14 15:05
 */
@Service
public class CheckConfigService{

    @Resource(name = "checkConfigCache")
    private com.github.benmanes.caffeine.cache.Cache<Integer, AdmCheckConfigDto> checkConfigDto;

    /**
     * @Description 获取故障类型对应的校验配置
     * @Author yuelei
     * @Date 2021/12/14 15:14
     * @param
     * @param alarmType
     */
    public List<AdmCheckConfigDto> listByAlarmType(String alarmType) {
        Collection<AdmCheckConfigDto> values = checkConfigDto.asMap().values();
        String finalAlarmType = "[" + alarmType + "]";
        return values.stream().filter(dto -> (dto.getType() == 0 ||
                        (dto.getType() == 1 && dto.getAlarmType().contains(finalAlarmType))))
                        .collect(Collectors.toList());
    }
}
