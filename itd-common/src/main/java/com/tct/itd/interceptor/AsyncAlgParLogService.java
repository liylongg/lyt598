package com.tct.itd.common.interceptor;


import com.tct.itd.constant.SysServiceName;
import com.tct.itd.dto.DeviceConfigDto;
import com.tct.itd.enums.DeviceConfigEnum;
import com.tct.itd.init.config.cache.DeviceConfigHashMapCache;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * @Description 异步处理算法请求参数日志
 * @Author 赵可
 * @Date 2021/12/5 21:09
 */
@Slf4j
@Component
public class AsyncAlgParLogService {

    @Async("taskExecutor")
    public void log(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        String url = request.url().toString();
        if (!StringUtils.isEmpty(url) && url.contains(SysServiceName.ITD_ALG)) {
            log.info("算法请求地址:{}", request.url().toString());
            String requestJson = this.getRequestJson(chain);
            if (StringUtils.isBlank(requestJson)) {
                log.info("算法请求参数Json内容为空");
            } else {
                log.info("算法请求参数Json内容:{}", requestJson);
            }
        }
    }

    /**
     * 获取请求中的算法请求参数
     *
     * @param chain
     * @return
     * @throws IOException
     */
    private String getRequestJson(Interceptor.Chain chain) throws IOException {
        RequestBody requestBody = chain.request().body();
        StringBuilder resultSB = new StringBuilder();
        if (!Objects.isNull(requestBody) &&
                !Objects.isNull(requestBody.contentType()) &&
                StringUtils.isNoneBlank(Objects.requireNonNull(requestBody.contentType()).subtype()) &&
                (Objects.requireNonNull(requestBody.contentType()).subtype().equals("json") ||
                        Objects.requireNonNull(requestBody.contentType()).subtype().equals("plain"))) {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            //获取request的输入流，并设置格式为UTF-8
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(buffer.inputStream(), StandardCharsets.UTF_8));
            //将输入流数据放入StringBuilder
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                resultSB.append(inputStr);
            }
            //将StringBuilder转换为JSONObject
            return resultSB.toString();
        } else {
            return "";
        }
    }
}
