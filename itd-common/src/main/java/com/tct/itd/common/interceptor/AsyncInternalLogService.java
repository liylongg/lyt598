package com.tct.itd.common.interceptor;


import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okio.Buffer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @Description 异步处理内部接口请求日志
 * @Author zjr
 * @Date 2022/2/25 21:09
 */
@Slf4j
@Component
public class AsyncInternalLogService {

    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    @Async("taskExecutor")
    public void log(Request request, String s) throws IOException {
        try {
            // 获取请求中的请求体
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            String bodyString = buffer.readUtf8();

            log.info("请求方式:【{}】,url:【{}】,请求参数:【{}】, 请求体:【{}】, 返回值: 【{}】",
                    request.method(), request.url(), request.url().query(), bodyString, s);
        } catch (Exception e) {
            log.error("记录内部接口请求日志异常!", e);
        }
    }
}
