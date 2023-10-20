package com.tct.itd.adm.util;


import com.tct.itd.dto.DisposeDto;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @Author: zhangyinglong
 * @Date:2021/5/18 15:17
 */
public class AidDecisionStepUtils {
    /**
     * @Description 大客流之断面客流
     * @Author zhangyinglong
     * @Date 2021/7/6 16:35
     * @param alarmSite 车站
     * @param upDown 向下行
     * @return java.util.List<java.lang.String>
     */
    public static List<DisposeDto> generateLargePassengerFlowOfSectionFlow(String alarmSite,String upDown) {
        List<String> stepList = new ArrayList<>();
        //发送电子调度命令 (车辆段)
        String step1 = "动态调整运行图实际调整图预览并向ATS系统推送加载执行";
        //发送电子调度命令（所有车站）
        String step2 = "自动下发电子调度命令通知所有车站预备车加开路线;";
        stepList.add(step1);
        stepList.add(step2);

        List<DisposeDto> list = new ArrayList<>();
        for (String s : stepList) {
            list.add(new DisposeDto(s, 1));
        }
        return list;
    }

}
