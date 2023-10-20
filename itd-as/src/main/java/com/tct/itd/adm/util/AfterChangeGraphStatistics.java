package com.tct.itd.adm.util;

import com.tct.itd.adm.api.AlgPowerClient;
import com.tct.itd.adm.convert.StatisticsAlgorithmDataConvert;
import com.tct.itd.adm.task.TrainAdjustCountTask;
import com.tct.itd.common.dto.StatisticsAlgorithmData;
import com.tct.itd.common.dto.StatisticsAlgorithmDataDto;
import com.tct.itd.common.dto.TrainNumberAdjustSubDto;
import com.tct.itd.exception.BizException;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author yuelei
 * @Desc 调图前后调整信息统计
 * @Date 15:50 2022/8/26
 */
@Component
@Slf4j
public class AfterChangeGraphStatistics {
    @Resource
    private AlgPowerClient algPowerClient;
    @Resource
    private TrainAdjustCountTask trainAdjustCountTask;
    @Resource
    private StatisticsAlgorithmDataConvert statisticsAlgorithmDataConvert;

    @Async("taskExecutor")
    public void statistics(StatisticsAlgorithmDataDto dto){
        log.info("记录统计指标");
        StatisticsAlgorithmData statisticsAlgorithmData = statisticsAlgorithmDataConvert.dtoToVo(dto);
        BaseResponse<List<TrainNumberAdjustSubDto>> result = algPowerClient.getFinalIndicatorStatistics(statisticsAlgorithmData);
        log.info("指标返回结果：{}", JsonUtils.toJSONString(result));
        if(!result.getSuccess()){
            throw new BizException("指标统计出错");
        }
        List<TrainNumberAdjustSubDto> data = result.getData();
        if (data.isEmpty()) {
            log.error("未获取调整信息");
            return;
        }
        trainAdjustCountTask.adjustIndexCount(data, dto.getTableInfoId(),1);
    }
}
