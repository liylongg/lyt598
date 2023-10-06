package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.runGraph.stategy.IPrePlanRunGraphStrategy;
import com.tct.itd.basedata.dfsread.service.handle.*;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description 执行推荐指令-调整运行图
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("exeAidDisChangeGra")
@Slf4j
public class ExeAidDisChangeGraHandler implements AidDecSubStepHandler {

    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private List<IPrePlanRunGraphStrategy> graphDataStrategyList;
    @Resource
    private StopRegionDataService stopRegionDataService;

    private final Map<String, IPrePlanRunGraphStrategy> graphDataStrategyHashMap = new HashMap<>();

    /***
     * @Description 建立映射关系
     * @Author yuelei
     * @date 2021/9/30 15:26
     */
    @PostConstruct
    public void init() {
        graphDataStrategyList.forEach(
                graphDataStrategy -> graphDataStrategyHashMap.put(graphDataStrategy.strategy(), graphDataStrategy)
        );
    }

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        String strategy = String.valueOf(alarmInfo.getAlarmTypeDetail());
        IPrePlanRunGraphStrategy graphDataStrategy = graphDataStrategyHashMap.get(strategy);
        if (graphDataStrategy == null) {
            log.error("未找到故障类型对应的运行图预览策略类");
            throw new BizException(CodeEnum.ALARM_TYPE_UNHANDLED);
        }
        alarmInfo.setEndAlarmTime(graphDataStrategy.getEndAlarmTime(alarmInfo));
        alarmInfo.setAlarmState(graphDataStrategy.getAlarmState(alarmInfo));
        String platformId = alarmInfo.getPlatformId();
        alarmInfo.setStopAreaNumber(stopRegionDataService.getStopAreaByPlatformId(Integer.valueOf(platformId)));
        log.info("准备调整运行图");
        boolean isSuccess = aidDecisionExecService.changeGraph(alarmInfo);
        if(isSuccess){
            log.info("调整运行图成功");
        }else{
            log.error("调整运行图失败");
        }
    }
}
