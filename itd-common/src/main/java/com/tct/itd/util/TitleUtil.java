package com.tct.itd.util;


import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.util.AlarmUtil;

import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.SpringContextUtil;

/**
 * @author kangyi
 * @description 推送到前台推荐指令title
 * @date 2022年 06月19日 10:28:34
 */
public class TitleUtil {

    public static String getTitle(AlarmInfo alarmInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(alarmInfo.getExecuteStep() < 0 ? "故障恢复:" : "产生故障:");
        String alarmType = String.valueOf(alarmInfo.getAlarmType());
        if(AlarmTypeConstant.INTERLOCKING_DOUBLE_PARENT.equals(alarmType) ||
                AlarmTypeConstant.SIGNAL_ELECTRIC_PARENT.equals(alarmType) ||
                AlarmTypeConstant.LARGE_PASSENGER.equals(alarmType)){
            sb.append(alarmInfo.getAlarmSite());
        }
        if(AlarmTypeConstant.SWITCH_FAILURE.equals(alarmType)){
            sb.append(alarmInfo.getAlarmSite());
            sb.append(alarmInfo.getSwitchName());
            sb.append("号道岔");
        }
        if(AlarmTypeConstant.AXLE_COUNTER.equals(alarmType)){
            sb.append(alarmInfo.getAlarmSite());
            sb.append(alarmInfo.getAxleCounterName());
            sb.append("计轴区段");
        }
        if(AlarmTypeConstant.TRACTION_POWER_PARENT.equals(alarmType)){
            sb.append(alarmInfo.getAlarmSite());
            sb.append("区段");
        }else {
            AlarmUtil alarmUtil = SpringContextUtil.getBean(AlarmUtil.class);
            //获取表号
            String serverNum = alarmUtil.getServerNumByTrainId(alarmInfo.getTrainId());
            sb.append(serverNum).append(alarmInfo.getOrderNum());
            sb.append("次列车（车组号:");
            sb.append(alarmInfo.getTrainId());
            sb.append(")");
        }

        sb.append("发生");
        if (String.valueOf(alarmInfo.getAlarmType()).equals(AlarmTypeConstant.LARGE_PASSENGER)){
            sb.append(alarmInfo.getLargePassFlowCrowdLevel()).append("级").append(AdmAlertDetailTypeService.getDescribeByCode(alarmInfo.getAlarmTypeDetail()));
        }else {
            sb.append(AdmAlertDetailTypeService.getDescribeByCode(alarmInfo.getAlarmTypeDetail()));
        }
        return sb.toString();
    }
}
