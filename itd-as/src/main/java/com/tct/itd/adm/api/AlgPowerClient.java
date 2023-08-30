package com.tct.itd.adm.api;


import com.tct.itd.common.dto.*;
import com.tct.itd.common.interceptor.AlgParLogInterceptor;
import com.tct.itd.constant.SysServiceName;
import com.tct.itd.restful.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Description : 接触网失电算法服务api接口
 * @Author : zhangjiarui
 * @Date : Created in 2022-3-8 10:06:04
 */
@FeignClient(name = SysServiceName.ITD_ALG, configuration = AlgParLogInterceptor.class,url = "localhost:80", path = SysServiceName.ITD_ALG)
public interface AlgPowerClient {

    /**
     * 接触网失电的算法获取受影响车次（扣车站台、站台抬车）
     * @param algorithmData 算法参数
     * @return 扣抬车
     */
    @PostMapping("/api/Solver/switchFailure/getFollowingTrains")
    BaseResponse<FolloweTrainsDto> getFollowingTrainsForPower(@RequestBody AlgorithmData algorithmData);

    /**
     * 接触网失电算法自检
     * @param algorithmData 算法参数
     * @return 算法自检结果
     */
    @PostMapping("/api/Solver/switchFailure/check")
    BaseResponse<String> tractionPowerCheck(@RequestBody AlgorithmData algorithmData);

    /**
     * 接触网失电调图调图
     * @param algorithmData 算法参数
     * @return 结果
     */
    @PostMapping("/api/Solver/switchFailure")
    BaseResponse<AlgorithmResult> tractionPowerSolver(@RequestBody AlgorithmData algorithmData);

    /**
     * 接触网失电获取运行图方案类别表算法接口
     * @param algorithmData 算法参数
     * @return 结果
     */
    @PostMapping("/api/Solver/switchFailure/adjustStrategy")
    BaseResponse<List<AlgStrategyResult>> tractionPowerAdjustStrategy(@RequestBody AlgorithmData algorithmData);


    /**
     * @Author yuelei
     * @Desc 获取折返扣车站台
     * @Date 17:50 2022/8/10
     */
    @PostMapping("/api/Solver/switchFailure/getRetraceStopArea")
    BaseResponse<List<Integer>> getRetraceStopArea(@RequestBody AlgorithmData algorithmData);

    /**
     * @Author yuelei
     * @Desc  获取运行图调整前后对比统计
     * @Date 15:08 2022/8/26
     */
    @PostMapping("/api/Solver/switchFailure/getFinalIndicatorStatistics")
    BaseResponse<List<TrainNumberAdjustSubDto>> getFinalIndicatorStatistics(@RequestBody StatisticsAlgorithmData algorithmData);

}

