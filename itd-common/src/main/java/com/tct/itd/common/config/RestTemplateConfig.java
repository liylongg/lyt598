package com.tct.itd.common.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class RestTemplateConfig {

    //  客户端从服务端读取数据的超时时间
    private static final int HTTP_TIMEOUT = 20000;
    //  客户端与服务器建立连接的超时时间
    private static final int HTTP_CON_TIMEOUT = 20000;
    //  客户端从连接池中获取连接的超时时间
    private static final int HTTP_CON_REQ_TIMEOUT = 10000;
    //  路由的默认最大连接
    private static final int HTTP_MAX_PERROUTE = 200;
    //  整个连接池连接的最大值
    private static final int HTTP_MAX_TOTAL = 1000;

    @Bean
    public RestTemplate customRestTemplate() {
       /*// 长连接保持30秒
        PoolingHttpClientConnectionManager pollingConnectionManager = new PoolingHttpClientConnectionManager(20, TimeUnit.SECONDS);
        // 总连接数
        pollingConnectionManager.setMaxTotal(500);
        // 同路由的并发数
        pollingConnectionManager.setDefaultMaxPerRoute(500);

        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setConnectionManager(pollingConnectionManager);
        // 重试次数，默认是3次，没有开启
        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(2, true));
        // 保持长连接配置，需要在头添加Keep-Alive
        httpClientBuilder.setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE);

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.16 Safari/537.36"));
        headers.add(new BasicHeader("Accept-Encoding", "gzip,deflate"));
        headers.add(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6"));
        headers.add(new BasicHeader("Connection", "keep-alive"));

        httpClientBuilder.setDefaultHeaders(headers);

        HttpClient httpClient = httpClientBuilder.build();

        // httpClient连接配置，底层是配置RequestConfig
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        //OkHttp3ClientHttpRequestFactory okHttp3ClientHttpRequestFactory = new OkHttp3ClientHttpRequestFactory(okHttpClient);
        // 连接超时
        clientHttpRequestFactory.setConnectTimeout(5000);
        // 数据读取超时时间，即SocketTimeout
        clientHttpRequestFactory.setReadTimeout(50000);
        // 连接不够用的等待时间，不宜过长，必须设置，比如连接不够用时，时间过长将是灾难性的
        clientHttpRequestFactory.setConnectionRequestTimeout(200);
        // 缓冲请求数据，默认值是true。通过POST或者PUT大量发送数据时，建议将此属性更改为false，以免耗尽内存。
        // clientHttpRequestFactory.setBufferRequestBody(false);
        // 添加内容转换器
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        messageConverters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
//        messageConverters.add(new FormHttpMessageConverter());
//        messageConverters.add(new MappingJackson2XmlHttpMessageConverter());
        messageConverters.add(new MappingJackson2HttpMessageConverter());
        messageConverters.add(new ByteArrayHttpMessageConverter());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(messageConverters);
        restTemplate.setRequestFactory(clientHttpRequestFactory);
        //restTemplate.setRequestFactory(okHttp3ClientHttpRequestFactory);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());

        log.info("RestClient initialization complete");
        return restTemplate;*/

        //  创建连接池管理器
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        //  设置socket配置
        SocketConfig socketConfig = SocketConfig.custom()
                .setTcpNoDelay(true)
                .build();
        connManager.setDefaultSocketConfig(socketConfig);
        connManager.setMaxTotal(HTTP_MAX_TOTAL);
        connManager.setDefaultMaxPerRoute(HTTP_MAX_PERROUTE);

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.16 Safari/537.36"));
        headers.add(new BasicHeader("Accept-Encoding", "gzip,deflate"));
        headers.add(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6"));
        headers.add(new BasicHeader("Connection", "keep-alive"));

        //  设置获取连接超时时间、建立连接超时时间、从服务端读取数据的超时时间
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(HTTP_TIMEOUT)
                .setConnectTimeout(HTTP_CON_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_CON_REQ_TIMEOUT)
                .build();
        //  创建httpclient实例
        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(defaultRequestConfig)
                .setDefaultHeaders(headers)
                //.setRetryHandler(new DefaultHttpRequestRetryHandler(2, true))
                .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                .build();

        // httpClient连接配置，底层是配置RequestConfig
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        /*MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        ArrayList<MediaType> mediaTypes = new ArrayList<>();
        //mediaTypes.add(MediaType.TEXT_PLAIN);
        mediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        mediaTypes.add(MediaType.APPLICATION_XML);
        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(mediaTypes);*/
        // 添加内容转换器
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        messageConverters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        messageConverters.add(new FormHttpMessageConverter());
        messageConverters.add(new MappingJackson2HttpMessageConverter());
        messageConverters.add(new ByteArrayHttpMessageConverter());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(messageConverters);
        //restTemplate.setRequestFactory(okHttp3ClientHttpRequestFactory);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
        restTemplate.setRequestFactory(clientHttpRequestFactory);
        return restTemplate;
    }
}
