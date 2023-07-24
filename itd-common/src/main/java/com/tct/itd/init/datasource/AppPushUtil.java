package com.tct.itd.init.datasource;

import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.model.Info;
import com.tct.itd.model.Message;
import com.tct.itd.model.PushMessageRequest;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Component;

/**
 * @Description: 发送通知
 * @Author: zhangyinglong
 * @Date:2021/5/18 15:26
 */

@Slf4j
@Component
public class AppPushUtil {

    public static void sendWebNoticeMessageToAny(WebNoticeDto t) {
        log.info("发送app-push 业务码MsgTypeEnum.code【{}】", MsgTypeEnum.WEB_NOTICE.getMsgType());
        Message message = new Message(RandomUtils.nextLong(),
                MsgTypeEnum.WEB_NOTICE.getMsgType(),
                "","null", DateUtil.getTimeStamp(),
                t);
        //这里的StationId只起校验websocket连接作用
        sendMessageByRestFull(message,t.getStationIds());
    }



    //这里的StationId不起作用，无需理会
    public static <T> void sendMessageByRestFull(Message message, String stationIds){
        //发送通知
        Object t = message.getMsg();
        try {
            if (t instanceof Info){
                Object obj = ((Info) t).getData();
                if(obj instanceof AdmIdea){
                    stationIds = String.valueOf(((AdmIdea) obj).getPushTargetStationId());
                }
            }
            AppPushUtilClient appPushClient = SpringContextUtil.getBean(AppPushUtilClient.class);
            BaseResponse<String> baseResponse = appPushClient.pushData(new PushMessageRequest(stationIds, message));
            log.info("将消息发送给推送服务：目标stationId[{}],推送结果[{}]",stationIds,baseResponse.state());
        } catch (Exception e) {
            log.error("发送通知失败,msgType:" + message.getMsgType() + "msgInfo:"+ JsonUtils.toJSONString(message),e);
        }
    }
}
