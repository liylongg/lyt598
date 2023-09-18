package com.tct.itd.adm.runGraph.stategy;

import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.common.dto.PrePlanRunGraph;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description 站台门故障-列车进站过程中站台门打开-运行图预览
 * @Author yuelei
 * @Date 2021/9/1 14:27
 */
@Slf4j
@Service
public class AllPlatformDoorOpenIntoStationStrategy extends AbstractPrePlanRunGraphStrategy implements IPrePlanRunGraphStrategy {

    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        return AlarmStateEnum.LATE_ADJUSTMENT.getCode();
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        //获取调图冗余参数,时间毫秒
        long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
        return DateUtil.getTimeStamp(System.currentTimeMillis() + setRedundancyTime);
    }

    @Override
    public List<AlgStrategyResult> listAdjustStrategy(AlarmInfo alarmInfo) {
        log.info("获取方案列表时，传入故障信息：{}", JsonUtils.toJSONString(alarmInfo));
        Assert.notNull(alarmInfo, "客户端未上报故障");
        //获取运行图策略，用于预览运行图
        alarmInfo.setEndAlarmTime(this.getEndAlarmTime(alarmInfo));
        alarmInfo.setAlarmState(getAlarmState(alarmInfo));
        List<AlgStrategyResult> algStrategyResultList;
        try {
            algStrategyResultList = aidDecisionExecService.adjustRunGraphAlg(alarmInfo);
        } catch (Exception e) {
            log.error("获取运行图预览方案列表失败,失败信息:【{}】", e.getMessage());
            ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        Assert.notEmpty(algStrategyResultList, "未获取到调整方案");
        //更新结束时间到数据库
        admAlertInfoSubService.updateEndTime(alarmInfo);
        return algStrategyResultList;
    }

    @Override
    public PrePlanRunGraph previewRunGraph(AlarmInfo alarmInfo) {
        //获取告警信息
        alarmInfo = admAlertInfoSubService.queryByInfoId(alarmInfo.getTableInfoId());
        Assert.notNull(alarmInfo, "当前故障生命周期已结束");
        alarmInfo.setAlarmState(getAlarmState(alarmInfo));
        return aidDecisionExecService.getPlanRunGraph(alarmInfo);
    }

    @Override
    public String strategy() {
        return AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_INTO_STATION;
    }
}
