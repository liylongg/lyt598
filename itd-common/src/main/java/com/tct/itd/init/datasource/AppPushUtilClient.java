package com.tct.itd.init.datasource;

import com.tct.itd.constant.SysServiceName;
import com.tct.itd.model.PushMessageRequest;
import com.tct.itd.restful.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @description: apppush接口
 * @author: kangyi
 * @date:2021/11/26
 **/
@FeignClient(name = SysServiceName.ITD_GATEWAY, url = "localhost:80", path = SysServiceName.ITD_GATEWAY)
public interface AppPushUtilClient {

    /**
     * @param pushMessageRequest
     * @description: 调用推送服务向客户端推送数据
     * @author: kangyi
     * @date:2021/11/26
     * @return:com.tct.iids.restful.BaseResponse<java.lang.String>
     **/
    @PostMapping(value = "push/pushObject", produces = "application/json;charset=utf-8")
    BaseResponse<String> pushData(@RequestBody PushMessageRequest pushMessageRequest);
}
