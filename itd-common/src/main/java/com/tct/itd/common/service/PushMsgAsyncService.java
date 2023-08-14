package com.tct.itd.common.service;

import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.config.ChannelGroupManager;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.constant.NumStrConstant;
import com.tct.itd.model.PushMsg;
import com.tct.itd.model.ResendMsg;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.GZIPUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.MasterSlaveUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @classname: PushMsgAsyncService
 * @description: 异步向客户端推送消息
 * @author: liyunlong
 * @date: 2022/7/19 18:12
 */
@Service
@Slf4j
public class PushMsgAsyncService {

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    /**
     * 向客户端推送非心跳消息
     *
     * @param msgJson 推送消息内容
     * @author liyunlong
     * @date 2022/7/19 18:16
     */
    @Async("taskExecutor")
    public void pushMsg(String msgJson,PushMsg pushMsg) {
        String localMasterSlaveStatus = MasterSlaveUtil.getLocalMasterSlaveStatus();
        if (NumStrConstant.ZERO.equals(localMasterSlaveStatus)) {
            log.info("当前服务是从服务,不推送非心跳信息:【{}】", pushMsg);
            return;
        }
        String stationIdsJson = pushMsg.getStationId();
        String[] stationIdList = stationIdsJson.split(",");
        Arrays.asList(stationIdList).parallelStream().forEach(stationId -> {
            for (Map.Entry<String, Channel> entry : ChannelGroupManager.WIN_CHANNEL_GROUP.entrySet()) {
                if (entry.getKey().substring(0, entry.getKey().indexOf(":")).equals(stationId)) {
                    Channel channel = entry.getValue();
                    if (!ChannelGroupManager.checkChannelIsUesed(channel)) {
                        log.error("推送非心跳信息失败,stationId:【{}】对应的netty通道未找到", entry.getKey());
                        ResendMsg sendMsg = ResendMsg.builder().msgJson(JsonUtils.toJSONString(pushMsg)).sendCount(0).stationId(stationId).build();
                        sendMsg.setMsgJson(GZIPUtil.gzip(sendMsg.getMsgJson()));
                        BasicCommonCacheUtils.hPut(Cache.CLIENT_ANSWER, entry.getKey() + ":" + pushMsg.getMessage().getMsgId(), sendMsg);
                        continue;
                    }

                    channel.writeAndFlush(msgJson);
                    ResendMsg resendMsg = ResendMsg.builder().msgJson(JsonUtils.toJSONString(pushMsg)).sendCount(1).stationId(stationId).build();
                    resendMsg.setMsgJson(GZIPUtil.gzip(resendMsg.getMsgJson()));
                    BasicCommonCacheUtils.hPut(Cache.CLIENT_ANSWER, entry.getKey() + ":" + pushMsg.getMessage().getMsgId(), resendMsg);
                    log.info("向客户端:【{}】第【{}】次推送消息,消息内容:【{}】", stationId, 1, JsonUtils.toJSONString(pushMsg));
                }else{
                    atomicInteger.incrementAndGet();
                }
            }
            // 推送消息的stationId没有匹配到通道次数
            int matchNotCount = atomicInteger.get();
            // 推送消息的客户端未连接
            if (matchNotCount == ChannelGroupManager.WIN_CHANNEL_GROUP.size()) {
                ResendMsg resendMsg = ResendMsg.builder().msgJson(JsonUtils.toJSONString(pushMsg)).sendCount(0).stationId(stationId).build();
                resendMsg.setMsgJson(GZIPUtil.gzip(resendMsg.getMsgJson()));
                BasicCommonCacheUtils.hPut(Cache.CLIENT_ANSWER, stationId + ":" + pushMsg.getMessage().getMsgId(),
                        resendMsg);
            }
            atomicInteger.set(NumConstant.ZERO);
        });
    }
}