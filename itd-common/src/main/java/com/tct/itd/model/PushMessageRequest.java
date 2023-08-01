package com.tct.itd.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName PushMessageRequest
 * @Description TODO
 * @Author yuhao.L
 * @Date 2021/7/1 17:54
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PushMessageRequest {
    //站台id字符串用逗号隔开
    private String stationIds;
    //发送消息体
    private Message message;
}
