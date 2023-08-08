package com.tct.itd.common.config;

import com.tct.itd.common.service.PushMsgAsyncService;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.constant.StringConstant;
import com.tct.itd.model.PushMsg;
import com.tct.itd.utils.GZIPUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.NettyUtils;
import com.tct.itd.utils.SpringContextUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description 连接通道分组配置类
 * @Author zhaoke
 * @Date 2022/4/13 17:26
 */
@Slf4j
public class ChannelGroupManager {


    private ChannelGroupManager() {
    }

    /**
     * 客户端长连接管理
     */
    public static final Map<String, Channel> WIN_CHANNEL_GROUP = new ConcurrentHashMap<>();

    /**
     * 应用服务长连接管理
     */
    public static final Map<String, Channel> AS_CHANNEL_GROUP = new ConcurrentHashMap<>();

    /**
     * 算法服务长连接管理
     */
    public static final Map<String, Channel> ALG_CHANNEL_GROUP = new ConcurrentHashMap<>();

    /**
     * 网关服务长连接管理
     */
    public static final Map<String, Channel> GATEWAY_CHANNEL_GROUP = new ConcurrentHashMap<>();


    private static final ReentrantLock CHANNEL_LOCK = new ReentrantLock();

    /**
     * 添加通道到对应通道管理中
     *
     * @param context 通道上下文
     */
    public static void addChannel(ChannelHandlerContext context) {
        Channel channel = context.channel();
        String udid = (String) context.channel().attr(AttributeKey.valueOf(StringConstant.UDID)).get();
        switch (udid) {
            case "win":
                String stationId =
                        (String) context.channel().attr(AttributeKey.valueOf(StringConstant.STATIONID)).get();
                ChannelGroupManager.addWinChannel(stationId, channel);
                log.info("检测到客户端建立连接通道,将通道存入客户端通道管理组,客户端ID【{}】,通道ID[{}]", stationId, channel.id().asShortText());
                break;
            case "as":
                String asStationId =
                        (String) context.channel().attr(AttributeKey.valueOf(StringConstant.STATIONID)).get();
                ChannelGroupManager.addAsChannel(asStationId, channel);
                log.info("检测到应用服务(as)建立连接通道，将通道存入应用服务通道管理组,应用服务(as)ID【{}】,通道ID[{}]", asStationId,
                        channel.id().asShortText());
                break;
            case "alg":
                String algStationId =
                        (String) context.channel().attr(AttributeKey.valueOf(StringConstant.STATIONID)).get();
                ChannelGroupManager.addAlgChannel(algStationId, channel);
                log.info("检测到算法服务(alg)建立连接通道，将通道存入应用服务通道管理组,算法服务(alg)ID【{}】,通道ID[{}]", algStationId,
                        channel.id().asShortText());
                break;
            case "gateway":
                String gatewayStationId =
                        (String) context.channel().attr(AttributeKey.valueOf(StringConstant.STATIONID)).get();
                ChannelGroupManager.addGatewayChannel(gatewayStationId, channel);
                log.info("检测到网关建立连接通道，将通道存入应用服务通道管理组,网关服务(gateway)ID【{}】,通道ID[{}]", gatewayStationId,
                        channel.id().asShortText());
                break;
            default:
                channel.close();
                log.info("IP:{}检测设备码[{}]为非法设备,禁止建立连接", NettyUtils.getClientIp(context), udid);
        }
    }

    /**
     * 根据target目标将消息路由到指定服务中
     *
     * @param target  目标服务
     * @param msgJson 消息json
     */
    public static void routerMsg(String target, String msgJson) {
        switch (target) {
            case "win":
                PushMsg pushMsg = JsonUtils.jsonToObject(msgJson, PushMsg.class);
                int msgType = pushMsg.getMessage().getMsgType();
                boolean isHeartMsg = NumConstant.NEGATIVE_ONE.equals(msgType);
                 String stationIdsJson = JsonUtils.jsonToNodeStr(msgJson, StringConstant.STATIONID);
                String[] stationIdList = stationIdsJson.split(",");
                String newMsgJson= GZIPUtil.gzip(JsonUtils.toJSONString(pushMsg));
                // 推送心跳信息
                if (isHeartMsg) {
                    Arrays.asList(stationIdList).parallelStream().forEach(stationId -> {
                        Channel channel = ChannelGroupManager.WIN_CHANNEL_GROUP.get(stationId);
                        if (!checkChannelIsUesed(channel)) {
                            log.error("回复心跳信息失败,stationId:【{}】对应的netty通道未找到", stationId);
                            return;
                        }
                        channel.writeAndFlush(newMsgJson);
                    });
                }
                // 推送非心跳信息
                else {
                    PushMsgAsyncService pushMsgAsyncService = (PushMsgAsyncService) SpringContextUtil.getBean("pushMsgAsyncService");
                    pushMsgAsyncService.pushMsg(newMsgJson,pushMsg);
                }
                break;
            case "as":
                String asStationId = "as:" + JsonUtils.jsonToNodeStr(msgJson, StringConstant.STATIONID);
                Channel channel = ChannelGroupManager.AS_CHANNEL_GROUP.get(asStationId);
                if (Boolean.FALSE.equals(checkChannelIsUesed(channel))) {
                    log.error("回复应用服务(as)心跳信息失败,asStationId:【{}】对应的netty通道未找到", asStationId);
                    return;
                }
                channel.writeAndFlush(msgJson);
                break;
            case "alg":
                String algStationId = JsonUtils.jsonToNodeStr(msgJson, StringConstant.STATIONID);
                Channel algChannel = ChannelGroupManager.ALG_CHANNEL_GROUP.get(algStationId);
                if (Boolean.FALSE.equals(checkChannelIsUesed(algChannel))) {
                    log.error("回复算法服务(alg)心跳信息失败,algStationId:【{}】对应的netty通道未找到", algStationId);
                    return;
                }
                algChannel.writeAndFlush(msgJson);
                break;
            case "gateway":
                String gatewayStationId = JsonUtils.jsonToNodeStr(msgJson, StringConstant.STATIONID);
                Channel gatewayChannel = ChannelGroupManager.GATEWAY_CHANNEL_GROUP.get(gatewayStationId);
                if (Boolean.FALSE.equals(checkChannelIsUesed(gatewayChannel))) {
                    log.error("回复网关服务(gateway)心跳信息失败,gatewayStationId:【{}】对应的netty通道未找到", gatewayStationId);
                    return;
                }
                gatewayChannel.writeAndFlush(msgJson);
                break;
            default:
                log.info("未知目标类别[{}],无法将消息进行路由,消息内容:{}", target, msgJson);
        }
    }

    /**
     * 添加客户端长连接到管理组
     *
     * @param stationId 集中区ID
     * @param channel   通道
     */
    private static void addWinChannel(String stationId, Channel channel) {
        try {
            if (CHANNEL_LOCK.tryLock(500L, TimeUnit.MILLISECONDS)) {
                if (WIN_CHANNEL_GROUP.containsKey(stationId)) {
                    WIN_CHANNEL_GROUP.remove(stationId);
                }
                WIN_CHANNEL_GROUP.put(stationId, channel);
                log.info("客户端:【{}】建立连接,客户端WIN_CHANNEL_GROUP数量:【{}】,stationId:【{}】", stationId,
                        WIN_CHANNEL_GROUP.size(), JsonUtils.toJSONString(WIN_CHANNEL_GROUP.keySet()));
            }
        } catch (InterruptedException e) {
            log.error("管理组添加长连接并发加锁异常", e);
        } finally {
            CHANNEL_LOCK.unlock();
        }
    }

    /**
     * 添加as服务长连接到管理组
     *
     * @param stationId 集中区ID
     * @param channel   通道
     */
    private static void addAsChannel(String stationId, Channel channel) {
        try {
            if (CHANNEL_LOCK.tryLock(500L, TimeUnit.MILLISECONDS)) {
                if (AS_CHANNEL_GROUP.containsKey(stationId)) {
                    AS_CHANNEL_GROUP.remove(stationId);
                }
                AS_CHANNEL_GROUP.put(stationId, channel);
                log.info("应用服务(as):【{}】建立连接,应用服务AS_CHANNEL_GROUP数量:【{}】,stationId:【{}】", stationId,
                        AS_CHANNEL_GROUP.size(), JsonUtils.toJSONString(AS_CHANNEL_GROUP.keySet()));
            }
        } catch (InterruptedException e) {
            log.error("管理组添加长连接并发加锁异常", e);
        } finally {
            CHANNEL_LOCK.unlock();
        }
    }

    /**
     * 添加网关服务长连接到管理组
     *
     * @param stationId 集中区ID
     * @param channel   通道
     */
    private static void addGatewayChannel(String stationId, Channel channel) {
        try {
            if (CHANNEL_LOCK.tryLock(500L, TimeUnit.MILLISECONDS)) {
                if (GATEWAY_CHANNEL_GROUP.containsKey(stationId)) {
                    GATEWAY_CHANNEL_GROUP.remove(stationId);
                }
                GATEWAY_CHANNEL_GROUP.put(stationId, channel);
                log.info("网关服务(gateway):【{}】建立连接,网关服务GATEWAY_CHANNEL_GROUP数量:【{}】,stationId:【{}】", stationId,
                        GATEWAY_CHANNEL_GROUP.size(), JsonUtils.toJSONString(GATEWAY_CHANNEL_GROUP.keySet()));
            }
        } catch (InterruptedException e) {
            log.error("管理组添加长连接并发加锁异常", e);
        } finally {
            CHANNEL_LOCK.unlock();
        }
    }

    /**
     * 添加alg服务长连接到管理组
     *
     * @param stationId 集中区ID
     * @param channel   通道
     */
    private static void addAlgChannel(String stationId, Channel channel) {
        try {
            if (CHANNEL_LOCK.tryLock(500L, TimeUnit.MILLISECONDS)) {
                if (ALG_CHANNEL_GROUP.containsKey(stationId)) {
                    ALG_CHANNEL_GROUP.remove(stationId);
                }
                ALG_CHANNEL_GROUP.put(stationId, channel);
                log.info("算法服务(alg):【{}】建立连接,网关服务ALG_CHANNEL_GROUP数量:【{}】,stationId:【{}】", stationId,
                        ALG_CHANNEL_GROUP.size(), JsonUtils.toJSONString(ALG_CHANNEL_GROUP.keySet()));
            }
        } catch (InterruptedException e) {
            log.error("管理组添加长连接并发加锁异常", e);
        } finally {
            CHANNEL_LOCK.unlock();
        }
    }

    /**
     * 检查通道是否可用
     * @author liyunlong
     * @date 2023/2/9 20:33
     * @param channel 通道
     * @return java.lang.Boolean
     */
    public static Boolean checkChannelIsUesed(Channel channel) {
        return Objects.nonNull(channel) && channel.isWritable();
    }
}
