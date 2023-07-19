package com.tct.itd.util;

import com.tct.itd.utils.SpringContextUtil;
import org.springframework.util.StringUtils;

/**
 * @author kangyi
 * @description 格式化输出
 * @date 2022年 10月12日 11:52:40
 */
public class FormatUtil {

    //拼接车次号,前面补0
    public static String formatOrderNumber(String orderNumberStr) {
        if (StringUtils.isEmpty(orderNumberStr) || "0".equals(orderNumberStr)) {
            return "0";
        }
        com.github.benmanes.caffeine.cache.Cache<String, String> initTrainGraphSysParamCache = (com.github.benmanes.caffeine.cache.Cache<String, String> )
                SpringContextUtil.getBean("initTrainGraphSysParamCache");
        //系统参数配置的车次号位数
        String digitStr = initTrainGraphSysParamCache.getIfPresent(com.tct.itd.common.cache.Cache.ORDER_NUMBER_DIGIT);
        if (StringUtils.isEmpty(digitStr)) {
            return orderNumberStr;
        }
        int digit = Integer.parseInt(digitStr);
        //需要在车次号前补0的位数
        int sub = digit - orderNumberStr.length();
        if (sub <= 0) {
            return orderNumberStr;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sub; i++) {
            sb.append("0");
        }
        sb.append(orderNumberStr);
        return sb.toString();
    }

    //拼接表号,前面补0
    public static String formatServerNumber(String serverNumberStr) {
        if (StringUtils.isEmpty(serverNumberStr) || "0".equals(serverNumberStr)) {
            return "0";
        }
        com.github.benmanes.caffeine.cache.Cache<String, String> initTrainGraphSysParamCache = (com.github.benmanes.caffeine.cache.Cache<String, String> )
                SpringContextUtil.getBean("initTrainGraphSysParamCache");
        //系统参数配置的表号位数
        String digitStr = initTrainGraphSysParamCache.getIfPresent(com.tct.itd.common.cache.Cache.SERVER_NUMBER_DIGIT);
        if (StringUtils.isEmpty(digitStr)) {
            return serverNumberStr;
        }
        int digit = Integer.parseInt(digitStr);
        //需要在车次号前补0的位数
        int sub = digit - serverNumberStr.length();
        if (sub <= 0) {
            return serverNumberStr;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sub; i++) {
            sb.append("0");
        }
        sb.append(serverNumberStr);
        return sb.toString();
    }

}
