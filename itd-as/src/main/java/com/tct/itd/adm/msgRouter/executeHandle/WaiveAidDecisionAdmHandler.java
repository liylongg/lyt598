package com.tct.itd.adm.msgRouter.executeHandle;

import com.tct.itd.adm.entity.AdmAlertInfo;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.*;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.client.CacheClient;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * @Description 放弃推荐指令
 * @Author zyl
 * @Date 2021/5/17 22:20
 **/

@Slf4j
@Component
public class WaiveAidDecisionAdmHandler implements AuxiliaryDecisionHandler {

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AdmCommonMethodService admCommonMethodService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AdmAlertInfoService admAlertInfoService;
    @Resource
    private AxleCounterService axleCounterService;
    @Resource
    private SwitchFailureService switchFailureService;
    @Resource
    private InterLockAndTractionPowerService lockAndTractionPowerService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;
    @Resource
    private CacheClient cacheClient;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    //故障放弃路由常量0
    private static final String ZERO = "0";

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        long tableInfoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(tableInfoId);
        //终止流程图执行状态
        alarmFlowchartService.giveUp(alarmInfo.getTableInfoId());
        //计轴故障保存放弃状态缓存
        axleCounterService.waiveAidDecision(alarmInfo);
        // 道岔故障放弃推荐指令
        switchFailureService.waiveAidDecision(alarmInfo);
        //联锁和接触网失电放弃处理
        lockAndTractionPowerService.waiveAidDecision(alarmInfo);
        if(alarmInfo.getExecuteStep() == 1){
            //第一次推荐指令
            log.info("放弃第一次推荐指令");
            //车门放弃故障恢复，不修改第一次推荐指令为已放弃
            if(!(alarmInfo.getAlarmType() == Integer.parseInt(AlarmTypeConstant.TRAIN_DOOR_FAILURE)
                    && auxiliaryDecision.getExecuteStep() == IidsConstPool.EXECUTE_STEP_0_99)){
                alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(),alarmInfo.getTableBoxId(),0);
            }
        }else{
            //放弃第二次推荐指令时，置灰预览图按钮
            admCommonMethodService.disablePreviewButton(alarmInfo);
            alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(),alarmInfo.getTableBoxId2(),0);
            log.info("第二次放弃推荐指令");
            //modified by kangyi 放弃操作，不做处理
            //aidDecisionExecService.offHoldTrain();
        }
        if (1 == admAlertInfoService.selectById(alarmInfo.getTableInfoId()).getAllowFailover()) {
            //状态设置为禁用
            admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), 2);
        }
        //更新子表状态为已执行
        alarmInfo.setExecuteEnd(2);
        //推荐指令执行完毕，将该故障状态更新为已结束
        alarmInfo.setEndLife(false);
        admAlertInfoSubService.updateById(alarmInfo);
        //关闭车站弹窗
        this.closeStationWin(tableInfoId);
        BasicCommonCacheUtils.delKey(Cache.HOLD_TRAIN_FLAG);
        cacheClient.clearOfflineClientCache();
        //刷新前台页面
        appPushService.sendWebNoticeMessageToAny(
                new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
    }

    /**
     * @Author yuelei
     * @Desc 关闭车站弹窗
     * @Date 11:18 2023/3/8
     */
    public void closeStationWin(long infoId){
        //故障放弃，车站存在弹窗，关闭车站弹窗
        if(BasicCommonCacheUtils.exist(Cache.STATION_CONFIRM_IDS)){
            //获取故障类型
            AdmAlertInfo admAlertInfo = admAlertInfoService.selectById(infoId);
            String stations = (String)BasicCommonCacheUtils.get(Cache.STATION_CONFIRM_IDS);
            String[] stationArr = stations.split(",");
            for (String stationId : stationArr) {
                // 故障放弃,关闭车站的确认弹窗
                appPushService.sendWebNoticeMessageToAny(new WebNoticeDto(MsgPushEnum.CLOSE_STATION_CONFIRM_NOTICE_MSG.getCode(),
                        stationId, "关闭车站的确认弹窗"));
                //故障放弃关闭车站弹窗
                appPushService.sendMessage(MsgTypeEnum.STATION_RECOVER_ALERT_WID, new WebNoticeDto(CommandEnum.STATION_RECOVER_ALERT_WID.getMsgCode(), stationId, admAlertInfo.getType() + "流程已放弃"));
            }
            BasicCommonCacheUtils.delKey(Cache.STATION_CONFIRM_IDS);
        }
    }

    @Override
    public String channel() {
        return ZERO;
    }
}
