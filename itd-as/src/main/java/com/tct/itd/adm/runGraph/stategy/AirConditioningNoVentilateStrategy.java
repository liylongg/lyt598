package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.util.GraphDataUtil;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.TrainTime;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.hub.service.PlanRunGraphService;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @Description 列车空调无通风-运行图预览
 * @Author zhangyinglong
 * @Date 2021/6/2 14:27
 */
@Slf4j
@Service
public class AirConditioningNoVentilateStrategy extends AbstractPrePlanRunGraphStrategy {

    @Resource
    private PlanRunGraphService planRunGraphService;

    /**
     * 跨天标志
     */
    private final static String ALG_SECOND_DAY_TAG = "1.";

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        if (Objects.equals(alarmInfo.getAlarmState(), AlarmStateEnum.REVERSE_RAIL.getCode())) {
            return AlarmStateEnum.REVERSE_RAIL.getCode();
        }
        if (alarmInfo.getSectionFlag() == 0) {
            //站台上报
            return AlarmStateEnum.NEXT_STATION_DROP_LINE.getCode();
        }
        if (alarmInfo.getSectionFlag() == 1) {
            //区间上报,前端报即将到达的站, 上报算法下一站掉线算法
            return AlarmStateEnum.STATION_DROP_LINE.getCode();
        } else {
            throw new RuntimeException(String.format("故障信息sectionFlag[%s]:车辆位置错误", alarmInfo.getSectionFlag()));
        }
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        //通用清客客时间
        int clearGuest = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_CLEAR_TIME)) * 1000;
        //区间上报,下一站掉线
        if (alarmInfo.getAlarmState() == AlarmStateEnum.REVERSE_RAIL.getCode() && alarmInfo.getSectionFlag() == 1) {
            // 折返区间特殊处理
            String algTime = alarmInfo.getEndAlarmTime();
            List<String> nowAndNextDay = GraphDataUtil.getNowAndNextDay(alarmInfo.getStartAlarmTime());
            String nowDate = nowAndNextDay.get(0);
            String nextDate = nowAndNextDay.get(1);
            String endAlarmTime;
            if (algTime.startsWith(ALG_SECOND_DAY_TAG)) {
                endAlarmTime = nextDate + " " + (algTime.replaceFirst(ALG_SECOND_DAY_TAG, ""));
            } else {
                endAlarmTime = nowDate + " " + algTime;
            }
            Date endTime = DateUtil.getStringToDate(endAlarmTime, "yyyy-MM-dd HH:mm:ss");
            if (endTime.getTime() < System.currentTimeMillis()) {
                endTime = new Date();
            }
            return DateUtil.getTimeStamp(endTime.getTime() + 15000, "yyyy-MM-dd HH:mm:ss.SSS");
        } else if (alarmInfo.getSectionFlag() == 1) {
            //查询车辆到达指定停车区域站的时刻
            TrainTime time = planRunGraphService.getTrainTime(alarmInfo.getTrainId(), alarmInfo.getStopAreaNumber(), Integer.parseInt(alarmInfo.getOrderNum()));
            log.info("{} 车次到达时间为:{},发车时间为:{}", alarmInfo.getOrderNum(), DateUtil.getDateToString(time.getArriveTime(), "yyyy-MM-dd HH:mm:ss"), DateUtil.getDateToString(time.getStartTime(), "yyyy-MM-dd HH:mm:ss"));
            //车辆到下站时间 加 清客时长
            return DateUtil.getTimeStamp(time.getArriveTime().getTime(), "yyyy-MM-dd HH:mm:ss.SSS");
        } else {//站台上报
            //改成本站扣车后，时间是车辆到本站时间 加 清客时长
            Date startAlarmTime = DateUtil.getStringToDate(alarmInfo.getStartAlarmTime(), "yyyy-MM-dd HH:mm:ss.SSS");
            return DateUtil.getTimeStamp(startAlarmTime.getTime() + clearGuest, "yyyy-MM-dd HH:mm:ss.SSS");
        }
    }

    @Override
    public String strategy() {
        return AlarmTypeConstant.AIR_CONDITIONING_VENTILATE_FAILURE;
    }
}
