package com.tct.itd.common.interceptor;

import okhttp3.Interceptor;
import okhttp3.Response;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @Description 算法请求参数日志拦截器
 * @Author zhaoke
 * @Date 2021/12/5 14:55
 */
@Configuration
public class AlgParLogInterceptor implements Interceptor {

    @Resource
    private AsyncAlgParLogService aysnAlgParLogService;

    @Override
    public Response intercept(Chain chain) throws IOException {
        //异步记录算法服务接口请求参数
        aysnAlgParLogService.log(chain);
        return chain.proceed(chain.request());
    }




}
