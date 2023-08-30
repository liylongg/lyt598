package com.tct.itd.adm.api;


import com.tct.itd.common.dto.*;
import com.tct.itd.common.interceptor.AlgParLogInterceptor;
import com.tct.itd.constant.SysServiceName;
import com.tct.itd.dto.ReverseInfoDto;
import com.tct.itd.restful.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Description : 算法服务api接口
 * @Author : zhangjiarui
 * @Date : Created in 2021/8/19
 */
@FeignClient(name = SysServiceName.ITD_ALG, configuration = AlgParLogInterceptor.class, url = "localhost:80", path = SysServiceName.ITD_ALG)
public interface AlgorithmClient {
    /**
     * 获取受影响车次（扣车站台、站台抬车）
     *
     * @param algorithmData 算法参数
     * @return 受影响车次
     */
    @PostMapping("/api/solver/train/getFollowingTrains")
    BaseResponse<FolloweTrainsDto> getFollowingTrains(@RequestBody AlgorithmData algorithmData);

    /**
     * 获取调整后运行图和调整车次列表
     *
     * @param algorithmData 算法参数
     * @return 调整后运行图和调整车次列表
     */
    @PostMapping("/api/Solver")
    BaseResponse<AlgorithmResult> apiSolver(@RequestBody AlgorithmData algorithmData);

    /**
     * 获取运行图调整方案列表
     *
     * @param algorithmData 算法参数
     * @return 运行图调整方案列表
     */
    @PostMapping("/api/solver/adjustStrategy")
    BaseResponse<List<AlgStrategyResult>> adjustStrategyUrl(@RequestBody AlgorithmData algorithmData);

    /**
     * 获取大客流调整后运行图和调整车次列表
     *
     * @param largePassFlowAlgorithmParam 大客流算法参数
     * @return 大客流调整后运行图和调整车次列表
     */
    @PostMapping("/api/solver/largePassFlow")
    BaseResponse<AlgorithmResult> largePassFlowGraphUrl(@RequestBody LargePassFlowAlgorithmParam largePassFlowAlgorithmParam);

    /**
     * 获取大客流运行图调整方案列表
     *
     * @param largePassFlowAlgorithmParam 大客流算法参数
     * @return 大客流运行图调整方案列表
     */
    @PostMapping("/api/solver/largePassFlow/adjustStrategy")
    BaseResponse<List<AlgStrategyResult>> largePassFlowStrategyUrl(@RequestBody LargePassFlowAlgorithmParam largePassFlowAlgorithmParam);

    /**
     * 算法自检数据是否满足调图条件(防止出现算法无法频繁调图等异常)
     * code = 501时, 异常,目前是全列
     *
     * @param algorithmData algorithmData
     * @return null
     */
    @PostMapping("/api/solver/Check")
    BaseResponse<String> check(@RequestBody AlgorithmData algorithmData);

    /**
     * 大客流算法自检数据是否满足调图条件(防止出现算法无法频繁调图等异常)
     * code = 501时, 异常,目前断面大客流
     *
     * @param largePassFlowAlgorithmParam algorithmData
     * @return null
     */
    @PostMapping("/api/solver/largePassFlow/check")
    BaseResponse<String> largePassFlowCheck(@RequestBody LargePassFlowAlgorithmParam largePassFlowAlgorithmParam);

    @GetMapping("/api/Solver/test")
    String apiSolverTest();

    /**
     * @param
     * @description 获取版本号
     * @date 2022/6/19 17:09
     * @author kangyi
     * @return: java.lang.String
     */
    @PostMapping("/api/Solver/getVersionId")
    BaseResponse<String> getVersionId();

    /**
     * @param
     * @description 获取版本号
     * @date 2022/6/19 17:09
     * @author kangyi
     * @return: java.lang.String
     */
    @PostMapping("/api/solver/GetFirstTrain")
    BaseResponse<String> getFirstTrain(TrainPlaceAlgorithmData algorithmData);

    /**
     *  获取算法返回列车在折返区间或折返轨处的信息
     * @param algorithmData 算法接口参数
     * @return 算法返回列车在折返区间或折返轨处的信息
     */
    @PostMapping("api/solver/GetReversalStationPlatform")
    BaseResponse<ReverseInfoDto> getReversalStationPlatform(@RequestBody AlgorithmData algorithmData);
}
