package com.tct.itd.common.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @Description : 接口日志接口切面
 * @Author : zhangjiarui
 * @Date : Created in 2022/2/24
 */
@Aspect
@Component
//@Profile({"test"})
@Slf4j
public class InterfaceLogAspect {

    @Resource
    private AspectInternalLogService aspectInternalLogService;

    @Resource
    private AspectExternalLogService aspectExternalLogService;

    @Pointcut("execution(public * com.tct.itd.*.controller..*.*(..)) ||" + " execution(public * com.tct.itd.basedata.*.easyexcel.tidy.controller..*.*(..))")
    public void pointCut(){}


    @AfterReturning(returning = "result", value = "pointCut()")
    public void doReturn(JoinPoint joinPoint, Object result){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        // 从请求头中获取该请求是否是内部请求
        String header = request.getHeader(HeaderFeignInterceptor.FEIGN_HEADER_NAME);
        if (HeaderFeignInterceptor.FEIGN_HEADER_VALUE.equals(header)){
            aspectInternalLogService.log(request, result, joinPoint);
        } else {
            aspectExternalLogService.log(request, result, joinPoint);
        }
    }
}