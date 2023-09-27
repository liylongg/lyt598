package com.tct.itd.adm.msgRouter.service;

import com.google.common.collect.Lists;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep.*;
import com.tct.itd.adm.msgRouter.handle.TractionPowerHandler;
import com.tct.itd.adm.msgRouter.handle.VirtualGroupHandler;
import com.tct.itd.adm.service.*;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.dto.AdmNoticeDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @Description : ADM中各个故障handler都有可能用到的方法
 * @Author : zhangjiarui
 * @Date : Created in 2021/8/17
 */
@Slf4j
@Component
public class AdmCommonMethodService {
    private static final String MINUS_ONE = "-1";
    private static final int TWO = 2;
    private static final int THREE = 3;
    private static final int FOUR = 4;
    private static final int FIVE = 5;
    private static final String TWO_STRING = "2";

    @Resource
    AppPushService appPushService;
    @Resource
    AdmAlertDetailService admAlertDetailService;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AdmAlertInfoService admAlertInfoService;
    @Resource
    private TractionPowerChangeGraph tractionPowerChangeGraph;
    @Resource
    private TractionPowerRecoveryChangeGraph tractionPowerRecoveryChangeGraph;
    @Resource
    private TractionPowerHandler tractionPowerHandler;
    @Resource
    private InterLockChangeGraph interLockChangeGraph;
    @Resource
    private InterLockRecoveryChangeGraph interLockRecoveryChangeGraph;
    @Resource
    private SignalElectricSourceChangeGraph signalElectricSourceChangeGraph;
    @Resource
    private SignalElectricSourceRecoveryChangeGraph signalElectricSourceRecoveryChangeGraph;
    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;
    @Resource
    private VirtualGroupHandler virtualGroupHandler;

    /**
     * 判断故障开始时间是否在发车时间内，系统配置参数中取出发车时间范围
     *
     * @param startAlarmTime 故障开始时间
     */

    
    @Deprecated
    public void isNowBelongToTrainRuntime(String startAlarmTime) {
//        log.debug("进行行车运行时间判断，故障录入时间{}",startAlarmTime);
//        // 系统配置参数中取出发车时间范围
//        String startTime = SysParamKit.getByCode(IidsSysParamPool.TRAIN_DEPARTURE_TIME);
//        String endTime = SysParamKit.getByCode(IidsSysParamPool.TRAIN_IDLING_TIME);
//        if(!DateUtil.isBelongPeriodTime(startAlarmTime,startTime, endTime)){
//            throw new BizException(CodeEnum.TRAIN_NOT_IN_RUNTIME);
//        }
    }

    /**
     * 推送中心或车站请求确认或通知车站录入故障的消息
     *
     * @param alarmInfo alarmInfo
     * @return 1 中心或车站已确认,继续执行之后handler 0 中心或车站未确认，发送至中心确认，不执行之后handler
     */
    
    public Integer pushConfirmToCenter(AlarmInfo alarmInfo) {
        // 大客流故障
        if (alarmInfo.getAlarmType() == Integer.parseInt(AlarmTypeConstant.LARGE_PASSENGER)) {
            return pushLargePass(alarmInfo);
        } else {
            // 其他非中心录入故障
            if (0 != alarmInfo.getReportClientIndex() && 0 == alarmInfo.getIfConfirmed()) {
                log.info("{}推送中心应急事件录入确认", alarmInfo.getReportClientIndex());
                // 获取故障子类型的故障名，并添加到对象中
                AdmNoticeDto admNoticeDto = new AdmNoticeDto(alarmInfo,
                        admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
                // 推送中心请求确认故障的消息
                appPushService.sendWebNoticeMessageToAny(
                        new WebNoticeDto(WebNoticeCodeConst.ADM_INSERT_ALARM_CONFIRM,
                                "0", admNoticeDto));
                return 0;
                // 中心确认后，前端重新执行execute请求，走else；中心未确认，前端不请求，即不执行
            } else {
                return 1;
            }
        }
    }

    
    public Integer disablePreviewButton(AlarmInfo alarmInfo) {
        // 将预览图按钮禁用
        Integer affectedLine = admAlertDetailService.updateButtonStatus(2, alarmInfo.getTableInfoId(), "2");
        log.info("禁用预览图按钮，更新影响的数据库为【{}】行", affectedLine);
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
        return affectedLine;
    }

    
    public Integer visiablePreviewButton(AlarmInfo alarmInfo) {
        Integer affectedLine = admAlertDetailService.updateButtonStatus(0, alarmInfo.getTableInfoId(), "2");
        log.info("使预览图按钮可预览不可执行，更新影响的数据库为【{}】行", affectedLine);
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
        return affectedLine;
    }


    /**
     * 录入大客流时处理
     *
     * @param alarmInfo
     * @return 1 继续执行,0 不执行
     */
    private Integer pushLargePass(AlarmInfo alarmInfo) {
        // 录入本车站大客流,也需要给本站推送确认信息,和 下面 车站录入另一个车站大客流代码一样
        if (alarmInfo.getReportClientIndex() == alarmInfo.getStationId()) {
            // 车站未确认
            if (0 == alarmInfo.getIfConfirmed()) {
                log.info("{}车站推送确认大客流事件至发生车站{}",
                        alarmInfo.getReportClientIndex(), alarmInfo.getStationId());
                // 获取故障子类型的故障名，并添加到对象中
                AdmNoticeDto admNoticeDto = new AdmNoticeDto(alarmInfo,
                        admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
                // 推送大客流发生车站确认消息
                appPushService.sendWebNoticeMessageToAny(
                        new WebNoticeDto(WebNoticeCodeConst.ADM_INSERT_ALARM_CONFIRM,
                                Integer.toString(alarmInfo.getStationId()), admNoticeDto));
                return 0;
                // 车站确认后
            } else {
                // 推送中心通知
                // 获取故障子类型的故障名，并添加到对象中
                AdmNoticeDto admNoticeDto = new AdmNoticeDto(alarmInfo,
                        admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
                appPushService.sendWebNoticeMessageToAny(
                        new WebNoticeDto(WebNoticeCodeConst.ADM_INSERT_ALARM_NOTICE, "0", admNoticeDto));
                return 1;
            }
        }
        // 中心录入大客流
        else if (alarmInfo.getReportClientIndex() == 0) {
            // 车站未确认
            if (0 == alarmInfo.getIfConfirmed()) {
                log.info("中心推送确认大客流事件至发生车站{}", alarmInfo.getStationId());
                // 获取故障子类型的故障名，并添加到对象中
                AdmNoticeDto admNoticeDto = new AdmNoticeDto(alarmInfo,
                        admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
                // 推送大客流发生车站确认消息
                appPushService.sendWebNoticeMessageToAny(
                        new WebNoticeDto(WebNoticeCodeConst.ADM_INSERT_ALARM_CONFIRM,
                                Integer.toString(alarmInfo.getStationId()), admNoticeDto));
                return 0;
            } else {
                return 1;
            }
        }
        // 车站录入另一个车站大客流
        else if (alarmInfo.getStationId() != alarmInfo.getReportClientIndex()) {
            // 车站未确认
            if (0 == alarmInfo.getIfConfirmed()) {
                log.info("{}车站推送确认大客流事件至发生车站{}",
                        alarmInfo.getReportClientIndex(), alarmInfo.getStationId());
                // 获取故障子类型的故障名，并添加到对象中
                AdmNoticeDto admNoticeDto = new AdmNoticeDto(alarmInfo,
                        admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
                // 推送大客流发生车站确认消息
                appPushService.sendWebNoticeMessageToAny(
                        new WebNoticeDto(WebNoticeCodeConst.ADM_INSERT_ALARM_CONFIRM,
                                Integer.toString(alarmInfo.getStationId()), admNoticeDto));
                return 0;
                // 车站确认后
            } else {
                // 推送中心通知
                // 获取故障子类型的故障名，并添加到对象中
                AdmNoticeDto admNoticeDto = new AdmNoticeDto(alarmInfo,
                        admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail()));
                appPushService.sendWebNoticeMessageToAny(
                        new WebNoticeDto(WebNoticeCodeConst.ADM_INSERT_ALARM_NOTICE, "0", admNoticeDto));
                return 1;
            }
        }
        return 0;
    }

    /**
     * 只有第一次推荐指令的故障，置灰预览图按钮；其余可以预览
     *
     * @param alarmInfo
     * @return
     */
    
    public void disablePreviewButtonWhenOnlyOneAdm(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());

        Optional.ofNullable(alarmInfo1).ifPresent(t -> {

            if (  t.getAlarmType() == 1
                || t.getAlarmType() == 2
                    || t.getAlarmType() == 3
                    || t.getAlarmType() == 5
                    // 列车进/出站过程中站台门打开,只有一步推荐指令,执行完预览运行图置灰运行图预览按钮
                    || t.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_OUT_STATION)
                    || t.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_INTO_STATION)
                    || t.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH)
                    || t.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_SWITCH)
                    || t.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH_RESET)
                    || t.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_SWITCH_RESET)
                    || t.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.SINGLE_PLATFORM_DOOR_CANNOT_OPEN)
                    || t.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.SINGLE_PLATFORM_DOOR_CANNOT_CLOSE)
                    || t.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_PLATFORM_DOOR_CANNOT_OPEN)
                    || t.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.ALL_PLATFORM_DOOR_CANNOT_CLOSE)
                    || t.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.MORE_MCM_FAILURE)) {
                this.disablePreviewButton(alarmInfo);
            }
            // 道岔故障单独处理
            else if (t.getAlarmType() == 12) {
                changePreviewBtn(t);
                //计轴故障点完执行
            }
            // 车门故障、牵引故障及其它情况运行图预览按钮可以查看
            else {
                this.visiablePreviewButton(alarmInfo);
            }
        });
    }

    
    public void changePreviewBtn(AlarmInfo alarmInfo) {
        if ((AlarmTypeConstant.SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE.equals(String.valueOf(alarmInfo.getAlarmTypeDetail())) && alarmInfo.getExecuteStep() == 3)
                || alarmInfo.getExecuteStep() == 4
                || alarmInfo.getExecuteStep() == -2
                || alarmInfo.getExecuteStep() == -3) {
            admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), IidsConstPool.ALLOW_FAILOVER_2);
        }
        disablePreviewButton(alarmInfo);
        // 道岔故障每执行一预览图,就要调一次图,且每次调图的caseCode都有可能相同。暂时在Redis中做个标识,用于调图
        BasicCommonCacheUtils.set(Cache.SWITCH_FAILURE_PREVIEW_SIGN, "1");
    }

    
    public void updatePreviewBtm(AdmAlertDetail admAlertDetail) {
        admAlertDetailService.updatePreviewBtn(admAlertDetail);
    }

    
    public void changeGraphAfterCase(AlarmInfo alarmInfo) {
        AlarmInfo alarmInfo1 = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo1, "生命周期已结束，推荐指令流程结束");
//        // 调图前先删除扣车缓存
//        BasicCommonCacheUtils.delKey(Cache.HOLD_TRAIN_FLAG);
//        // 接触网失电方案选择后直接调图
//        if (AlarmTypeDetailEnum.TRACTION_POWER.getAlarmDetailType() == alarmInfo1.getAlarmTypeDetail()) {
//            // 接触网失电第二步推荐指令为非故障恢复
//            if (IidsConstPool.EXECUTE_STEP_2 == alarmInfo1.getExecuteStep()) {
//                tractionPowerChangeGraph.handle(alarmInfo1, null);
//            } else {
//                // 否则都是故障恢复调图或者最后一步调图
//                tractionPowerRecoveryChangeGraph.handle(alarmInfo1, null);
//                //调图后生命周期结束
//                alarmInfo1.setEndLife(false);
//                admAlertInfoSubService.updateById(alarmInfo1);
//            }
//        }
//        // 联锁双机方案选择后直接调图
//        if (AlarmTypeDetailEnum.INTER_LOCK.getAlarmDetailType() == alarmInfo1.getAlarmTypeDetail()) {
//            // 故障恢复调图后故障结束
//            if (IidsConstPool.EXECUTE_STEP_2 == alarmInfo1.getExecuteStep()) {
//                interLockChangeGraph.handle(alarmInfo1, null);
//            }else if(IidsConstPool.EXECUTE_STEP_0_1 == alarmInfo.getExecuteStep()){
//                //设置故障为恢复状态
//                alarmInfo.setAlarmState(2);
//                //设置故障结束时间
//                String currentDateTime = DateUtil.getDate("yyyy-MM-dd HH:mm:ss.SSS");
//                alarmInfo.setEndAlarmTime(currentDateTime);
//                aidDecisionExecService.changeGraph(alarmInfo);
//            }else{
////            if (IidsConstPool.EXECUTE_STEP_0_1 == alarmInfo1.getExecuteStep()) {
//                interLockRecoveryChangeGraph.handle(alarmInfo1, null);
//                //调图后生命周期结束
//                alarmInfo1.setEndLife(false);
//                admAlertInfoSubService.updateById(alarmInfo1);
//            }
//        }
//        // 信号电源方案选择后直接调图
//        if (AlarmTypeDetailEnum.SIGNAL_ELECTRIC.getAlarmDetailType() == alarmInfo1.getAlarmTypeDetail()) {
//            // 故障恢复调图后故障结束
//            if (IidsConstPool.EXECUTE_STEP_2 == alarmInfo1.getExecuteStep()) {
//                signalElectricSourceChangeGraph.handle(alarmInfo1, null);
//            }else if(IidsConstPool.EXECUTE_STEP_0_1 == alarmInfo.getExecuteStep()){
//                //设置故障为恢复状态
//                alarmInfo.setAlarmState(2);
//                //设置故障结束时间
//                String currentDateTime = DateUtil.getDate("yyyy-MM-dd HH:mm:ss.SSS");
//                alarmInfo.setEndAlarmTime(currentDateTime);
//                aidDecisionExecService.changeGraph(alarmInfo);
//            }else{
//                signalElectricSourceRecoveryChangeGraph.handle(alarmInfo1, null);
//                //调图后生命周期结束
//                alarmInfo1.setEndLife(false);
//                admAlertInfoSubService.updateById(alarmInfo1);
//            }
//        }
        String alarmTypeDetail = String.valueOf(alarmInfo1.getAlarmTypeDetail());
        //自动广播故障流程图刷新
        if (Objects.equals(alarmTypeDetail,AlarmTypeConstant.BROADCAST_FAILURE_CAN_MANUAL)){
            alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(7));
        }
        //人工广播故障流程图刷新
        if (Objects.equals(alarmTypeDetail,AlarmTypeConstant.BROADCAST_FAILURE_CANNOT_MANUAL)){
            alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(7));
        }
        //空调功能故障流程图刷新
        if (Objects.equals(alarmTypeDetail,AlarmTypeConstant.AIR_CONDITIONING_FAILURE)){
            alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(6,7));
        }
        //空调功能故障流程图刷新
        if (Objects.equals(alarmTypeDetail,AlarmTypeConstant.AIR_CONDITIONING_VENTILATE_FAILURE)){
            alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(6,7));
        }
        //牵引功能障流程图刷新
        if (Objects.equals(alarmTypeDetail,AlarmTypeConstant.MORE_MCM_FAILURE)){
            if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(),7)){
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(13));
            }else if (alarmFlowchartService.nodeAlreadyRefreshed(alarmInfo.getTableInfoId(),8)){
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(20));
            }
        }

        if (alarmTypeDetail.equals(AlarmTypeConstant.AXLE_COUNTER_NOT_SWITCH)) {
            if (IidsConstPool.EXECUTE_STEP_2 == alarmInfo1.getExecuteStep()) {
                //interLockChangeGraph.handle(alarmInfo1, null);
            }
        }
    }
    
    public void popRouter(String choice,Integer code) {
        //获取存活的alarmInfo
        AlarmInfo alarmInfo = admAlertInfoSubService.getInfoInLife();
        if (alarmInfo == null) {
            throw new BizException("未获取到处于生命周期的alarmInfo");
        }
        // 立即推送, 发生前端不能主动推送的问题,故全列改成这里存缓存,通过定时器触发第二次
        String key = String.format(Cache.POP_CONFIRM, alarmInfo.getTableInfoId());
        BasicCommonCacheUtils.set(key, choice, 240L, TimeUnit.SECONDS);
        String alarmTypeDetail = String.valueOf(alarmInfo.getAlarmTypeDetail());
        if(alarmTypeDetail.equals(AlarmTypeConstant.TRACTION_POWER)){
            tractionPowerHandler.judgeStep(choice,code);
        } else if (alarmTypeDetail.equals(AlarmTypeConstant.VIRTUAL_GROUP)) {
            virtualGroupHandler.judgeStep(choice);
        }else {
            throw new BizException("未知故障类型");
        }
    }
}