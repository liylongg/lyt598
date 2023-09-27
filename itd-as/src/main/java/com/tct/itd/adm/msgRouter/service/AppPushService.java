package com.tct.itd.adm.msgRouter.service;

import com.tct.itd.client.AppPushClient;
import com.tct.itd.common.dto.Info;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.model.Message;
import com.tct.itd.model.PushMessageRequest;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @Description: 发送通知
 * @Author: zhangyinglong
 * @Date:2021/5/18 15:26
 */

@Slf4j
@Component
public class AppPushService {

    @Resource
    private AppPushClient appPushClient;

    //循环推送报警弹窗，间隔时间
    private static final long DELAY_TIME = 6000L;

    /**
     * @description 循环往前台推送弹窗
     * @date 2022/11/4 17:55
     * @author kangyi
     * @param t 通知内容
     * @param count 循环次数 前端显示时间 次数 * 5秒
     * @return: void
     */
    @Async("taskExecutor")
    public void circlePopupWindow(WebNoticeDto t, int count) {
        log.info("发送app-push 业务码MsgTypeEnum.code【{}】", MsgTypeEnum.WEB_NOTICE.getMsgType());
        while (count > 1) {
            sendWebNoticeMessageToAny(t);
            count--;
            try {
                Thread.sleep(DELAY_TIME);
            } catch (InterruptedException e) {
                log.error("线程休眠异常");
            }
        }
    }


    public <T> void sendMessage(MsgTypeEnum msgTypeEnum, T t) {
        Message message = new Message(UidGeneratorUtils.getUID(),
                msgTypeEnum.getMsgType(),
                "", "null", DateUtil.getTimeStamp(),
                t);
        //发送通知,这里的StationId只起校验websocket连接作用
        sendMessageByRestFull(message, "0");
    }

    public <T> void sendMessage(MsgTypeEnum msgTypeEnum, WebNoticeDto t) {
        Message message = new Message(UidGeneratorUtils.getUID(),
                msgTypeEnum.getMsgType(),
                "", "null", DateUtil.getTimeStamp(),
                t);
        //发送通知,这里的StationId只起校验websocket连接作用
        sendMessageByRestFull(message, t.getStationIds());
    }

    public void sendWebNoticeMessageToAny(WebNoticeDto t) {
        log.info("发送app-push 业务码MsgTypeEnum.code【{}】", MsgTypeEnum.WEB_NOTICE.getMsgType());
        Message message = new Message(UidGeneratorUtils.getUID(),
                MsgTypeEnum.WEB_NOTICE.getMsgType(),
                "", "null", DateUtil.getTimeStamp(),
                t);
        //这里的StationId只起校验websocket连接作用
        sendMessageByRestFull(message, t.getStationIds());
    }


    public void sendWebNoticeDcMessageToAny(WebNoticeDto webNoticeDto) {
        log.info("调用app-push模块发送信息,业务码MsgTypeEnum.code【{}】", MsgTypeEnum.WEB_NOTICE_DC.getMsgType());
        Message message = new Message(UidGeneratorUtils.getUID(),
                MsgTypeEnum.WEB_NOTICE_DC.getMsgType(),
                "", "null", DateUtil.getTimeStamp(),
                webNoticeDto);
        //这里的StationId只起校验websocket连接作用
        sendMessageByRestFull(message, webNoticeDto.getStationIds());
    }


    public void sendWebNoticeToAny(WebNoticeDto webNoticeDto, int code) {
        log.info("调用app-push模块发送信息,业务码MsgTypeEnum.code【{}】", MsgTypeEnum.WEB_NOTICE_GRAPH_CASE.getMsgType());
        Message message = new Message(UidGeneratorUtils.getUID(),
                code,
                "", "null", DateUtil.getTimeStamp(),
                webNoticeDto);
        //这里的StationId只起校验websocket连接作用
        sendMessageByRestFull(message, webNoticeDto.getStationIds());
    }

    //这里的StationId不起作用，无需理会
    public <T> void sendMessageByRestFull(Message message, String stationIds) {
        //发送通知
        Object t = message.getMsg();
        try {
            if (t instanceof Info) {
                Object obj = ((Info) t).getData();
                if (obj instanceof AdmIdea) {
                    stationIds = String.valueOf(((AdmIdea) obj).getPushTargetStationId());
                }
            }
            BaseResponse<String> baseResponse = appPushClient.pushData(new PushMessageRequest(stationIds, message));
            log.info("将消息发送给推送服务：目标stationId[{}],推送结果[{}]", stationIds, baseResponse.state());
        } catch (Exception e) {
            log.error("发送通知失败,msgType:" + message.getMsgType() + "msgInfo:" + JsonUtils.toJSONString(message), e);
        }
    }
}
