package com.tct.itd.adm.msgRouter.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.InterLockAlarmInfo;
import com.tct.itd.common.dto.TractionPowerInfo;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @Description 联锁、接触网可能用到的方法
 * @Author zhoukun
 * @Date 2022/8/17
 */
@Slf4j
@Service
public class InterLockAndTractionPowerService {

    @Resource(name = "interLockStatusCache")
    private com.github.benmanes.caffeine.cache.Cache<String, InterLockAlarmInfo> interLockStatusCache;
    @Resource(name = "singalElectricStatus")
    private com.github.benmanes.caffeine.cache.Cache<String, InterLockAlarmInfo> singalElectricStatus;
    @Resource(name = "tractionPowerInfoCache")
    private Cache<String, TractionPowerInfo> tractionPowerInfoCache;
    @Resource
    private AdmAlertInfoService admAlertInfoService;

    public void waiveAidDecision(AlarmInfo alarmInfo) {
        if (Objects.isNull(alarmInfo)) {
            return;
        }

        //删除已经上报过的故障缓存
        InterLockAlarmInfo interLockAlarmInfo=new InterLockAlarmInfo();
        //接触网失电
        if (alarmInfo.getAlarmType() == Integer.valueOf(AlarmTypeConstant.TRACTION_POWER)){
            log.info(alarmInfo.getAlarmType()+"故障，放弃推荐指令处理");
            //放弃自动上报推荐指令时，存入当前失电区间的编号
            BasicCommonCacheUtils.hPut(com.tct.itd.common.cache.Cache.ALREADY_REPORT_TRACTION_POWER,alarmInfo.getTractionSectionId().toString(),1);
            //将故障恢复的状态为1的置为2，按钮置灰
            admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), IidsConstPool.ALLOW_FAILOVER_2);
        }
        //联锁故障
        if (alarmInfo.getAlarmType() == Integer.valueOf(AlarmTypeConstant.INTERLOCKING_DOUBLE)){
            log.info(alarmInfo.getAlarmType()+"故障，放弃推荐指令处理");
            //放弃自动上报推荐指令时，存入当前联锁ID
            BasicCommonCacheUtils.set(com.tct.itd.common.cache.Cache.ALREADY_REPORT_INNER_LOCK,1);
            // 禁用故障续报按钮
            admAlertInfoService.updateReportById(alarmInfo.getTableInfoId(), 2);
        }
        //信号电源故障
        if (alarmInfo.getAlarmType() == Integer.valueOf(AlarmTypeConstant.SIGNAL_ELECTRIC)){
            log.info(alarmInfo.getAlarmType()+"故障，放弃推荐指令处理");
            // 禁用故障续报按钮
            admAlertInfoService.updateReportById(alarmInfo.getTableInfoId(), 2);
            //将故障恢复的状态为1的置为2，按钮置灰
            admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), IidsConstPool.ALLOW_FAILOVER_2);
        }

    }
}
