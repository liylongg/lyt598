package com.tct.itd.common.config;

import com.tct.itd.common.interceptor.AlgParLogInterceptor;
import com.tct.itd.common.interceptor.HttpResendInterceptor;
import com.tct.itd.common.interceptor.InternalLogInterceptor;
import feign.Feign;
import okhttp3.ConnectionPool;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Description Feign配置OkHttp连接池
 * @Author zhaoke
 * @Date 2020/5/19 10:30
 **/
@Configuration
@ConditionalOnClass(Feign.class)
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class FeignConfig {

    @Resource
    private AlgParLogInterceptor algParLogInterceptor;

    @Resource
    private InternalLogInterceptor internalLogInterceptor;

    @Resource
    private HttpResendInterceptor httpResendInterceptor;

    @Bean
    public okhttp3.OkHttpClient okHttpClient() {
        return new okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                //modified by kangyi 偶现空指针，暂时注释
                //.addInterceptor(interceptor)
                .addInterceptor(algParLogInterceptor)
                // 内部接口日志记录
                //.addInterceptor(internalLogInterceptor)
                //modified by kangyi 全局feign拦截器，暂时注释
                .addInterceptor(httpResendInterceptor)
                .build();
    }
}
