package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.service.AdmAlertDetailService;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.AtsByteCmdData;
import com.tct.itd.common.dto.CtrlSwitchCommand;
import com.tct.itd.common.dto.NotifyParam;
import com.tct.itd.common.enums.CommandTypeEnum;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.tias.service.AtsByteCommandFlatService;
import com.tct.itd.tias.service.SendNotifyService;
import com.tct.itd.utils.UidGeneratorUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author kangyi
 * @description 控制权转换命令
 * @date 2022年 03月25日 10:15:50
 */
@Service("ctrlSwitch")
@Slf4j
public class CtrlSwitchHandler implements AidDecSubStepHandler {

    @Resource
    private AtsByteCommandFlatService atsByteCommandClient;

    @Resource
    private AdmAlertDetailService admAlertDetailService;

    @Resource
    private SendNotifyService sendNotifyService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        AtsByteCmdData atsByteCmdData = new AtsByteCmdData();
        //转换控制权
        atsByteCmdData.setCommandTypeEnum(CommandTypeEnum.CTRL_SWITCH);
        CtrlSwitchCommand ctrlSwitchCommand = new CtrlSwitchCommand();
        ctrlSwitchCommand.setTableInfoId(alarmInfo.getTableInfoId());
        //集中站
        ctrlSwitchCommand.setRtuId(alarmInfo.getAlarmConStation());
        //控制权转换类型:1遥控2站控3联锁控
        String param = dto.getParam();
        if (StringUtils.isEmpty(param)) {
            throw new BizException(CodeEnum.CTRL_SWITCH_NO_PARAM);
        }
        try {
            ctrlSwitchCommand.setType(Integer.parseInt(param));
        } catch (NumberFormatException e) {
            throw new BizException(CodeEnum.CTRL_SWITCH_PARAM_ERROR);
        }
        atsByteCmdData.setT(JsonUtils.toJSONString(ctrlSwitchCommand));
        String desc = "将集中站控制权转换为" + getTypeStr(param) + "状态";
        //插入详情
        AdmAlertDetail alertDetail = new AdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "已发送控制权转换命令", new Date(), desc, "0", 2, new Date().getTime());
        admAlertDetailService.insert(alertDetail);
        //推送前端通知
        NotifyParam notifyParam = new NotifyParam();
        notifyParam.setMsgPushEnum(getNotifyTypeEnum(param));
        notifyParam.setInfoId(alarmInfo.getTableInfoId());
        notifyParam.setConStation(Integer.parseInt(alarmInfo.getAlarmConStation()));
        sendNotifyService.sendNotify(notifyParam);
        log.info("开始发送ATS转换控制权命令:{}", JsonUtils.toJSONString(atsByteCmdData));
        atsByteCommandClient.sendCommandTrain(atsByteCmdData);
        log.info("开始发送ATS转换控制权命令完成");
    }

    //获取日志推送枚举类型
    private MsgPushEnum getNotifyTypeEnum(String param) {
        switch (param) {
            case IidsConstPool.CTRL_SWITCH_STATION:
                return MsgPushEnum.SEND_CTRL_STATION_MSG;
            case IidsConstPool.CTRL_SWITCH_REMOTE:
                return MsgPushEnum.SEND_CTRL_REMOTE_MSG;
            default:
                throw new BizException(CodeEnum.CTRL_SWITCH_PARAM_ERROR);
        }
    }

    //获取日志推送枚举类型
    private String getTypeStr(String param) {
        switch (param) {
            case IidsConstPool.CTRL_SWITCH_STATION:
                return "站控";
            case IidsConstPool.CTRL_SWITCH_REMOTE:
                return "遥控";
            default:
                throw new BizException(CodeEnum.CTRL_SWITCH_PARAM_ERROR);
        }
    }


}
