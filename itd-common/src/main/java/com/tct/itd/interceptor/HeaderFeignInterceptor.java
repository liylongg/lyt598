package com.tct.itd.common.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * @Description 全局feign拦截，将请求header信息放入feign请求头中
 * @Author zhaoke
 * @Date 2020-10-28 14:07
 */
@Configuration
public class HeaderFeignInterceptor implements RequestInterceptor {
    public static final String FEIGN_HEADER_NAME = "Feign";
    public static final String FEIGN_HEADER_VALUE = "Internal";

    @Override
    public void apply(RequestTemplate requestTemplate) {
        if(RequestContextHolder.getRequestAttributes() != null){
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    String values = request.getHeader(name);
                    requestTemplate.header(name, values);
                }
            }

            // 在feign中的header加入标志, 以和其他区分
            requestTemplate.header(FEIGN_HEADER_NAME, FEIGN_HEADER_VALUE);
        }
    }
}
