package com.tct.itd.adm.msgRouter.check.handle;

import com.tct.itd.adm.api.AlgorithmClient;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.check.router.CheckConfigHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.runGraph.stategy.IPrePlanRunGraphStrategy;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.AlgorithmData;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.restful.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/***
 * @Description 故障录入校验-故障结束时间早于计划到达时间
 * @Author zhoukun
 * @Date 2022/4/21
 */
@Slf4j
@Service("checkEndTimeEarlierPlanTime")
public class CheckEndTimeEarlierPlanTimeHandler implements CheckConfigHandler {
    private final Map<String, IPrePlanRunGraphStrategy> graphDataStrategyHashMap = new HashMap<>();
    @Resource
    private List<IPrePlanRunGraphStrategy> graphDataStrategyList;

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;
    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AlgorithmClient algorithmClient;

    /**
     * @Description 建立映射关系
     * @Author zhangyinglong
     * @Date 2021/6/2 14:24
     */
    @PostConstruct
    public void init() {
        graphDataStrategyList.forEach(
                graphDataStrategy -> {
                    graphDataStrategyHashMap.put(graphDataStrategy.strategy(), graphDataStrategy);
                    if (Objects.equals(graphDataStrategy.strategy(), AlarmTypeConstant.AIR_CONDITIONING_VENTILATE_FAILURE)) {
                        graphDataStrategyHashMap.put(AlarmTypeConstant.AIR_CONDITIONING_VENTILATE_FAILURE, graphDataStrategy);
                    }
                    if (Objects.equals(graphDataStrategy.strategy(), AlarmTypeConstant.BROADCAST_FAILURE_CANNOT_MANUAL)) {
                        graphDataStrategyHashMap.put(AlarmTypeConstant.BROADCAST_FAILURE_CANNOT_MANUAL, graphDataStrategy);
                    }
                }
        );
    }


    @Override
    public void handle(AlarmInfo alarmInfo) {
        if (StringUtils.isEmpty(alarmInfo.getTrainId()) || IidsConstPool.TRAIN_ID_0_1.equals(alarmInfo.getTrainId())) {
            return;
        }
        //录入的区间，不做校验
        if (alarmInfo.getSectionFlag()==1) {
            return;
        }
        String alarmTypeDetail = String.valueOf(alarmInfo.getAlarmTypeDetail());
        IPrePlanRunGraphStrategy graphDataStrategy = graphDataStrategyHashMap.get(alarmTypeDetail);
        alarmInfo.setEndAlarmTime(graphDataStrategy.getEndAlarmTime(alarmInfo));
        alarmInfo.setAlarmState(graphDataStrategy.getAlarmState(alarmInfo));
        AlgorithmData algorithmData = aidDecisionExecService.getAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "调整运行图时,未获取到获取算法参数");
        //算法自检
        BaseResponse<String> baseResponse = algorithmClient.check(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                ExecuteAidDecUtil.giveUp(alarmInfo.getTableInfoId());
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
    }
}
