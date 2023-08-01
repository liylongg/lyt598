package com.tct.itd.model;

import com.tct.itd.utils.Md5Utils;
import com.tct.itd.utils.SpringContextUtil;
import lombok.Data;
import org.apache.commons.lang.math.RandomUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class Message implements Serializable {
    //报文消息id：雪花算法（long-64位）
    private long msgId;
    //消息处理器类型
    private String handleType;
    //消息类型
    private int msgType;
    //路由键
    private String routingKey;
    //校验 sign = MD5(msgId+key+msg+msgTs)
    private String sign;
    //时间戳
    private String msgTs;
    //报文数据信息
    private Object msg;

    public Message() {
    }

    public Message(long msgId, int msgType, String routingKey, String sign, String msgts, Object msg) {
        this.msgId = msgId;
        this.msgType = msgType;
        this.routingKey = routingKey;
        this.sign = sign;
        this.msgTs = msgts;
        this.msg = msg;
    }

    public Message(Integer msgType){
        this.msgType = msgType;
        Long msgId = SpringContextUtil.threadLocal.get();
        if(msgId != null){
            this.msgId = msgId;
        }else {
            this.msgId = RandomUtils.nextLong();
        }
        this.msgTs = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        this.sign = Md5Utils.string2Md5(msgId + SpringContextUtil.getValue("spring.application.name") + msg + msgTs);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"msgId\":")
                .append(msgId);
        sb.append(",\"msgType\":")
                .append(msgType);
        sb.append(",\"routingKey\":\"")
                .append(routingKey).append('\"');
        sb.append(",\"sign\":\"")
                .append(sign).append('\"');
        sb.append(",\"msgts\":\"")
                .append(msgTs).append('\"');
        sb.append(",\"msg\":")
                .append(msg);
        sb.append('}');
        return sb.toString();
    }
}
