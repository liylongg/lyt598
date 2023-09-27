package com.tct.itd.adm.msgRouter.check.handle;

import com.tct.itd.adm.msgRouter.check.router.CheckConfigHandler;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/***
 * @Description 故障录入校验-非区间停稳
 * @Author yuelei
 * @Date 2021/12/14 16:02
 */
@Slf4j
@Service("standStill")
public class StandStillHandler implements CheckConfigHandler {

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        if(StringUtils.isEmpty(alarmInfo.getTrainId())  || IidsConstPool.TRAIN_ID_0_1.equals(alarmInfo.getTrainId()) ){
            return;
        }
        TiasTraceInfo tiasTraceInfo =trainTraceCache.getIfPresent(AlarmUtil.getTrainId(alarmInfo));
        //非区间校验停稳
        if(alarmInfo.getSectionFlag() != 1){
            //列车要停稳
            if(!tiasTraceInfo.getTrainArrFlg().equals(IidsConstPool.STAND_STILL)){
                throw new BizException(CodeEnum.TRAIN_NOT_STAND_STILL_NOT_ALLOW_SECTION);
            }
        }
    }
}
