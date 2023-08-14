package com.tct.itd.client;

import com.tct.itd.constant.SysServiceName;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @classname: CacheClient
 * @description: 缓存client
 * @author: liyunlong
 * @date: 2023/1/10 16:46
 */
@FeignClient(name = SysServiceName.ITD_GATEWAY, url = "localhost:80", path = SysServiceName.ITD_GATEWAY + "/operateCache")
public interface CacheClient {

    @PostMapping("/clearOfflineClientCache")
    void clearOfflineClientCache();
}
