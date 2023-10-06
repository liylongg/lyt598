package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.util.DisCmdSendUtils;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description 车门故障发送电子调度命令
 * @Author zhangjiarui
 * @Date 2022-3-8 14:33:20
 */
@Service(value = "sendDisCmdForTrainDoor")
@Slf4j
public class SendDisCmdForTrainDoorHandler implements AidDecSubStepHandler {


    @Resource
    private DisCmdSendUtils disCmdSendUtils;


    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        String station = disCmdSendUtils.getStationByAcceptType(alarmInfo, dto.getAcceptStation());
        String disCmdContent = dto.getDisCmdContent();
        String recoveryTrainNumber = (String) BasicCommonCacheUtils.get(Cache.RECOVERY_TRAIN_NUMBER);
        disCmdContent = String.format(disCmdContent, recoveryTrainNumber);
        disCmdSendUtils.sendDisCmd(alarmInfo, disCmdContent, station);
    }

}
