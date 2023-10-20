package com.tct.itd.adm.util;

import com.tct.itd.adm.service.FireInfoPushService;
import com.tct.itd.basedata.dfsread.service.handle.*;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.PlatformInfo;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.common.dto.TrainCtrlPrv;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.util.FormatUtil;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.UidGeneratorUtils;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.LocalDateTimeUtils;
import com.tct.itd.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

/**
 * @ClassName AlarmUtil
 * @Description 告警工具类
 * @Author YHF
 * @Date 2021/4/29 14:59
 */
@Slf4j
@Component
public class AlarmUtil {
    @Resource
    private PlatformInfoService platformInfoService;
    @Resource
    private StopRegionDataService stopRegionDataService;
    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    /**
     * 扣车和抬车相关信息
     *
     * @param stopAreaId
     * @param alarmInfo
     * @param type
     * @return
     */
    public TrainCtrlPrv getTrainCtrlPrv(Integer stopAreaId, AlarmInfo alarmInfo, Integer type) {
        TrainCtrlPrv trainCtrl = new TrainCtrlPrv();
        trainCtrl.setKey(UidGeneratorUtils.getUID());
        trainCtrl.setType(type);

        // 之后需要修改 中间站台同一停车区域存在两个站台id
        trainCtrl.setPlatformId(platformInfoService.getPlatformIdByStopArea(stopAreaId));
        trainCtrl.setOvertime(300L);
        return trainCtrl;
    }

    public TrainCtrlPrv getTrainCtrlPrv(Integer platformId,Integer type) {
        TrainCtrlPrv trainCtrl = new TrainCtrlPrv();
        trainCtrl.setKey(UidGeneratorUtils.getUID());
        trainCtrl.setType(type);

        // 之后需要修改 中间站台同一停车区域存在两个站台id
        trainCtrl.setPlatformId(platformId);
        trainCtrl.setOvertime(300L);
        return trainCtrl;
    }

    /**
     * 扣车和抬车相关信息
     *
     * @param platformId
     * @param type
     * @return
     */
    public TrainCtrlPrv getTrainCtrlPrvForTrainDoor(Integer platformId, Integer type) {
        TrainCtrlPrv trainCtrl = new TrainCtrlPrv();
        trainCtrl.setKey(UidGeneratorUtils.getUID());
        trainCtrl.setType(type);

        // 之后需要修改 中间站台同一停车区域存在两个站台id
        trainCtrl.setPlatformId(platformId);
        trainCtrl.setOvertime(300L);
        return trainCtrl;
    }

    /**
     * 根据站台编号获取站台数据
     * 要么是上行站台数据，要么是下行站台数据
     *
     * @param platformId 站台编号
     * @return
     */
    public PlatformInfo getPlatformInfo(int platformId) {
        return platformInfoService.getPlatformByPId(platformId);
    }

    /*
     * @Description 判断故障车门是否为连续及发生时间是否为高峰时间
     * @Date 15:59 2021/6/1
     * @param alarmInfo
     * @return boolean
     **/
    public boolean manyDoorAlarmInRush(AlarmInfo alarmInfo) {
        return false;
//        Map<String, TrafficParamDTO> rushTimeClass = SysParamKit.getRushTimeClass();
//        String morningPeakStart = rushTimeClass.get(IidsSysParamPool.MORNING_PEAK_START).getValue();//早高峰开始时间
//        String morningPeakEnd = rushTimeClass.get(IidsSysParamPool.MORNING_PEAK_END).getValue();//早高峰结束时间
//        String eveningPeakStart = rushTimeClass.get(IidsSysParamPool.EVENING_PEAK_START).getValue();//晚高峰开始时间
//        String eveningPeakEnd = rushTimeClass.get(IidsSysParamPool.EVENING_PEAK_END).getValue();//晚高峰结束时间
//        Date alarmDateTime = DateUtil.getStringToDate(alarmInfo.getStartAlarmTime(), "yyyy-MM-dd HH:mm:ss.SSS");
//        return (isTimeInRange(alarmDateTime, morningPeakStart, morningPeakEnd) || isTimeInRange(alarmDateTime, eveningPeakStart, eveningPeakEnd));
    }

    /**
     * @param alarmTime
     * @param startTimeStr
     * @param endTimeStr
     * @return boolean
     * @Description 判断时分秒是否在时间范围内
     * @Author zhangyinglong
     * @Date 2021/6/16 15:22
     */
    public static boolean isTimeInRange(Date alarmTime, String startTimeStr, String endTimeStr) {
        LocalDateTime localDateTime = LocalDateTimeUtils.dateToLocalDateTime(alarmTime).withNano(0);

        String[] startTimeStrArr = startTimeStr.split(":");
        LocalDateTime start = LocalDateTime.now().withHour(Integer.parseInt(startTimeStrArr[0]))
                .withMinute(Integer.parseInt(startTimeStrArr[1]))
                .withSecond(Integer.parseInt(startTimeStrArr[2]))
                .withNano(0);

        String[] endTimeStrArr = endTimeStr.split(":");
        LocalDateTime end = LocalDateTime.now().withHour(Integer.parseInt(endTimeStrArr[0]))
                .withMinute(Integer.parseInt(endTimeStrArr[1]))
                .withSecond(Integer.parseInt(endTimeStrArr[2]))
                .withNano(0);
        return (start.isBefore(localDateTime) && end.isAfter(localDateTime)) || start.isEqual(localDateTime) || end.isEqual(localDateTime);
    }

    /**
     * @param alarmInfo
     * @return java.lang.String
     * @Description 获取车组号
     * @Author yuelei
     * @Date 2021/12/14 16:12
     */
    public static String getTrainId(AlarmInfo alarmInfo) {
        //车组号
        String trainId = alarmInfo.getTrainId();
        if (trainId == null || trainId.length() == 0) {
            throw new BizException(CodeEnum.NO_FOUND_TRAIN_ID);
        }
        log.info("前端录入车组号为：{}", alarmInfo.getTrainId());
        return trainId;
    }

    /***
     * @Description 解析车次号
     * @Author yuelei
     * @Date 2022/2/9 9:31
     * @param orderNum
     * @return java.lang.String
     */
    public static String getOrderNum(String orderNum) {
        if (orderNum.length() == 1) {
            return "0" + orderNum;
        } else {
            return orderNum;
        }
    }

    /**
     * 根据激活端方向获取上下行
     * @return
     */
    public static Integer getUpdownByActiveEnd(Integer activeEnd){
        Integer upDown=0;
        if (activeEnd.equals(1)){
            upDown=85;
        }
        if (activeEnd.equals(2)){
            upDown=170;
        }
        return upDown;
    }

    /**
     * @param alarmInfo
     * @return void
     * @Description 推送和利时
     * @Author yuelei
     * @Date 2021/9/24 15:08
     */
    public static void sendFireInfoPush(AlarmInfo alarmInfo, String msg) {
        FireInfoPushService fireInfoPushService = SpringContextUtil.getBean(FireInfoPushService.class);
        //alarmState=1，表示产生故障
        //是否推送和利时开关 0 关闭  1开启
        String fireInfoPushSwitch = SysParamKit.getByCode(IidsSysParamPool.FIRE_INFO_PUSH_SWITCH);
        if (alarmInfo.getAlarmState() == 1 && "1".equals(fireInfoPushSwitch)) {
            //和利时推送报警信息
            fireInfoPushService.pushFireAlarm(alarmInfo, msg);
        }
    }

    /**
     * 通过前端录入的车组号获取当前故障站台id(列车所在站台)
     *
     * @param trainId 车组号
     * @return 站台id
     */
    public Integer getPlatformIdByTrainTrace(String trainId) {
        TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent(trainId);
        if (tiasTraceInfo == null) {
            log.error("列车{}的车次追踪信息为空", trainId);
            //抛出异常
            throw new BizException(CodeEnum.NO_GET_TRAIN_TRACE);
        }
        switch (tiasTraceInfo.getActiveEnd()) {
            case 0:
                log.error("列车{}的车次追踪信息里激活端为未知", trainId);
                throw new BizException(CodeEnum.TRAIN_TRACE_ACTIVE_END_UNKNOWN);
            case 1:
                return platformInfoService.getPlatformIdByStationId(IidsConstPool.TRAIN_UP, tiasTraceInfo.getStopAreaNumber());
            case 2:
                return platformInfoService.getPlatformIdByStationId(IidsConstPool.TRAIN_DOWN, tiasTraceInfo.getStopAreaNumber());
            default:
                log.error("列车{}的车次追踪信息里激活端为:{},无法解析", trainId, tiasTraceInfo.getActiveEnd());
                throw new BizException(CodeEnum.TRAIN_TRACE_ACTIVE_END_UNKNOWN);
        }

    }

    /**
     * 通过前端录入的车组号获取当前区间故障站台id(列车所在站台)
     *
     * @param trainId 车组号
     * @return 站台id
     */
    public Integer getPlatformIdByTrainTraceInSection(String trainId) {
        TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent(trainId);
        log.info("clearPeopleNextStation传入的车次追踪数据{}", trainId);
        log.info("clearPeopleNextStation获取到的车次追踪数据{}", tiasTraceInfo);
        if (tiasTraceInfo == null) {
            log.error("列车{}的车次追踪信息为空", trainId);
            //抛出异常
            throw new BizException(CodeEnum.NO_GET_TRAIN_TRACE);
        }
        switch (tiasTraceInfo.getActiveEnd()) {
            case 0:
                log.error("列车{}的车次追踪信息里激活端为未知", trainId);
                throw new BizException(CodeEnum.TRAIN_TRACE_ACTIVE_END_UNKNOWN);
            case 1:
                return platformInfoService.getPlatformIdByStationId(IidsConstPool.TRAIN_UP, tiasTraceInfo.getNextStopAreaId());
            case 2:
                return platformInfoService.getPlatformIdByStationId(IidsConstPool.TRAIN_DOWN, tiasTraceInfo.getNextStopAreaId());
            default:
                log.error("列车{}的车次追踪信息里激活端为:{},无法解析", trainId, tiasTraceInfo.getActiveEnd());
                throw new BizException(CodeEnum.TRAIN_TRACE_ACTIVE_END_UNKNOWN);
        }
    }

    public Integer getStationIdByTrainTraceInSection(String trainId) {
        TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent(trainId);
        log.info("sendDisCmdClearPeople传入的车次追踪数据{}", trainId);
        log.info("sendDisCmdClearPeople获取到的车次追踪数据{}", tiasTraceInfo);
        if (tiasTraceInfo == null) {
            log.error("列车{}的车次追踪信息为空", trainId);
            //抛出异常
            throw new BizException(CodeEnum.NO_GET_TRAIN_TRACE);
        }
        return stopRegionDataService.getStationIdByStopAreaId(tiasTraceInfo.getNextStopAreaId());
    }


    /**
     * @Author yuelei
     * @Desc  获取时分秒，如果在00:00:00
     * @Date 11:44 2022/7/1
     */
    public static String getHms(long time){
        String timeStamp = "";
        try {
            timeStamp = new SimpleDateFormat("HH:mm:ss").format(time);
        } catch (Exception e) {
            log.error("日期格式化异常：{}", e.getMessage(), e);
        }
        //跨天运营起始时间
        String secondDayTime = "00:00:00";
        //跨天运营结束时间
        String endDayTime = SysParamKit.getByCode(IidsSysParamPool.UPDATE_GRAPH_TIME);
        //yy-mm-dd
        String date = DateUtil.getDate();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            long startTime = df.parse(date + " " + secondDayTime).getTime();
            long endTime= df.parse(date + " " + endDayTime).getTime();
            long trainTime =  df.parse(date + " " + timeStamp).getTime();
            if(trainTime >= startTime && trainTime <= endTime){
                timeStamp = "1." + timeStamp;
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return timeStamp;
    }

    /**
     * @Author yuelei
     * @Desc 根据车组号取拿表号
     * @Date 14:25 2022/10/29
     */
    public String getServerNumByTrainId(String trainId){
        if (StringUtils.isEmpty(trainId) || trainId.equals("-1")) {
            return "";
        }
        TiasTraceInfo traceInfo = trainTraceCache.getIfPresent(trainId);
        if (Objects.isNull(traceInfo)) {
            log.error("根据车组号【{}】获取车次追踪数据失败！", trainId);
            return "";
        }
        return FormatUtil.formatServerNumber(traceInfo.getServerNumber());
    }

    public static void main(String[] args) throws ParseException {
//        Date stringToDate = DateUtil.getStringToDate("2021-6-16 7:00:00.000", "yyyy-MM-dd HH:mm:ss.SSS");
//        LocalDateTime localDateTime = LocalDateTimeUtils.dateToLocalDateTime(stringToDate);
        System.out.println(isTimeInRange(DateUtil.getStringToDate("2021-7-12 7:00:00.000", "yyyy-MM-dd HH:mm:ss.SSS"), "7:00:00", "9:00:00"));
        System.out.println(isTimeInRange(DateUtil.getStringToDate("2021-7-12 9:00:00.000", "yyyy-MM-dd HH:mm:ss.SSS"), "7:00:00", "9:00:00"));
        System.out.println(isTimeInRange(DateUtil.getStringToDate("2021-7-12 7:00:00.001", "yyyy-MM-dd HH:mm:ss.SSS"), "7:00:00", "9:00:00"));
        System.out.println(isTimeInRange(DateUtil.getStringToDate("2021-7-12 6:00:00.001", "yyyy-MM-dd HH:mm:ss.SSS"), "7:00:00", "9:00:00"));
        System.out.println(isTimeInRange(DateUtil.getStringToDate("2021-7-12 9:00:00.001", "yyyy-MM-dd HH:mm:ss.SSS"), "7:00:00", "9:00:00"));
        System.out.println(isTimeInRange(DateUtil.getStringToDate("2021-7-12 9:00:00.900", "yyyy-MM-dd HH:mm:ss.SSS"), "7:00:00", "9:00:00"));
        System.out.println(isTimeInRange(DateUtil.getStringToDate("2021-7-12 9:00:01.900", "yyyy-MM-dd HH:mm:ss.SSS"), "7:00:00", "9:00:00"));
    }

    /**
     * 将区间故障地点旋转
     * @return
     */
    public static String getRotateString(String str){
        int index=str.indexOf("_");
        String newStr=str.substring(index+1,str.length())+"_"+str.substring(0,index);
        return newStr;
    }
}
