package com.tct.itd.client;

import com.tct.itd.constant.SysServiceName;
import com.tct.itd.dto.VersionDto;
import com.tct.itd.excel.AlarmInfoExcel;
import com.tct.itd.restful.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = SysServiceName.ITD_AS, url = "localhost:80", path = SysServiceName.ITD_AS)
public interface ExportClient {

    /**
     * @description 导出应急事件
     * @date 2022/9/14 17:22
     * @author kangyi
     * @param params
     * @return: com.tct.itd.restful.BaseResponse<java.util.List<com.tct.itd.excel.AlarmInfoExcel>>
     */
    @GetMapping("/alert_info/export")
    BaseResponse<List<AlarmInfoExcel>> exportAlertInfo(@RequestParam("params") Map<String, Object> params);

}
