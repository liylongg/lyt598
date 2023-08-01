package com.tct.itd.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 推送给客户端的msg
 * @author liyunlong
 * @date 2022/5/13 19:50
 */
@Data
@Accessors(chain = true)
public class ClientMsg<T> {
    //报文消息id：雪花算法（long-64位）
    private long msgId;
    //模块编码
    private int moduleCode;
    //消息类型
    private int msgType;
    //消息编码
    private int msgCode;
    //消息时间：2021-05-23 21:25:44.891
    private String msgTs;
    //报文数据信息
    private T msg;

    /**
     * 应答标识
     */
    private int answerFlag = 0;

    public ClientMsg(Class<T> clazz) {
        try {
            if (clazz!=null){
                msg = clazz.newInstance();
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    public ClientMsg(T msg) {
        this.msg=msg;
    }
    public ClientMsg(){}
    public ClientMsg(long msgId, int moduleCode, int msgType, int msgcode, String msgTs, T msg) {
        this.msgId = msgId;
        this.moduleCode = moduleCode;
        this.msgType = msgType;
        this.msgCode = msgcode;
        this.msgTs = msgTs;
        this.msg=msg;
    }

}
