package com.tct.itd.common.interceptor;

import com.tct.itd.enums.CodeEnum;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * @author zhoukun
 * @description 设置okhttp重试次数
 * @date 2022/11/24
 **/
@Component
@Slf4j
public class OkHttpRetryInterceptor implements Interceptor {

    //最大重试次数
    public int executionCount;

    //重试的间隔
    private long retryInterval;

    public OkHttpRetryInterceptor() {
    }

    OkHttpRetryInterceptor(Builder builder) {
        this.executionCount = builder.executionCount;
        this.retryInterval = builder.retryInterval;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = doRequest(chain, request);
        int retryNum = 0;
        while ((response == null || !response.isSuccessful()) && retryNum <= executionCount) {
//            log.info("intercept Request is not successful - {}", retryNum);
            final long nextInterval = getRetryInterval();
            try {
//                log.info("Wait for {}", nextInterval);
                Thread.sleep(nextInterval);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }
            retryNum++;
            // retry the request
            response = doRequest(chain, request);
        }
        Response response1 = new Response.Builder()
                .protocol(Protocol.HTTP_1_0)
                .body(null)
                .request(request)
                .code(CodeEnum.SERVER_EXCEPTION.getCode())
                .message(CodeEnum.SERVER_EXCEPTION.getMsg())
//                .headers()
                .build();
        return response==null ? response1 : response;
    }

    private Response doRequest(Chain chain, Request request) {
        Response response = null;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
        }
        return response;
    }

    /**
     * retry间隔时间
     */
    public long getRetryInterval() {
        return this.retryInterval;
    }

    public static final class Builder {
        private int executionCount;
        private long retryInterval;

        public Builder() {
            executionCount = 3;
            retryInterval = 1000;
        }

        public OkHttpRetryInterceptor.Builder executionCount(int executionCount) {
            this.executionCount = executionCount;
            return this;
        }

        public OkHttpRetryInterceptor.Builder retryInterval(long retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        public OkHttpRetryInterceptor build() {
            return new OkHttpRetryInterceptor(this);
        }
    }


}

