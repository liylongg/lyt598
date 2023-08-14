package com.tct.itd.client;

import com.tct.itd.constant.SysServiceName;
import com.tct.itd.dto.VersionDto;
import com.tct.itd.restful.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = SysServiceName.ITD_AS, url = "localhost:80", path = SysServiceName.ITD_AS)
public interface VersionClient {

    @GetMapping("/versions")
    BaseResponse<VersionDto> getVersion();
}
