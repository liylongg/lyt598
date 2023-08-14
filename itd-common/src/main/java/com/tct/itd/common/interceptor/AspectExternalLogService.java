package com.tct.itd.common.interceptor;


import com.tct.itd.restful.BaseResponse;
import com.tct.itd.restful.HlsResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * @Description 处理外部接口(非Feign)请求日志
 * @Author zjr
 * @Date 2022/2/25 21:09
 */
@Slf4j
@Component
public class AspectExternalLogService {

    public void log(HttpServletRequest request, Object s, JoinPoint joinPoint){
        try {
            // 从切点中获取信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            //参数名
            String[] parameterNames = signature.getParameterNames();
            // 参数值
            Object[] args = joinPoint.getArgs();
            // 参数类型
            Class[] parameterTypes = signature.getParameterTypes();

            // 方法名
            String method = joinPoint.getSignature().getName();

            //适配GlobalResponseHandle对全局除了String结果进行封装baseResponse
            if (!(s instanceof BaseResponse) && !(s instanceof HlsResponse) && !(s instanceof String)){
                s = BaseResponse.success(s).toString();
            }

            log.info("外部接口请求方式:【{}】, 端口:【{}】, url:【{}】, 请求方法:【{}】, 请求参数名【{}】,请求参数类型【{}】 , 请求参数值:【{}】, 返回值: 【{}】",
                    request.getMethod(), request.getLocalPort(), request.getRequestURI(), method, parameterNames, parameterTypes, args, s);
        } catch (Throwable e) {
            log.error("记录内部接口请求日志异常! url:"+request.getRequestURI(), e);
        }
    }
}
