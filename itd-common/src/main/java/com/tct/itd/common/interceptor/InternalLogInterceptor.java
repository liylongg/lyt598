package com.tct.itd.common.interceptor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @Description 内部请求参数日志拦截器
 * @Author zhangjiarui
 * @Date 2022/2/23 14:55
 */
@Slf4j
@Configuration
public class InternalLogInterceptor implements Interceptor {

    @Resource
    private AsyncInternalLogService asyncInternalLogService;
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        okhttp3.MediaType mediaType = response.body().contentType();
        String content = response.body().string();

        //异步记录内部接口请求日志
        //asyncInternalLogService.log(chain.request(), content);

        // 重新构建response
        return response.newBuilder()
                .body(okhttp3.ResponseBody.create(mediaType, content))
                .build();
    }
}
