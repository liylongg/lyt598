package com.tct.itd.restful;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * @Description restful接口返回值封装控制器
 * @Author zhaoke
 * @Date 2020/7/15 23:18
 **/
@Slf4j
@ControllerAdvice(annotations = {RestController.class})
public class GlobalResponseHandle implements ResponseBodyAdvice<Object> {

//    private final static List<String> ignoredController = new ArrayList<>();

    //返回值忽略包装的controller
//    static {
//        ignoredController.add("com.tct.iids.easyexcel.tidy.controller.ElectronicMapController");
//    }


    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
//        Class<?> declaringClass = methodParameter.getDeclaringClass();
//        if(ignoredController.contains(declaringClass.getCanonicalName())){
//            //直接返回
//            return false;
//        }

        final String returnTypeName = methodParameter.getParameterType().getTypeName();

        //HlsResponse与BaseResponse不处理
        return !"com.tct.itd.restful.BaseResponse".equals(returnTypeName) &&
            !"com.tct.itd.restful.HlsResponse".equals(returnTypeName);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        final String returnTypeName = methodParameter.getParameterType().getTypeName();
        if("void".equals(returnTypeName)){
            return BaseResponse.success(null);
        }
        if(!mediaType.includes(MediaType.APPLICATION_JSON) || !mediaType.includes(MediaType.APPLICATION_JSON_UTF8)){
            return body;
        }
        if("com.tct.itd.restful.BaseResponse".equals(returnTypeName)){
            return body;
        }
        return BaseResponse.success(body);
    }
}
