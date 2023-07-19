package com.tct.itd.util;

import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description 获取当前是否存在故障信息
 * @Author zhaoke
 * @Date 2020/5/28 10:27
 **/
@Slf4j
public class ExistAlarmUtils {

    public static Boolean getInfoInLife() {
        AdmAlertInfoSubService service = SpringContextUtil.getBean(AdmAlertInfoSubService.class);
        return service.getInfoByEndLife();
    }

}
