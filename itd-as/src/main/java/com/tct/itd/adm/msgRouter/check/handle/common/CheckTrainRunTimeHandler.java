package com.tct.itd.adm.msgRouter.check.handle.common;

import com.tct.itd.adm.msgRouter.check.router.CheckConfigHandler;
import com.tct.itd.adm.util.GraphDataUtil;
import com.tct.itd.dto.*;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.enums.PlanRunGraphEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.hub.service.PlanRunGraphService;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author yuelei
 * @Desc 非运营时间内故障上报防护
 * @Date 16:01 2022/7/4
 */
@Slf4j
@Service("checkTrainRunTime")
public class CheckTrainRunTimeHandler implements CheckConfigHandler {

    @Resource
    private PlanRunGraphService planRunGraphService;
    /**
     * 跨天标志
     */
    private final static String ALG_SECOND_DAY_TAG = "1.";

    @Override
    public void handle(AlarmInfo alarmInfo) {
        //获取运行图
        TrainGraphDto planRunGraph;
        //获取运行图
        try {
            planRunGraph = (TrainGraphDto) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.DTO);
        }catch (Exception e){
            log.error("获取当日计划运行图数据失败：{}", e.getMessage(), e);
            throw new BizException(CodeEnum.NO_GET_NOW_DATE_PLAN_GRAPH);
        }
        List<ServerNumberDto> serverNumbers = planRunGraph.getServerNumbers();
        if(serverNumbers != null && !serverNumbers.isEmpty()){
            //结束时间
            long maxEndTime = 0;
            //开始时间
            long minStartTime = 0;
            List<String> nowAndNextDay = GraphDataUtil.getNowAndNextDay(alarmInfo.getStartAlarmTime());
            for(ServerNumberDto dto : serverNumbers){
                TrainDto trainDto = dto.getTrainNumbers().get(0).getTrains().get(0);
                long startTime;
                if(trainDto.getTime().startsWith("1.")){
                    String nextDate = nowAndNextDay.get(1);
                    String time = trainDto.getTime();
                    time = nextDate + " " + (time.replaceFirst(ALG_SECOND_DAY_TAG, ""));
                    startTime = DateUtil.getStringToDate(time, "yyyy-MM-dd HH:mm:ss").getTime();
                }else{
                    startTime = DateUtil.getStringToDate(nowAndNextDay.get(0) + " " + trainDto.getTime(), "yyyy-MM-dd HH:mm:ss").getTime();
                }
                //获取最小值
                if(minStartTime == 0){
                    minStartTime = startTime;
                }else if(startTime < minStartTime){
                    minStartTime = startTime;
                }
                List<TrainNumberDto> trainNumbers = dto.getTrainNumbers();
                List<TrainDto> trains = trainNumbers.get(trainNumbers.size() - 1).getTrains();
                TrainDto trainDto1 = trains.get(trains.size() - 1);
                //如果跨天
                long endTime;
                if(trainDto1.getTime().startsWith("1.")){
                    String nextDate = nowAndNextDay.get(1);
                    String time = trainDto1.getTime();
                    time = nextDate + " " + (time.replaceFirst(ALG_SECOND_DAY_TAG, ""));
                    endTime = DateUtil.getStringToDate(time, "yyyy-MM-dd HH:mm:ss").getTime();
                }else{
                    endTime = DateUtil.getStringToDate(nowAndNextDay.get(0) + " " + trainDto1.getTime(), "yyyy-MM-dd HH:mm:ss").getTime();
                }
                //获取最小值
                if(maxEndTime == 0){
                    maxEndTime = endTime;
                }else if(endTime > maxEndTime){
                    maxEndTime = endTime;
                }
            }
            long trainTime = DateUtil.getStringToDate(alarmInfo.getStartAlarmTime(), "yyyy-MM-dd HH:mm:ss.SSS").getTime();
            log.info("运行图开始时间：{}， 运行图结束时间：{}，故障开始时间：{}", DateUtil.getTimeStamp(minStartTime), DateUtil.getTimeStamp(maxEndTime), alarmInfo.getStartAlarmTime());
            if (trainTime > maxEndTime || trainTime < minStartTime) {
                throw new BizException(CodeEnum.START_TIME_NOT_ALLOW);
            }
        }
    }
}
