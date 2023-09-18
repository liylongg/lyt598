package com.tct.itd.adm.runGraph.stategy;


import com.tct.itd.adm.iconstant.AlarmStateEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;

/**
 * @classname: ApdcncStrategy
 * @description: 站台门故障-整侧车门无法关闭(Apdcnc:All platform door cannot close)
 * @author: liyunlong
 * @date: 2021/10/21 17:18
 */
@Service
@Slf4j
public class ApdcncStrategy extends AbstractPrePlanRunGraphStrategy implements IPrePlanRunGraphStrategy {

    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;

    @Override
    public int getAlarmState(AlarmInfo alarmInfo) {
        return AlarmStateEnum.LATE_ADJUSTMENT.getCode();
    }

    @Override
    public String getEndAlarmTime(AlarmInfo alarmInfo) {
        //获取调图冗余参数,时间毫秒
        long setRedundancyTime = Long.parseLong(SysParamKit.getByCode(IidsSysParamPool.SET_REDUNDANCY_TIME));
        String endTime = DateUtil.getTimeStamp(System.currentTimeMillis() + setRedundancyTime);
        if (BasicCommonCacheUtils.exist(Cache.CHANGE_GRAPH_TIME_DIFFERENCE)) {
            endTime = (String) BasicCommonCacheUtils.get(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
        } else {
            BasicCommonCacheUtils.set(Cache.CHANGE_GRAPH_TIME_DIFFERENCE, endTime);
        }
        return endTime;
    }

    @Override
    public List<AlgStrategyResult> listAdjustStrategy(AlarmInfo alarmInfo) {
        log.info("获取方案列表时，传入故障信息：{}", JsonUtils.toJSONString(alarmInfo));
        Assert.notNull(alarmInfo, "客户端未上报故障");
        //获取运行图策略，用于预览运行图
        alarmInfo.setEndAlarmTime(getEndAlarmTime(alarmInfo));
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
        //插入运行图预览信息
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(), "系统产生运行图方案", "2", "运行图方案选择");
        return algStrategyResultList;
    }

    @Override
    public String strategy() {
        return AlarmTypeConstant.ALL_PLATFORM_DOOR_CANNOT_CLOSE;
    }
}
