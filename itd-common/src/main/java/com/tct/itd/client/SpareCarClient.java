package com.tct.itd.client;

import com.tct.itd.constant.SysServiceName;
import com.tct.itd.dto.DepotInfoDto;
import com.tct.itd.restful.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @classname: SpareCarClient
 * @description: 获取备车Client
 * @author: liyunlong
 * @date: 2022/5/24 9:27
 */
@FeignClient(name = SysServiceName.ITD_GATEWAY, url = "localhost:80",path = SysServiceName.ITD_GATEWAY+"/car")
public interface SpareCarClient {

    @PostMapping("/spareCar")
    BaseResponse<DepotInfoDto> getSpareCar();
}
