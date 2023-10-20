package com.tct.itd.adm.util;

import com.google.common.collect.Lists;
import com.tct.itd.common.dto.ConStationDto;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.DisposeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @classname: StrategyMatchUtil
 * @description: 运行图预览策略码值匹配工具
 * @author: liyunlong
 * @date: 2022/2/17 19:30
 */
@Slf4j
public class StrategyMatchUtil {

    private StrategyMatchUtil() {
    }

    /**
     * 重新匹配运行图预览策略码值
     * @author liyunlong
     * @date 2021/12/22 15:13
     * @param alarmType 故障类型
     * @param strategy 故障子类型
     * @param executeStep 推荐指令执行步骤
     * @return java.lang.String
     */
    public static String matchStrategy(int alarmType, String strategy, int executeStep) {
        // 道岔故障运行图预览策略根据辅助推荐指令步骤匹配
        if (NumConstant.TWELVE.equals(alarmType)) {
            // 推荐指令执行步骤
            switch (strategy) {
                // 终端战后折返道岔故障具备本站折返
                case "1201":
                    switch (executeStep) {
                        case 1:
                        case 2:
                            strategy = "120102";
                            break;
                        case 3:
                        case -2:
                        case -1:
                            strategy = "120103";
                            break;
                        default:
                            log.error("终端站折返道岔故障-具备本站折返没有对应的推荐指令步骤");
                            break;
                    }
                    break;
                case "1202":
                    switch (executeStep) {
                        case 1:
                        case 2:
                            strategy = "120202";
                            break;
                        case 3:
                            strategy = "120203";
                            break;
                        case 4:
                        case -2:
                        case -3:
                        case -1:
                            strategy = "120204";
                            break;
                        default:
                            log.error("终端站折返道岔故障-不具备本站折返没有对应的推荐指令步骤");
                            break;
                    }
                    break;
                case "1203":
                    switch (executeStep) {
                        case 1:
                        case 2:
                            strategy = "120302";
                            break;
                        case 3:
                            strategy = "120303";
                            break;
                        case 4:
                        case -2:
                        case -3:
                        case -1:
                            strategy = "120304";
                            break;
                        default:
                            log.error("道岔故障-影响上下行没有对应的推荐指令步骤");
                            break;
                    }
                    break;
                case "1204":
                    switch (executeStep) {
                        case 1:
                        case 2:
                            strategy = "120402";
                            break;
                        case 3:
                            strategy = "120403";
                            break;
                        case 4:
                        case -2:
                        case -3:
                        case -1:
                            strategy = "120404";
                            break;
                        default:
                            log.error("道岔故障-影响单行没有对应的推荐指令步骤");
                            break;
                    }
                    break;
                case "1206":
                    switch (executeStep) {
                        case 1:
                        case 2:
                            strategy = "120602";
                            break;
                        case 3:
                            strategy = "120603";
                            break;
                        case 4:
                        case -2:
                        case -3:
                        case -1:
                            strategy = "120604";
                            break;
                        default:
                            log.error("道岔故障-影响单行没有对应的推荐指令步骤");
                            break;
                    }
                    break;
                case "1207":
                    switch (executeStep) {
                        case 1:
                        case 2:
                            strategy = "120702";
                            break;
                        case 3:
                            strategy = "120703";
                            break;
                        case 4:
                        case -2:
                        case -3:
                        case -1:
                            strategy = "120704";
                            break;
                        default:
                            log.error("道岔故障-影响单行没有对应的推荐指令步骤");
                            break;
                    }
                    break;
                case "1210":
                    switch (executeStep) {
                        case 1:
                        case 2:
                            strategy = "121002";
                            break;
                        case 3:
                            strategy = "121003";
                            break;
                        case 4:
                        case -2:
                        case -3:
                        case -1:
                            strategy = "121004";
                            break;
                        default:
                            log.error("道岔故障-影响单行没有对应的推荐指令步骤");
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
        return strategy;
    }

    /**
     * 组装推荐指令
     * @author liyunlong
     * @date 2022/5/2 15:30
     * @param list 推荐指令信息
     * @param conStationDto 集中站信息
     * @return java.util.List<com.tct.model.dto.DisposeDto>
     */
    public static List<DisposeDto> formatStepList(List<DisposeDto> list, ConStationDto conStationDto) {
        List<DisposeDto> dtoList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(list)) {
            log.error("推荐指令执行单元内容为空");
            return dtoList;
        }
        list.forEach(l -> {
            String step = l.getStep();
            if (step.contains("%s站")) {
                l.setStep(step.replaceAll("%s站", conStationDto.getStationName()));
                dtoList.add(l);
                return;
            }
            dtoList.add(l);
        });
        return dtoList;
    }

    /**
     * 数组中是否包含元素
     * @author liyunlong
     * @date 2023/3/4 10:04
     * @param array 数组
     * @param str 元素
     * @return java.lang.Boolean
     */
    public static Boolean isContains(String[] array, String str) {
        Boolean result = Boolean.FALSE;
        if (array == null || array.length == 0) {
            log.warn("传入的数组参数为空!");
            return result;
        }
        if (StringUtils.isEmpty(str)) {
            log.warn("传入的判断参数为空");
            return result;
        }
        for (String s : array) {
            if (s.equals(str)) {
                result = Boolean.TRUE;
                break;
            }
        }
        return result;
    }
}
