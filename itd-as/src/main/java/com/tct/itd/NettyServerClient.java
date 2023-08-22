package com.tct.itd;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.Executor;


/**
 * @Description netty服务端启动类(预留)
 * @Author zhaoke
 * @Date 2021/4/13 16:24
 **/
@Slf4j
//@Component
public class NettyServerClient implements ApplicationRunner {

    @Value("${server.port}")
    private int serverPort;

    @Resource(name = "taskExecutor")
    private Executor threadPool;

    @Override
    public void run(ApplicationArguments args)  {
        threadPool.execute(this::start);
    }

    private void start() {
        int nettyPort = serverPort + 1;
    }


}
