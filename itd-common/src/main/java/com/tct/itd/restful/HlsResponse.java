package com.tct.itd.restful;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Description restful 统一封装对象
 * @Author zhaoke
 * @Date 2020/7/15 23:20
 **/
@Data
@AllArgsConstructor
public class HlsResponse<T> {

    private Integer code;

    private String message;

    private T results;

    public boolean state(){
        return this.code == 200;
    }

    public static <T> HlsResponse<T> success(T date){
        return new HlsResponse<>(200,"请求成功",date);
    }

    public static <T> HlsResponse<T> fail(Integer code, String message){
        return new HlsResponse<>(code,message,null);
    }
}
