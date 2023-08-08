package com.tct.itd.common.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author kangyi
 * @description 异步发送http请求
 * @date 2022年 03月04日 14:15:23
 */
@Component
@Slf4j
public class AsyncSendHttp {

    @Resource
    private RestTemplate restTemplate;

    @Async("taskExecutor")
    public void asyncSendHttp(ConcurrentHashMap<String,ResponseEntity> responseConcurrentHashMap, Map<String, String> countRespMap, String newUrl, Object body, HttpHeaders httpHeaders, String method, String uuid) {
        String newUuid = UUID.randomUUID().toString();
        try {
            log.debug("开始发送http请求，url:{},uuId:{}", newUrl, uuid);
            RequestEntity requestEntity = new RequestEntity(body, httpHeaders, HttpMethod.resolve(method), URI.create(newUrl));
            responseConcurrentHashMap.put(newUuid,restTemplate.exchange(requestEntity, String.class));
            countRespMap.put(newUuid,newUuid);
        } catch (Exception e) {
            countRespMap.put(newUuid,newUuid);
            log.error("使用restTemplate发送请求:{}失败！", newUrl);
        }
    }
}
