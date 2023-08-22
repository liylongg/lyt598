package com.tct.itd.adm.aop;

import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.basedata.service.NotifyLogService;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.NotifyLogDto;
import com.tct.itd.common.dto.NotifyLogOutDto;
import com.tct.itd.common.dto.NotifyParam;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.tias.service.SendNotifyService;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/***
 * @Description 操作日志切面
 * @Author yuelei
 * @Date 2022/2/9 17:49
 */
@Aspect
@Component
@Slf4j
public class ExecAidNotifyLogAspect {

    private static final Map<Integer, String> TO_CHINESE = new HashMap<>();

    @Resource
    private NotifyLogService notifyLogService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private SendNotifyService sendNotifyService;

    private static final String OVER_TIME = "超时";

    static {
        TO_CHINESE.put(1, "一");
        TO_CHINESE.put(2, "二");
        TO_CHINESE.put(3, "三");
        TO_CHINESE.put(4, "四");
        TO_CHINESE.put(5, "五");
        TO_CHINESE.put(6, "六");
    }


    @Pointcut("@annotation(com.tct.itd.adm.annotation.ExecAidNotifyLog)")
    private void cutMethod() {

    }

    /**
     * 前置通知：在目标方法执行前调用
     */
    @Before("cutMethod()")
    public void begin(JoinPoint joinPoint) {
        log.info("------进入推荐指令执行切面,记录操作日志-------");
        Object[] args = joinPoint.getArgs();
        //获取方法传入参数
        AuxiliaryDecision auxiliaryDecision = (AuxiliaryDecision) args[0];
        log.info("进入切面,获取传入参数:{}", auxiliaryDecision.toString());
        long infoId = auxiliaryDecision.getTableInfoId();
        NotifyLogDto dto = new NotifyLogDto();
        dto.setInfoId(infoId).setType(0);
        if (auxiliaryDecision.getExecuteStep() >= 0) {
            //放弃推荐指令
            if (auxiliaryDecision.getExecuteStep() == 0) {
                //获取告警信息
                AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
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
                if(auxiliaryDecision.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE_SLOW_DOWN)){
                    if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_3){
                        dto.setMsg("放弃第二次推荐指令");
                    }
                    if(alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4){
                        dto.setMsg("放弃第三次推荐指令");
                    }
                }
                if(auxiliaryDecision.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE_SLOW_DOWN)){
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
                    dto.setMsg(String.format("放弃第%s次推荐指令", TO_CHINESE.get(Math.abs(alarmInfo.getExecuteStep()))));
                }
                if(!Objects.isNull(auxiliaryDecision.getIdOverTime()) && auxiliaryDecision.getIdOverTime() == 1){
                    dto.setMsg(OVER_TIME + dto.getMsg());
                }
            } else {
                if(auxiliaryDecision.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE)){
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_3){
                        dto.setMsg("执行第二次推荐指令");
                    }
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4 || auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_6){
                        dto.setMsg("执行第三次推荐指令");
                    }
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_5){
                        dto.setMsg("执行第四次推荐指令");
                    }
                }
                if(auxiliaryDecision.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE_SLOW_DOWN)){
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_3){
                        dto.setMsg("执行第二次推荐指令");
                    }
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4){
                        dto.setMsg("执行第三次推荐指令");
                    }
                }
                if(auxiliaryDecision.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE_SLOW_DOWN)){
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_3)
                        dto.setMsg("执行第三次推荐指令");
                    }
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4){
                        dto.setMsg("执行第二次推荐指令");
                    }
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_5){
                        dto.setMsg("执行第三次推荐指令");
                    }
                }
                if (auxiliaryDecision.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE)){
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4){
                        dto.setMsg("执行第二次推荐指令");
                    }
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_5){
                        dto.setMsg("执行第三次推荐指令");
                    }
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_6){
                        dto.setMsg("执行第四次推荐指令");
                    }
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_7){
                        dto.setMsg("执行第五次推荐指令");
                    }
                }
                if(auxiliaryDecision.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_INTO_STATION)
                    || auxiliaryDecision.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_OUT_STATION)
                ){
                    if(auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_3){
                        dto.setMsg("执行第二次推荐指令");
                    }
                }
                if(StringUtils.isEmpty(dto.getMsg())){
                    dto.setMsg(String.format("执行第%s次推荐指令", TO_CHINESE.get(Math.abs(auxiliaryDecision.getExecuteStep()))));
                }
        }
        //executeStep < 0 表示故障恢复
        else {
            //获取告警信息
            AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
            Assert.notNull(alarmInfo, "生命周期已结束，推荐指令流程结束");
            //-99表示放弃故障恢复
            String msg = "";
            if (IidsConstPool.EXECUTE_STEP_0_99 == auxiliaryDecision.getExecuteStep()) {
                msg = "放弃故障恢复推荐指令";
            } else {
                msg = "执行故障恢复推荐指令";
            }
            dto.setMsg(msg);
            List<NotifyLogOutDto> dtoList = notifyLogService.selectList(infoId);
            //不允许插入重复日志
            if(!dtoList.isEmpty()){
                List<String> collect = dtoList.stream().map(NotifyLogOutDto::getMsg).collect(Collectors.toList());
                if(!collect.contains(msg)){
                    dto.setMsg(msg);
                }
            }
            if (!Objects.isNull(auxiliaryDecision.getIdOverTime()) && auxiliaryDecision.getIdOverTime() == 1) {
                dto.setMsg(OVER_TIME + dto.getMsg());
            }
        }
        if (!StringUtils.isEmpty(dto.getMsg())) {
            log.info("插入操作日志到数据库notifyLog:{}", JsonUtils.toJSONString(dto));
            log.info("--------操作日志插入成功--------");
            NotifyParam notifyParam = new NotifyParam();
            notifyParam.setInfoId(dto.getInfoId());
            notifyParam.setMsgPushEnum(MsgPushEnum.EXECUTE_AID_LOG_MSG);
            notifyParam.setMsg(dto.getMsg());
            if(dto.getMsg().startsWith(OVER_TIME)){
                notifyParam.setType(1);
            }else{
                notifyParam.setType(0);
            }
            sendNotifyService.sendNotify(notifyParam);
            log.info("推送执行推荐指令日志成功");
        } else {
            log.info("-----该步骤不需要插入日志-------");
        }
    }

}
