package com.tct.itd.adm.msgRouter.check.handle;

import com.tct.itd.adm.msgRouter.check.router.CheckConfigHandler;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.common.constant.FireInfoPushAlertMsgConstant;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;

/***
 * @Description 故障录入校验-判断是否存在录入列车的车次追踪缓存
 * @Author yuelei
 * @Date 2021/12/14 16:02
 */
@Slf4j
@Service("existTrainTraceForCurrent")
public class ExistTrainTraceForCurrentHandler implements CheckConfigHandler {

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;


    @Override
    public void handle(AlarmInfo alarmInfo) {
        if (!StringUtils.hasText(alarmInfo.getTrainId()) || IidsConstPool.TRAIN_ID_0_1.equals(alarmInfo.getTrainId())) {
            return;
        }
        Map<String, TiasTraceInfo> traceInfoMap = trainTraceCache.asMap();
        if (traceInfoMap.isEmpty()) {
            log.error("未获取到车次追踪信息");
            throw new BizException(CodeEnum.NO_GET_TRAIN_TRACE_MSG);
        }
        String trainId = AlarmUtil.getTrainId(alarmInfo);
        TiasTraceInfo traceInfo = trainTraceCache.getIfPresent(trainId);
        if (traceInfo == null) {
            log.error("未获取到列车{}的车次追踪信息", trainId);
            //故障录入信息失败推送和利时
            AlarmUtil.sendFireInfoPush(alarmInfo, FireInfoPushAlertMsgConstant.NO_GET_TRAIN_TRACE);
            //抛出异常
            throw new BizException(CodeEnum.NO_GET_TRAIN_TRACE);
        }
    }
}
