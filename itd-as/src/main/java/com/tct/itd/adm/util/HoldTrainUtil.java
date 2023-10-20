package com.tct.itd.adm.util;

import com.tct.itd.basedata.dfsread.service.handle.StopRegionDataService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.dto.HoldOffTrainTimeDto;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @ClassName HoldTrainUtil
 * @Description 扣车相关工具类
 * @Author yl
 * @Date 2021/4/29 14:59
 */
@Slf4j
@Component
public class HoldTrainUtil {
    @Resource
    private StopRegionDataService stopRegionDataService;
    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    /***
     * @Description 获取当前车站停车区域ID
     * @Author yuelei
     * @Date 2021/10/21 10:49
     * @param
     * @param alarmInfo
     * @return java.util.List<java.lang.Integer>
     */
    public List<Integer> getLocalStopAreaId(AlarmInfo alarmInfo){
        List<Integer> stopAreaIdList = new ArrayList<>();
        //不扣本站
        String platformId = alarmInfo.getPlatformId();
        Integer stopAreaId = stopRegionDataService.getStopAreaByPlatformId(Integer.parseInt(platformId));
        stopAreaIdList.add(stopAreaId);
        return stopAreaIdList;
    }

    /**
     * 判断列车是否晚点2分钟
     * @param alarmInfo
     */
    public void checkTrainIsLate(AlarmInfo alarmInfo) {
        // 查看列车晚点是否超过2分钟
        String platformId = alarmInfo.getPlatformId();
        String[] platformIdArray= platformId.split("/");
        log.info("故障类型："+alarmInfo.getAlarmTypeDetail()+"，开始扣车站台id:【{}】", JsonUtils.toJSONString(platformIdArray));
        for (String p : platformIdArray) {
            HoldOffTrainTimeDto holdOffTrainTimeDto = (HoldOffTrainTimeDto) BasicCommonCacheUtils.hGet(Cache.HOLD_AND_OFF_TRAIN, String.valueOf(p),HoldOffTrainTimeDto.class);
            log.info("站台id:【{}】对应的扣车信息:【{}】", p, holdOffTrainTimeDto);
            if (Objects.isNull(holdOffTrainTimeDto)) {
                log.warn("站台id:【{}】对应的站台没有扣车信息", p);
                continue;
            }
            String trainId = holdOffTrainTimeDto.getTrainId();
            TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent(trainId);
            // 该站台上扣住的车早晚点时间
            assert tiasTraceInfo != null;
            Integer otpTime = tiasTraceInfo.getOtpTime();
            log.info("站台id【{}】上扣住的车晚点时间【{}】",p,otpTime);
            if (120 < otpTime) {
                BasicCommonCacheUtils.set(Cache.TWO_MINUTES_LATE_RECOVERY_CHANGE_GRAPH, "1");
                break;
            }
        }
    }

}
