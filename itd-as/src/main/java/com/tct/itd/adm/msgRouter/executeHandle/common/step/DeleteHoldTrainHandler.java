package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Description 删除扣车缓存
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("deleteHoldTrain")
@Slf4j
public class DeleteHoldTrainHandler implements AidDecSubStepHandler {

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        log.info("删除扣车缓存");
        BasicCommonCacheUtils.delKey(Cache.HOLD_TRAIN_STATION);
        BasicCommonCacheUtils.delKey(Cache.HOLD_TRAIN_FLAG);
    }
}
