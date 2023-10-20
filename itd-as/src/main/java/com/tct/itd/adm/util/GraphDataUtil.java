package com.tct.itd.adm.util;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.TrainGraphConvert;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.AdjustStatisticalDto;
import com.tct.itd.common.dto.PrePlanRunGraph;
import com.tct.itd.common.dto.TrainNumberAdjustDto;
import com.tct.itd.common.enums.TrainStateEnum;
import com.tct.itd.dto.*;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.GZIPUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.XmlUtil;
import com.tct.itd.xml.TrainGraph;
import com.tct.itd.xml.TrainGraphRoot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description 运行图数据处理
 * @Author yuelei
 * @Date 2022/3/4 11:17
 */
@Component
@Slf4j
public class GraphDataUtil {

    private static final String IS_TRUE = "True";
    private static final String BLUE = "-16728064,";

    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final int THREE = 3;

    private static final int NUM_120 = 120;

    private static final int NUM_300 = 300;

    //调整标记-勾连
    private static final String ADJUST_MARK = "adjustMark";

    //调整标记-不勾连
    private static final String NO_ADJUST_MARK = "noAdjustMark";

    private static TrainGraphConvert trainGraphConvert;

    @Resource
    public void setTrainGraphConvert(TrainGraphConvert trainGraphConvert) {
        GraphDataUtil.trainGraphConvert = trainGraphConvert;
    }

    /**
     * @param planRunGraph
     * @return java.lang.String
     * @Description 截取调图影响范围内的运行图数据
     * @Author yuelei
     * @Date 2022/3/4 11:20
     */
    public static TrainGraphDto getChangeAfterGraphData(TrainGraphDto planRunGraph, List<TrainNumberAdjustDto> dataList) {
        //获取受影响的车次集合
        Set<String> set = new HashSet<>();
        dataList.forEach(data -> {
            if (data.getAdjustType().equals(TrainStateEnum.TRAIN_RETURN.getValue())) {
                set.add(getOrderNum(data.getServerNumber(), data.getNextTrainNumber()));
            }
            set.add(getOrderNum(data.getServerNumber(), data.getTrainOrder()));
        });
        List<ServerNumberDto> serverNumbers = planRunGraph.getServerNumbers();
        //标记需要保留的车次信息
        for (ServerNumberDto serverNumber : serverNumbers) {
            List<TrainNumberDto> trainNumbers = serverNumber.getTrainNumbers();
            if (trainNumbers.isEmpty() || trainNumbers.size() == 1) {
                continue;
            }
            for (int i = 0; i < trainNumbers.size(); i++) {
                TrainNumberDto trainNumber = trainNumbers.get(i);
                if (set.contains(getOrderNum(serverNumber.getId(), trainNumber.getOrderNumber()))) {
                    trainNumber.getTrains().forEach(train -> train.setReserve(ADJUST_MARK));
                    if (i == 0) {
                        (trainNumbers.get(i + 1)).getTrains().get(0).setReserve(NO_ADJUST_MARK);
                    } else if (i == trainNumbers.size() - 1) {
                        List<TrainDto> trains = (trainNumbers.get(i - 1)).getTrains();
                        trains.get(trains.size() - 1).setReserve(NO_ADJUST_MARK);
                    } else {
                        (trainNumbers.get(i + 1)).getTrains().get(0).setReserve(NO_ADJUST_MARK);
                        List<TrainDto> trains = (trainNumbers.get(i - 1)).getTrains();
                        trains.get(trains.size() - 1).setReserve(NO_ADJUST_MARK);
                    }
                }
            }
        }
        //移除未标记的车次信息
        serverNumbers.forEach(serverNumber -> {
            List<TrainNumberDto> trainNumbers = serverNumber.getTrainNumbers();
            trainNumbers.forEach(trainNumber -> {
                trainNumber.getTrains().removeIf(train -> !ADJUST_MARK.equals(train.getReserve()) && !NO_ADJUST_MARK.equals(train.getReserve()));
            });
            trainNumbers.removeIf(trainNumber -> trainNumber.getTrains().isEmpty());
        });
        //移除车次为空的ServerNumber
        serverNumbers.removeIf(next -> next.getTrainNumbers().isEmpty());
//        //赋值颜色
//        String color = SysParamKit.getByCode(IidsSysParamPool.CHANGE_AFTER_COLOR);
//        serverNumbers.forEach(serverNumber -> {
//            serverNumber.getTrainNumbers().forEach(trainNumber ->{
//                //如果包含该车次，则修改颜色
//                trainNumber.setHasCustomColor(IS_TRUE);
//                trainNumber.setCustomColorInfo(color);
//            });
//        });
        return planRunGraph;
    }

    /**
     * @param planRunGraph
     * @param dataList
     * @return com.tct.model.vo.trainGraph.PlanRunGraph
     * @Description 修改变更前运行图调整车次的颜色
     * @Author yuelei
     * @Date 2022/3/21 10:04
     */
    public static TrainGraphDto changeBeforeGraphColor(TrainGraphDto planRunGraph, List<TrainNumberAdjustDto> dataList) {
        //获取受影响的车次集合
//        Set<String> set = new HashSet<>();
//        dataList.forEach(data -> {
//            set.add(getOrderNum(data.getServerNumber(), data.getTrainOrder()));
//        });
//        List<ServerNumber> serverNumbers = planRunGraph.getServerNumbers();
//        serverNumbers.forEach(serverNumber -> {
//            List<TrainNumber> trainNumbers = serverNumber.getTrainNumbers();
//            trainNumbers.forEach(trainNumber ->{
//                //如果包含该车次，则修改颜色
//                if(set.contains(getOrderNum(serverNumber.getId(), trainNumber.getOrderNumber()))){
//                    trainNumber.setHasCustomColor(IS_TRUE);
//                    trainNumber.setCustomColorInfo(BLUE);
//                }
//            });
//        });
        return planRunGraph;
    }

    /**
     * @param beforeGraph
     * @param afterGraph
     * @param dataList
     * @return com.tct.model.vo.tias.push.PrePlanRunGraph
     * @Description 获取前后运行图对比数据
     * @Author yuelei
     * @Date 2022/3/21 10:36
     */
    public static PrePlanRunGraph getPrePlanRunGraph(String beforeGraph, String afterGraph, List<TrainNumberAdjustDto> dataList) {
        PrePlanRunGraph prePlanRunGraph = new PrePlanRunGraph();
        //处理修改之前的运行图数据
        TrainGraphDto beforeGraphData =
                JsonUtils.jsonToObject(GZIPUtil.unGzip(beforeGraph), TrainGraphDto.class);
        GraphDataUtil.changeBeforeGraphColor(beforeGraphData, dataList);
        prePlanRunGraph.setBeforeGraphDataXml(GZIPUtil.gzip(GraphDataUtil.dealToXml(beforeGraphData)));
        //解析修改后的运行图数据
        TrainGraphDto planRunGraph =
                JsonUtils.jsonToObject(GZIPUtil.unGzip(afterGraph), TrainGraphDto.class);
        GraphDataUtil.getChangeAfterGraphData(planRunGraph, dataList);
        prePlanRunGraph.setGraphDataXml(GZIPUtil.gzip(GraphDataUtil.dealToXml(planRunGraph)));
        return prePlanRunGraph;
    }


    /**
     * @param id
     * @param orderNumber
     * @return java.lang.String
     * @Description 返回新的车次号 = 表号 + 车次号
     * @Author yuelei
     * @Date 2022/3/21 10:38
     */
    public static String getOrderNum(String id, String orderNumber) {
        return Integer.parseInt(id) + "" + Integer.parseInt(orderNumber);
    }

    /***
     * @Description 获取当天和下一天的日期
     * @Author yuelei
     * @Date 2022/1/12 16:54
     * @param startAlarmTime
     * @return java.util.List<java.lang.String>
     */
    public static List<String> getNowAndNextDay(String startAlarmTime) {
        //调图晚点时间影响范围
        //跨天运营起始时间
        String secondDayTime = "00:00:00";
        //跨天运营结束时间
        String endDayTime = SysParamKit.getByCode(IidsSysParamPool.UPDATE_GRAPH_TIME);
        List<String> list = new ArrayList<>();
        String nextDate = "";
        String nowDate = "";
        String dateToString = DateUtil.getStringToDate(DateUtil.getStringToDate(startAlarmTime, "yyyy-MM-dd HH:mm:ss.SSS"),
                "yyyy-MM-dd");
        //判断时间是否跨天来确定算法参数中的时间是否加"1."
        if (DateUtil.isBelongPeriodTime(startAlarmTime, secondDayTime, endDayTime)) {
            nextDate = dateToString;
            nowDate = DateUtil.getPreDateByDate(dateToString);
        } else {
            nowDate = dateToString;
            nextDate = DateUtil.getNextDateByDate(nowDate);
        }
        list.add(nowDate);
        list.add(nextDate);
        return list;
    }


    /**
     * @return void
     * @Description 运行图对象转换为xml
     * @Author yuelei
     * @Date 2022/2/23 19:35
     */
    public static String dealToXml(TrainGraphDto planRunGraph) {
        TrainGraph trainGraph = trainGraphConvert.dtoToXml(planRunGraph);
        TrainGraphRoot root = new TrainGraphRoot();
        root.setTrainGraph(trainGraph);
        return XmlUtil.convertToXml(root);
    }

    /**
     * @description: 生成运行图调整方案结果
     * @author: yuzhenxin
     * @date: 2022/8/1 15:50
     **/
    public static List<AdjustStatisticalDto> generateAdjustStatisticalResult(List<TrainNumberAdjustDto> trainNumberAdjustDtoList) {

        List<AdjustStatisticalDto> statisticalDtoList = Lists.newArrayList();
        for (TrainNumberAdjustDto adjustDto : trainNumberAdjustDtoList) {
            AdjustStatisticalDto statisticalDto = new AdjustStatisticalDto();
            statisticalDto.setTrainId(adjustDto.getTrainId());
            statisticalDto.setTrainOrder(adjustDto.getServerNumber() + adjustDto.getTrainOrder());
            statisticalDto.setStartLateTime(adjustDto.getTrainNumberAdjustSubDto().getStartLateTime());
            statisticalDto.setStartLateStation(Objects.equals(statisticalDto.getStartLateTime(), 0) ? null : adjustDto.getTrainNumberAdjustSubDto().getLateStationName());
            statisticalDto.setArriveLateTime(adjustDto.getTrainNumberAdjustSubDto().getArriveLateTime());
            statisticalDto.setArriveLateStation(Objects.equals(statisticalDto.getArriveLateTime(), 0) ? null : adjustDto.getTrainNumberAdjustSubDto().getLateStationName());
            statisticalDto.setTurnBackStationName(adjustDto.getTrainNumberAdjustSubDto().getTurnBackStationName());
            statisticalDto.setClearStationName(adjustDto.getTrainNumberAdjustSubDto().getClearStationName());
            if (Objects.equals(adjustDto.getTrainNumberAdjustSubDto().getAdjustSubType(), 2048)) {
                statisticalDto.setAddTrainStationName(adjustDto.getTrainNumberAdjustSubDto().getAddTrainStationNameList());
            } else if (Objects.equals(adjustDto.getTrainNumberAdjustSubDto().getAdjustSubType(), 64)) {

                statisticalDto.setAddEmptyTrainStationName(adjustDto.getTrainNumberAdjustSubDto().getAddTrainStationNameList());
            }
            statisticalDto.setThroughStationName("-");
            if ((!Objects.equals(adjustDto.getTrainNumberAdjustSubDto().getStoptStationNameList(), null)) && Objects.equals(adjustDto.getTrainNumberAdjustSubDto().getStoptStationNameList().size(), 2)) {
                statisticalDto.setStopStationName(adjustDto.getTrainNumberAdjustSubDto().getStoptStationNameList().get(0) + "-" + adjustDto.getTrainNumberAdjustSubDto().getStoptStationNameList().get(1));
            }
            statisticalDto.setReplaceOldID(adjustDto.getTrainNumberAdjustSubDto().getReplaceOldID());
            statisticalDto.setImmediateOffLineStationName(Objects.equals(adjustDto.getTrainNumberAdjustSubDto().getSubOffLineType(), 1) ? adjustDto.getTrainNumberAdjustSubDto().getOffLineStationName() : null);
            statisticalDto.setTerminusOffLineStationName(Objects.equals(adjustDto.getTrainNumberAdjustSubDto().getSubOffLineType(), 2) ? adjustDto.getTrainNumberAdjustSubDto().getOffLineStationName() : null);
            statisticalDtoList.add(statisticalDto);
        }
        Map<String, List<AdjustStatisticalDto>> map = statisticalDtoList.stream().collect(Collectors.groupingBy(AdjustStatisticalDto::getTrainOrder));
        List<AdjustStatisticalDto> result = Lists.newArrayList();
        for (List<AdjustStatisticalDto> list : map.values()) {
            AdjustStatisticalDto res = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                if (Objects.equals(res.getStartLateStation(), null)) {
                    res.setStartLateStation(list.get(i).getStartLateStation());
                }
                if (Objects.equals(res.getStartLateTime(), null)) {
                    res.setStartLateTime(list.get(i).getStartLateTime());
                }
                if (Objects.equals(res.getArriveLateStation(), null)) {
                    res.setArriveLateStation(list.get(i).getArriveLateStation());
                }
                if (Objects.equals(res.getArriveLateTime(), null)) {
                    res.setArriveLateTime(list.get(i).getArriveLateTime());
                }
                if (Objects.equals(res.getTurnBackStationName(), null)) {
                    res.setTurnBackStationName(list.get(i).getTurnBackStationName());
                }
                if (Objects.equals(res.getClearStationName(), null)) {
                    res.setClearStationName(list.get(i).getClearStationName());
                }
                if (Objects.equals(res.getAddTrainStationName(), null)) {
                    res.setAddTrainStationName(list.get(i).getAddTrainStationName());
                }
                if (Objects.equals(res.getAddEmptyTrainStationName(), null)) {
                    res.setAddEmptyTrainStationName(list.get(i).getAddEmptyTrainStationName());
                }
                if (Objects.equals(res.getStopStationName(), null)) {
                    res.setStopStationName(list.get(i).getStopStationName());
                }

                if (Objects.equals(res.getReplaceOldID(), null)) {
                    res.setReplaceOldID(list.get(i).getReplaceOldID());
                }

                if (Objects.equals(res.getImmediateOffLineStationName(), null)) {
                    res.setImmediateOffLineStationName(list.get(i).getImmediateOffLineStationName());
                }

                if (Objects.equals(res.getTerminusOffLineStationName(), null)) {
                    res.setTerminusOffLineStationName(list.get(i).getTerminusOffLineStationName());
                }

            }
            result.add(res);
        }
        List<AdjustStatisticalDto> qualifiedResult = result.stream().filter(r -> !(Objects.equals(r.getStartLateTime(), 0)
                && Objects.equals(r.getStartLateStation(), null)
                && Objects.equals(r.getArriveLateTime(), 0)
                && Objects.equals(r.getArriveLateStation(), null)
                && Objects.equals(r.getTurnBackStationName(), null)
                && Objects.equals(r.getClearStationName(), null)
                && Objects.equals(r.getAddTrainStationName(), null)
                && Objects.equals(r.getAddEmptyTrainStationName(), null)
                && Objects.equals(r.getStopStationName(), null)
                && Objects.equals(r.getReplaceOldID(), null)
                && Objects.equals(r.getImmediateOffLineStationName(), null)
                && Objects.equals(r.getTerminusOffLineStationName(), null))).collect(Collectors.toList());
        return qualifiedResult;
    }
}
