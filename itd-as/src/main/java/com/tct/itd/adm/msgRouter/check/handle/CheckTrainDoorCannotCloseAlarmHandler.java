package com.tct.itd.adm.msgRouter.check.handle;

import com.tct.itd.adm.msgRouter.check.router.CheckConfigHandler;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.constant.NumStrConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author yuelei
 * @Desc 校验是否存在vobc-超时未关闭报警帧
 * @Date 11:14 2023/2/20
 */
@Slf4j
@Service("checkTrainDoorCannotCloseAlarm")
public class CheckTrainDoorCannotCloseAlarmHandler implements CheckConfigHandler {

    @Resource(name = "atoInfoCache")
    private com.github.benmanes.caffeine.cache.Cache<String, List<String>> atoInfoCache;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        ConcurrentMap<String, List<String>> atoMap = atoInfoCache.asMap();
        if (CollectionUtils.isEmpty(atoMap)) {
            throw new BizException(CodeEnum.NO_GET_TRAIN_ATO_MSG);
        }
        if (CollectionUtils.isEmpty(atoMap.get(alarmInfo.getTrainId()))) {
            throw new BizException(CodeEnum.NO_GET_CURRENT_TRAIN_ATO_MSG);
        }
        //获取到VOBC报警帧数据
        List<String> atoList = atoMap.get(alarmInfo.getTrainId());
        log.info("当前列车报警帧信息：{}", atoList.toString());
        // 车门超时未关闭
        String trainDoorNotCloseVobc = atoList.get(NumConstant.ZERO);
        if (!NumStrConstant.ONE.equals(trainDoorNotCloseVobc)) {
            throw new BizException(CodeEnum.NO_GET_OVER_TIME_CLOSE_MSG);
        }

    }
}
