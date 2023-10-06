package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.dto.AdmNoticeDto;
import com.tct.itd.constant.NumStrConstant;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.BasicCommonCacheUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @classname: CtrlSwitchPopoverHandler
 * @description: 控制权转换弹窗
 * @author: liyunlong
 * @date: 2023/3/1 9:29
 */
@Service("ctrlSwitchPopover")
public class CtrlSwitchPopoverHandler implements AidDecSubStepHandler {

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AppPushService appPushService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        // 1:遥控弹窗 2:站控弹窗
        String param = dto.getParam();
        if (StringUtils.isEmpty(param)) {
            throw new BizException(CodeEnum.CTRL_SWITCH_POPOVER_NO_PARAM);
        }
        String msg;
        if (NumStrConstant.ONE.equals(param)) {
            msg = NumStrConstant.STATION_CONFIRM_RECYCLE_MSG;
        } else {
            msg = NumStrConstant.STATION_CONFIRM_SEND_DOWN_MSG;
        }
        alarmInfo.setNoticeMsg(msg);
        alarmInfo.setCrtlSwitch(Integer.parseInt(param));
        AdmNoticeDto admNoticeDto = new AdmNoticeDto(alarmInfo,
                admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
        appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(WebNoticeCodeConst.ADM_INSERT_ALARM_CONFIRM,
                Integer.toString(alarmInfo.getStationId()), admNoticeDto));
        //推送给车站的弹窗，缓存用于故障放弃时关闭车站弹窗
        BasicCommonCacheUtils.set(Cache.STATION_CONFIRM_IDS, alarmInfo.getStationId());
    }
}
