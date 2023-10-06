package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.msgRouter.executeHandle.common.step.CircleHoldTrainHandler;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.service.AdmAlertInfoUpgradeService;
import com.tct.itd.adm.service.HoldTrainService;
import com.tct.itd.basedata.dfsread.service.handle.PlatformInfoService;
import com.tct.itd.client.AlgSwitchClient;
import com.tct.itd.common.dto.AlgorithmData;
import com.tct.itd.common.dto.DetainTrainStopArea;
import com.tct.itd.common.enums.HoldTrainTypeEnum;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.PlanRunGraphEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.hub.service.PlanRunGraphService;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @classname: HoldTrainMorePlatFormHandler
 * @description: 分组扣车
 * @author: yuelei
 * @date: 2022/10/12 17:51
 */
@Service("holdTrainMorePlatForm")
@Slf4j
public class HoldTrainMorePlatFormHandler implements AidDecSubStepHandler {

    @Resource
    private CircleHoldTrainHandler circleHoldTrain;
    @Resource
    private HoldTrainService holdTrainService;
    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AdmAlertInfoUpgradeService admAlertInfoUpgradeService;
    @Resource
    private PlanRunGraphService planRunGraphService;
    @Resource
    private AlgSwitchClient algSwitchClient;
    @Resource
    private PlatformInfoService platformInfoService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        log.info("执行扣车推荐指令,alarmInfo:{},AidDesSubStepOutDto:{}",
                JsonUtils.toJSONString(alarmInfo), JsonUtils.toJSONString(dto));
        String holdParam = dto.getParam();
        if (StringUtils.isEmpty(holdParam)) {
            throw new BizException("扣车参数为空");
        }
        String zipPlanRunGraph = (String) planRunGraphService.findPlanRunGraph(PlanRunGraphEnum.ZIP);
        AlgorithmData algorithmData = new AlgorithmData(zipPlanRunGraph, alarmInfo);
        algorithmData.setNowTime(DateUtil.getTimeStamp());
        BaseResponse<DetainTrainStopArea> detainTrainStopArea = algSwitchClient.getDetainTrainStopArea(algorithmData);
        if (detainTrainStopArea.getCode() != 200) {
            throw new BizException("调用算法获取扣车停车区域出错:{}", detainTrainStopArea.getMessage());
        }
        List<Integer> stopAreaIdList = detainTrainStopArea.getData().getStopAreaId();
        if (CollectionUtils.isEmpty(stopAreaIdList)) {
            log.debug("调用算法获取扣车停车区域为空");
            return;
        }
        log.info("算法返回开始扣车的停车区域id:【{}】", JsonUtils.toJSONString(stopAreaIdList));
        //故障站根据数据库配置获取
        circleHoldTrain.handle(alarmInfo, dto);
        //算法返回扣车站台，默认扣本站
        stopAreaIdList.forEach(s -> {
            List<Integer> list = new ArrayList<>();
            list.add(platformInfoService.getPlatformIdByStopArea(s));
            holdTrainService.holdTrain(list, HoldTrainTypeEnum.CURRENT_PLATFORM.getType());
        });

    }
}
