package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.common.dto.AtsByteCmdData;
import com.tct.itd.common.dto.NotifyParam;
import com.tct.itd.common.dto.TrainCtrlPrv;
import com.tct.itd.common.dto.TrainNumberAdjust;
import com.tct.itd.common.enums.CommandTypeEnum;
import com.tct.itd.constant.NumStrConstant;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.tias.service.AtsByteCommandFlatService;
import com.tct.itd.tias.service.SendNotifyService;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description 执行推荐指令-故障站立即清人掉线
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("clearPeopleHomeStation")
@Slf4j
public class ClearPeopleHomeStationHandler implements AidDecSubStepHandler {

    @Resource
    private AtsByteCommandFlatService atsByteCommandClient;
    @Resource
    private SendNotifyService sendNotifyService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        TrainNumberAdjust trainNumberAdjust = new TrainNumberAdjust();
        trainNumberAdjust.setTrainId(alarmInfo.getTrainId());
        log.info("发送清客指令至ATS");
        if(NumStrConstant.ZERO.equals(alarmInfo.getPlatformId())){
            throw new BizException(CodeEnum.PLATFORM_ID_NULL);
        }
        NotifyParam notifyParam = new NotifyParam();
        notifyParam.setMsgPushEnum(MsgPushEnum.TRAIN_CLEAR_PASSENGER_CMD_LOG_MSG);
        notifyParam.setInfoId(alarmInfo.getTableInfoId());
        notifyParam.setPlatformId(Integer.parseInt(alarmInfo.getPlatformId()));
        notifyParam.setOrderNo(alarmInfo.getOrderNum());
        sendNotifyService.sendNotify(notifyParam);

        TrainCtrlPrv trainCtrlPrv = new TrainCtrlPrv();
        trainCtrlPrv.setAlarmInfoId(String.valueOf(alarmInfo.getTableInfoId()));
        trainCtrlPrv.setOrderNo(alarmInfo.getOrderNum());
        trainCtrlPrv.setPlatformId(Integer.parseInt(alarmInfo.getPlatformId()));

        AtsByteCmdData atsByteCmdData = new AtsByteCmdData(CommandTypeEnum.CLEAR_TRAIN_RETURN,
                JsonUtils.toJSONString(trainNumberAdjust), trainCtrlPrv, Integer.parseInt(alarmInfo.getPlatformId()));
        log.info("收到向ATS发送指令,命令类型:{}-{}", atsByteCmdData.getCommandTypeEnum().getCommandName(), atsByteCmdData.getCommandTypeEnum().getCommandValue());
        if (log.isDebugEnabled()) {
            log.debug("收到向ATS发送指令,命令内容:{}", JsonUtils.toJSONString(atsByteCmdData));
        }
        atsByteCommandClient.sendCommandTrain(atsByteCmdData);
        log.info("已完成智能调度向ATS系统发送命令");
    }
}
