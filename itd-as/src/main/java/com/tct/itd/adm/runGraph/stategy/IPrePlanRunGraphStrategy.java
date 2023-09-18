package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.common.dto.PrePlanRunGraph;
import com.tct.itd.dto.AlarmInfo;

import java.util.List;

/**
 * @Description
 * @Author zhangyinglong
 * @Date 2021/6/2 14:11
 */
public interface IPrePlanRunGraphStrategy {
    /**
     * @Description 故障状态,
     * @Author zhangyinglong
     * @Date 2021/6/4 14:04
     * @param alarmInfo
     * @return int
     */
    int getAlarmState(AlarmInfo alarmInfo);
    /**
     * @Description 故障结束时间
     * @Author zhangyinglong
     * @Date 2021/6/4 14:03
     * @param alarmInfo
     * @return java.lang.String
     */
    String getEndAlarmTime(AlarmInfo alarmInfo);

    /**
     * @Description 单个运行图方案
     * @Author zhangyinglong
     * @Date 2021/6/4 14:03
     * @param alarmInfo
     * @return com.tct.model.vo.tias.push.PrePlanRunGraph
     */
    PrePlanRunGraph previewRunGraph(AlarmInfo alarmInfo);

    /**
     * @Description 列出可选的运行图方案
     * @Author zhangyinglong
     * @Date 2021/6/7 14:55
     * @param alarmInfo
     * @return java.util.List<com.tct.model.vo.algorithm.AlgStrategyResult>
     */
    List<AlgStrategyResult> listAdjustStrategy(AlarmInfo alarmInfo);
    /**
     * @Description 故障类型
     * @Author zhangyinglong
     * @Date 2021/6/4 14:03
     * @param
     * @return java.lang.String
     */
    String strategy();
    /**
     * @Description 模拟运行图方案
     * @Author kangyi
     * @Date 2022/3/8 14:55
     * @return java.util.List<com.tct.model.vo.algorithm.AlgStrategyResult>
     */
    List<AlgStrategyResult> mockListAdjustStrategy();
    /**
     * @Description 模拟运行图方案
     * @Author kangyi
     * @Date 2022/3/8 14:55
     * @return com.tct.model.vo.tias.push.PrePlanRunGraph
     */
    PrePlanRunGraph mockPreviewRunGraph();
}
