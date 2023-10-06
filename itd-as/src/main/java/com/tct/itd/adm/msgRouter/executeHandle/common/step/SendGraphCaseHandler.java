package com.tct.itd.adm.msgRouter.executeHandle.common.step;


import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.runGraph.PrePlanRunGraphContext;
import com.tct.itd.adm.task.SavePreviewRunGraph;
import com.tct.itd.adm.util.GraphDataUtil;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.dto.AdmRunGraphCases;
import com.tct.itd.common.dto.AlgStrategyResult;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.util.TitleUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 推送运行调整方案列表
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("sendGraphCase")
@Slf4j
public class SendGraphCaseHandler implements AidDecSubStepHandler {

    @Resource
    private AppPushService appPushService;
    @Resource
    private PrePlanRunGraphContext prePlanRunGraphContext;
    @Resource
    private SavePreviewRunGraph savePreviewRunGraph;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        AdmRunGraphCases admRunGraphCases = prePlanRunGraphContext.listPreviewRunGraph(alarmInfo);
        admRunGraphCases.setExecuteStep(alarmInfo.getExecuteStep());
        List<AlgStrategyResult> resultList = admRunGraphCases.getAdmRunGraphCases();
        List<AlgStrategyResult> resultList1 = new ArrayList<>();
        resultList.forEach(result -> {
            AlgStrategyResult algStrategyResult = new AlgStrategyResult();
            BeanUtils.copyProperties(result, algStrategyResult);
            resultList1.add(algStrategyResult);
            result.setAdjustStatisticalDtoList(GraphDataUtil.generateAdjustStatisticalResult(result.getTrainNumberAdjustDtoList()));
            result.setTrainNumberAdjustDtoList(null);
            result.setBackTrainNumber(null);
            result.setRecoveryTrainDtoList(null);
            result.setCompressTrainGraphString(null);
            result.setBeforeTrainGraphString(null);
            result.setSlowlyTrainDtoList(null);
            result.setRecoverySlowlyTrainDtoList(null);
        });
        admRunGraphCases.setTitle(TitleUtil.getTitle(alarmInfo));
        // 推送运行图调整方案列表
        appPushService.sendWebNoticeToAny(new WebNoticeDto(WebNoticeCodeConst.GRAPH_CASE,
                "0", admRunGraphCases), MsgTypeEnum.WEB_NOTICE_GRAPH_CASE.getMsgType());
        //保存预览方案历史
        savePreviewRunGraph.save(alarmInfo, resultList1);
    }


}
