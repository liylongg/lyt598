package com.tct.itd.adm.iconstant;

import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.SpringContextUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author yuelei
 * @Desc
 * @Date 19:14 2022/6/13
 */
@Getter
@AllArgsConstructor
public enum AidStepTitleEnum {

    ALARM_SITE("AlarmSite", "故障地点"),

    ALARM_TYPE_DETAIL_STR("AlarmTypeString", "故障类型描述"),

    TRAIN_ID("TrainId", "车组号"),

    ORDER_NUM("OrderNum", "车次号");

    private String code;

    private String desc;


    public static String replace(String title, AlarmInfo alarmInfo, String alarmTypeDetailStr){
        if(title == null || title.length() == 0){
            return "";
        }
        if(title.contains(ALARM_SITE.getCode())){
            title = title.replaceAll(ALARM_SITE.getCode(), alarmInfo.getAlarmSite());
        }
        if(title.contains(ALARM_TYPE_DETAIL_STR.getCode())){
            title = title.replaceAll(ALARM_TYPE_DETAIL_STR.getCode(), alarmTypeDetailStr);
        }
        if(title.contains(TRAIN_ID.getCode())){
            title = title.replaceAll(TRAIN_ID.getCode(), alarmInfo.getTrainId());
        }
        if(title.contains(ORDER_NUM.getCode())){
            AlarmUtil alarmUtil = SpringContextUtil.getBean(AlarmUtil.class);
            //获取表号
            String serverNum = alarmUtil.getServerNumByTrainId(alarmInfo.getTrainId());
            title = title.replaceAll(ORDER_NUM.getCode(), serverNum + alarmInfo.getOrderNum());
        }
        return title;
    }

}
