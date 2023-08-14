package com.tct.itd.client;

import com.tct.itd.constant.SysServiceName;
import com.tct.itd.dto.GraphDto;
import com.tct.itd.dto.TrainGraphParamDto;
import com.tct.itd.restful.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @classname: GraphClient
 * @description: 运行图Client
 * @author: liyunlong
 * @date: 2022/5/23 10:53
 */
@FeignClient(name = SysServiceName.ITD_GATEWAY, url = "localhost:80",path =SysServiceName.ITD_GATEWAY+"/graph")
public interface GraphClient {

    @PostMapping("/getTrainGraph")
    BaseResponse<TrainGraphParamDto> findPlanRunGraph(@RequestParam("type") Integer type);

    @PostMapping("/update")
    void updatePlanRunGraph(@RequestBody GraphDto graphDto);

    @PostMapping("/version")
    BaseResponse<TrainGraphParamDto> getVersion();
}

