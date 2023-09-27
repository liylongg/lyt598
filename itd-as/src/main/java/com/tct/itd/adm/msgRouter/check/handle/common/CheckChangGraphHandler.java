package com.tct.itd.adm.msgRouter.check.handle.common;

import com.tct.itd.adm.msgRouter.check.router.CheckConfigHandler;
import com.tct.itd.adm.util.GraphDataUtil;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.TrainGraphDto;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.enums.PlanRunGraphEnum;
import com.tct.itd.hub.service.PlanRunGraphService;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/***
 * @Description 故障录入校验-校验调图影响范围
 * @Author yuelei
 * @Date 2021/12/14 16:02
 */
@Slf4j
@Service("checkChangGraph")
public class CheckChangGraphHandler implements CheckConfigHandler {

    /**
     * 算法调整范围描述开始字段
     */
    private static final String TIME_RANGE_START_FLAG = "算法调整范围[";
    private static final String CONNECT_FLAG = "-";
    private static final String TIME_RANGE_END_FLAG = "]";

    /**
     * 跨天标志
     */
    private final static String ALG_SECOND_DAY_TAG = "1.";

    @Resource
    private PlanRunGraphService planRunGraphService;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        TrainGraphDto planRunGraph;
        //获取运行图
        try {
            planRunGraph = (TrainGraphDto) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.DTO);
        }catch (Exception e){
            log.error("获取当日计划运行图数据失败：{}", e.getMessage(), e);
            throw new BizException(CodeEnum.NO_GET_NOW_DATE_PLAN_GRAPH);
        }
        log.info("获取到运行图");
        //判断预留字段是否包含算法调整范围
        if (!Objects.isNull(planRunGraph.getReserve()) && planRunGraph.getReserve().contains(TIME_RANGE_START_FLAG)) {
            String reserve = planRunGraph.getReserve();
            if (reserve.contains(TIME_RANGE_START_FLAG) && reserve.contains(TIME_RANGE_END_FLAG)) {
                int startIndex = reserve.indexOf(TIME_RANGE_START_FLAG) + TIME_RANGE_START_FLAG.length();
                int endIndex = reserve.indexOf(TIME_RANGE_END_FLAG);
                String timeStr = reserve.substring(startIndex, endIndex);
                String[] timeStrArray = timeStr.split(CONNECT_FLAG);
                //影响范围开始时间- 时分秒
                String start = timeStrArray[0];
                //影响范围结束时间- 时分秒
                String end = timeStrArray[1];
                List<String> nowAndNextDay = GraphDataUtil.getNowAndNextDay(alarmInfo.getStartAlarmTime());
                String nowDate = nowAndNextDay.get(0);
                String nextDate = nowAndNextDay.get(1);
                if(start.startsWith(ALG_SECOND_DAY_TAG)){
                    start = nextDate + " " + (start.replaceFirst(ALG_SECOND_DAY_TAG, ""));
                }else{
                    start = nowDate + " " + start;
                }
                if(end.startsWith(ALG_SECOND_DAY_TAG)){
                    end = nextDate + " " + (end.replaceFirst(ALG_SECOND_DAY_TAG, ""));
                }else{
                    end = nowDate + " " + end;
                }
                Date startAlarmTime = DateUtil.getStringToDate(alarmInfo.getStartAlarmTime(), "yyyy-MM-dd HH:mm:ss.SSS");
                Date startDate = DateUtil.getStringToDate(start, "yyyy-MM-dd HH:mm:ss");
                Date endDate = DateUtil.getStringToDate(end, "yyyy-MM-dd HH:mm:ss");
                //判断故障开始时是否在上一个故障的影响范围内
                if (startAlarmTime.getTime() <= endDate.getTime() && startAlarmTime.getTime() >= startDate.getTime()) {
                    log.error("新故障开始时上个故障调整还未完成，算法无法处理。上个故障:{},当前故障开始时间:{}", planRunGraph.getReserve()
                            ,alarmInfo.getStartAlarmTime());
                    throw new BizException(CodeEnum.CHECK_CHANGE_GRAPH);
                }
            }
        }
    }
}
