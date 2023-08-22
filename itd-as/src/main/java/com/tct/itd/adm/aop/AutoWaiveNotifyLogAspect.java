package com.tct.itd.adm.aop;

import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.basedata.service.NotifyLogService;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.NotifyLogDto;
import com.tct.itd.common.dto.NotifyParam;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.tias.service.SendNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/***
 * @Description 操作日志切面
 * @Author yuelei
 * @Date 2022/2/9 17:49
 */
@Aspect
@Component
@Slf4j
public class AutoWaiveNotifyLogAspect {

    private static final Map<Integer, String> TO_CHINESE = new HashMap<>();

    @Resource
    private NotifyLogService notifyLogService;

    @Resource
    private SendNotifyService sendNotifyService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    static {
        TO_CHINESE.put(1, "一");
        TO_CHINESE.put(2, "二");
        TO_CHINESE.put(3, "三");
        TO_CHINESE.put(4, "四");
        TO_CHINESE.put(5, "五");
        TO_CHINESE.put(6, "六");
    }


    @Pointcut("@annotation(com.tct.itd.adm.annotation.AutoWaiveNotifyLog)")
    private void cutMethod() {

    }

    /**
     * 前置通知：在目标方法执行前调用
     */
    @Before("cutMethod()")
    public void begin(JoinPoint joinPoint) {
        log.info("------进入自动执行放弃切面,记录操作日志-------");
        Object[] args = joinPoint.getArgs();
        //获取方法传入参数
        long infoId = (Long) args[0];
        log.info("进入切面,获取传入参数:{}", infoId);
        //获取告警信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        NotifyLogDto dto = new NotifyLogDto();
        if(alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE)){
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_3){
                dto.setMsg("放弃第二次推荐指令");
            }
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4 || alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_6){
                dto.setMsg("放弃第三次推荐指令");
            }
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_5){
                dto.setMsg("放弃第四次推荐指令");
            }
        }
        if (alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE)){
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4){
                dto.setMsg("放弃第二次推荐指令");
            }
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_5 || alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_7){
                dto.setMsg("放弃第三次推荐指令");
            }
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_6){
                dto.setMsg("放弃第四次推荐指令");
            }
        }
        if(alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_INTO_STATION)
                || alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_OUT_STATION)
        ){
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_3){
                dto.setMsg("放弃第二次推荐指令");
            }
        }
        if(alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE_SLOW_DOWN)){
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_3){
                dto.setMsg("放弃第二次推荐指令");
            }
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4){
                dto.setMsg("放弃第三次推荐指令");
            }
        }
        if(alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE_SLOW_DOWN)){
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_3){
                dto.setMsg("放弃第三次推荐指令");
            }
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4){
                dto.setMsg("放弃第二次推荐指令");
            }
            if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_5){
                dto.setMsg("放弃第三次推荐指令");
            }
        }
        if(StringUtils.isEmpty(dto.getMsg())){
            if(alarmInfo.getExecuteStep()<0){
                dto.setMsg("放弃故障恢复推荐指令");
            }else {
                dto.setMsg(String.format("放弃第%s次推荐指令", TO_CHINESE.get(Math.abs(alarmInfo.getExecuteStep()))));
            }
        }


        dto.setInfoId(infoId)
           .setType(1);
        NotifyParam notifyParam = new NotifyParam();
        notifyParam.setInfoId(alarmInfo.getTableInfoId());
        notifyParam.setMsgPushEnum(MsgPushEnum.EXECUTE_AID_LOG_MSG);
        notifyParam.setMsg(dto.getMsg());
        notifyParam.setType(1);
        sendNotifyService.sendNotify(notifyParam);
        log.info("推送执行推荐指令日志成功");
    }

}
