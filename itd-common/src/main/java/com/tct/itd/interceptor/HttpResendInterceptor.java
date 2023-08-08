package com.tct.itd.common.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.tct.itd.constant.StringConstant;
import com.tct.itd.constant.SysServiceName;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.IniConfigUtil;
import com.tct.itd.utils.IpUtil;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author kangyi
 * @description 拦截http请求给所有黄紫网发送请求
 * @date 2021/11/16
 **/
@Component
@Slf4j
public class HttpResendInterceptor implements Interceptor {

    @Resource
    private AsyncSendHttp asyncSendHttp;

    @Resource
    private IniConfigUtil iniConfigUtil;

    private static final String CONTENT_TYPE = "application/json;charset=UTF-8";

    //请求失败重发次数
    private static final int REQUEST_FAIL_RESEND_COUNT = 4;

    @Value("${hls.url}")
    private String hlsUrl;

    private static final ReentrantLock LOCK = new ReentrantLock();

    private static final Long LOCK_TIMEOUT = 2L;

    @Override
    public Response intercept(Chain chain) throws IOException {
        return resendHttp(chain);
    }

    private Response resendHttp(Chain chain) throws IOException {
        Request request = chain.request();
        //异步响应返回值
        ConcurrentHashMap<String,ResponseEntity> responseConcurrentHashMap = new ConcurrentHashMap();
        ConcurrentHashMap<String, String> countRespMap = new ConcurrentHashMap();
        HttpUrl httpUrl = request.url();
        String ip = httpUrl.host();
        //和利时接口不重发
        if (ip.equals(hlsUrl.split(":")[1].replaceAll("//", ""))) {
            return chain.proceed(request);
        }
        //获取发送http请求url集合
        List<String> ipSet = IpUtil.HTTP_IP;
        if (CollectionUtils.isEmpty(ipSet)) {
            throw new RuntimeException("获取缓存中后台服务ip为空");
        }
        List<String> newIpList = ipSet.stream().map(ipStr -> changeServiceId(httpUrl.toString(), ipStr)).collect(Collectors.toList());
        Headers headers = request.headers();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        Set<String> names = headers.names();
        HttpHeaders httpHeaders = new HttpHeaders();
        Object body = null;
        //封装restTemplate请求头
        for (String name : names) {
            String val = headers.get(name);
            if (!name.equals(com.tct.itd.common.cache.Cache.HTTP_REQUEST_UUID_KEY)) {
                httpHeaders.add(name, val);
            }
        }
        RequestBody requestBody = request.body();
        if (!Objects.isNull(requestBody) && !Objects.isNull(requestBody.contentType()) &&
                !StringUtils.isEmpty(requestBody.contentType().subtype()) &&
                requestBody.contentType().subtype().equals("json")) {
            httpHeaders.add("Content-Type", "application/json;charset=UTF-8");
            Buffer buffer = new Buffer();
            StringBuilder resultSB = new StringBuilder();
            try {
                requestBody.writeTo(buffer);
                //获取request的输入流，并设置格式为UTF-8
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(buffer.inputStream(), "UTF-8"));
                //将输入流数据放入StringBuilder
                String inputStr;
                while ((inputStr = streamReader.readLine()) != null) {
                    resultSB.append(inputStr);
                }
                //将StringBuilder转换为JSONObject
                if (resultSB.toString().startsWith("[") && resultSB.toString().endsWith("]")) {
                    body = JsonUtils.jsonToObject(resultSB.toString(), Array.class);
                } else {
                    body = JsonUtils.jsonToObject(resultSB.toString(), Object.class);
                }
            } catch (Exception e) {
                body = resultSB.toString();
                log.error("读取请求体失败！请求体body{}", requestBody);
            }
        } else {
            body = params;
        }
        //网关服务把网关值封进请求头
        if (httpUrl.toString().contains(SysServiceName.ITD_GATEWAY)) {
            httpHeaders.add(SysServiceName.ITD_GATEWAY, "1");
        }
        String uuid = UUID.randomUUID().toString();
        //封装uuid进请求头
        httpHeaders.add(com.tct.itd.common.cache.Cache.HTTP_REQUEST_UUID_KEY, uuid);
        httpHeaders.add(com.tct.itd.common.cache.Cache.HTTP_REQUEST_TYPE, "1");
        for (String newUrl : newIpList) {
            //异步发送http请求
            asyncSendHttp.asyncSendHttp(responseConcurrentHashMap, countRespMap, newUrl, body, httpHeaders, request.method(), uuid);
        }
        //把restTemplate响应封装成okhttp相应对象
        return getValidRespond(responseConcurrentHashMap, countRespMap, request, ipSet.size(), httpHeaders, newIpList, body);
    }

    /**
     * 修改FeignClient生成的URL中的serviceId
     *
     * @param url        URL字符串
     * @param nServiceId 新serviceId
     * @return 替换之后的字符串
     */
    private String changeServiceId(String url, String nServiceId) {
        StringBuilder sb = new StringBuilder();
        // 获取协议
        String protocol = url.split("://")[0];
        sb.append(protocol);
        sb.append("://");
        sb.append(nServiceId);
        sb.append(":");
        String[] tempStr = url.split("://")[1].split("/");
        String newPort = iniConfigUtil.getPortByService(tempStr[1]);
        sb.append(newPort);
        sb.append(url.split(tempStr[1])[1]);
        return sb.toString();
    }


    public Response getValidRespond(ConcurrentHashMap<String,ResponseEntity> responseConcurrentHashMap, Map<String, String> countRespMap, Request request, int totalRequest,
                                    HttpHeaders httpHeaders, List<String> newIpList, Object body) {
        log.debug("开始获取restTemplate响应对象");
        int resendCount = 0;
        while (true) {
            Set<Map.Entry<String,ResponseEntity>> entries = responseConcurrentHashMap.entrySet();
            try {
                if (LOCK.tryLock(LOCK_TIMEOUT, TimeUnit.SECONDS)) {
                    if (entries.size() != 0) {
                        for (Map.Entry<String,ResponseEntity > entry : entries) {
                            ResponseEntity responseEntity = entry.getValue();
                            if (CodeEnum.SUCCESS.getCode() == responseEntity.getStatusCode().value() && responseEntity.hasBody() && !Objects.isNull(responseEntity.getBody())) {
                                String bodyStr = responseEntity.getBody().toString();
                                BaseResponse baseResponse;
                                //log.debug("获取请求到url:{},restTemplate响应对象", request.url());
                                if (bodyStr.contains("code") && bodyStr.contains("success")) {
                                    baseResponse = JsonUtils.jsonToObject(responseEntity.getBody().toString(), BaseResponse.class);
                                } else {
                                    return getResponse(request, responseEntity, bodyStr.getBytes(StandardCharsets.UTF_8));
                                }
                                //获取项目自定义封装的响应对象
                                //过滤掉重复发送http请求的响应
                                //过滤掉从服务器http请求的响应
                                if (baseResponse.getCode() != CodeEnum.DUPLICATE_HTTP_REQUEST.getCode()
                                        && baseResponse.getCode() != CodeEnum.SLAVE_HTTP_REQUEST.getCode()) {
                                    byte[] bytes = JsonUtils.toJSONString(baseResponse).getBytes(StandardCharsets.UTF_8);
                                    return getResponse(request, responseEntity, bytes);
                                }
                            }
                        }
                    }
                    //响应结果数等于请求数，并且全部响应失败
                    if (entries.size() == totalRequest && checkAllRequestResult(entries)) {
                        log.error("获取请求url:{}失败！所有请求已经返回，返回值错误！,响应值:{}", request.url().toString(), responseConcurrentHashMap.entrySet());
                        return new Response.Builder()
                                .protocol(Protocol.HTTP_1_0)
                                .request(request)
                                .code(CodeEnum.INTERFACE_CALL_EXCEPTION.getCode())
                                .message(CodeEnum.INTERFACE_CALL_EXCEPTION.getMsg())
                                .build();
                    }
                    //响应结果数等于请求数，并且全部响应失败
                    if (!CollectionUtils.isEmpty(countRespMap) && countRespMap.size() == newIpList.size() && checkAllRequestResult(entries)) {
                        log.error("获取请求url:{}失败！所有请求已经执行,无正确响应结果,跳出循环！,响应值:{}", request.url().toString(), responseConcurrentHashMap.entrySet());
                        return new Response.Builder()
                                .protocol(Protocol.HTTP_1_0)
                                .request(request)
                                .code(CodeEnum.INTERFACE_CALL_EXCEPTION.getCode())
                                .message(CodeEnum.INTERFACE_CALL_EXCEPTION.getMsg())
                                .build();
                    }
                    //所有请求返回，没有返回成功值，重发
                    /*if (entries.size() == totalRequest && resendCount < REQUEST_FAIL_RESEND_COUNT) {
                        responseConcurrentHashMap.clear();
                        String uuid = UUID.randomUUID().toString();
                        //封装uuid进请求头
                        httpHeaders.add(com.tct.itd.common.cache.Cache.HTTP_REQUEST_UUID_KEY, uuid);
                        log.info("所有请求已经返回，全部失败，开始重发。url{}",JsonUtils.toJSONString(newIpList));
                        for (String newUrl : newIpList) {
                            //异步发送http请求
                            asyncSendHttp.asyncSendHttp(responseConcurrentHashMap, newUrl, body, httpHeaders, request.method(), uuid);
                        }
                        resendCount++;
                    }
                    //重发三次后，全部失败返回失败信息
                    if (entries.size() == totalRequest && resendCount == (REQUEST_FAIL_RESEND_COUNT - 1)) {
                        log.error("获取请求url:{}失败！所有请求已经返回，返回值错误！,响应值:{}", request.url().toString(), responseConcurrentHashMap.entrySet());
                        return new Response.Builder()
                                .protocol(Protocol.HTTP_1_0)
                                .request(request)
                                .code(CodeEnum.INTERFACE_CALL_EXCEPTION.getCode())
                                .message(CodeEnum.INTERFACE_CALL_EXCEPTION.getMsg())
                                .build();
                    }*/
                }
                Thread.sleep(250L);
            } catch (Exception e) {
                log.error("获取请求Request:{},restTemplate响应对象异常", request, e);
            } finally {
                if (LOCK.isHeldByCurrentThread()) {
                    LOCK.unlock();
                }
            }
        }
    }

    //校验所有返回响应，是否全部失败（true表示有成功响应，如果返回false表示全部失败）
    private boolean checkAllRequestResult(Set<Map.Entry<String,ResponseEntity>> entries){
        if (entries.size() != 0) {
            for (Map.Entry<String, ResponseEntity> entry : entries) {
                ResponseEntity responseEntity = entry.getValue();
                if (CodeEnum.SUCCESS.getCode() == responseEntity.getStatusCode().value() && responseEntity.hasBody() && !Objects.isNull(responseEntity.getBody())) {
                    String bodyStr = responseEntity.getBody().toString();
                    BaseResponse baseResponse;
                    if (bodyStr.contains("code") && bodyStr.contains("success")) {
                        baseResponse = JsonUtils.jsonToObject(responseEntity.getBody().toString(), BaseResponse.class);
                    } else {
                        return false;
                    }
                    //获取项目自定义封装的响应对象
                    //过滤掉重复发送http请求的响应
                    //过滤掉从服务器http请求的响应
                    if (baseResponse.getCode() != CodeEnum.DUPLICATE_HTTP_REQUEST.getCode()
                            && baseResponse.getCode() != CodeEnum.SLAVE_HTTP_REQUEST.getCode()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private Response getResponse(Request request, ResponseEntity responseEntity, byte[] bytes) {
        //http响应对象请求头
        HttpHeaders httpHeaders = responseEntity.getHeaders();
        MediaType mediaType = MediaType.parse(CONTENT_TYPE);
        //封装OKhttp响应对象
        ResponseBody responseBody = ResponseBody.create(mediaType, bytes);
        Response response = new Response.Builder()
                .protocol(Protocol.HTTP_1_0)
                .body(responseBody)
                .request(request)
                .code(CodeEnum.SUCCESS.getCode())
                .message(CodeEnum.SUCCESS.getMsg())
                .headers(Headers.of(httpHeaders.toSingleValueMap()))
                .build();
        return response;
    }

}
