package com.tct.itd.restful;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.EMapDataException;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Description restful 统一封装对象
 * @Author zhaoke
 * @Date 2020/7/15 23:20
 **/
@Data
@AllArgsConstructor
public class BaseResponse<T> {

    private Integer code;

    private Boolean success;

    private String message;

    private T data;

    public boolean state(){
        return this.code == 200;
    }

    public static <T> BaseResponse<T> success(){
        return new BaseResponse<>(200,true,"请求成功", null);
    }

    public static <T> BaseResponse<T> success(T date){
        return new BaseResponse<>(200,true,"请求成功",date);
    }

    public static <T> BaseResponse<T> fail(Integer code,String message){
        return new BaseResponse<>(code,false,message,null);
    }

    public static <T> BaseResponse<T> fail(CodeEnum codeEnum) {
        return new BaseResponse<>(codeEnum.getCode(), false, codeEnum.getMsg(), null);
    }

    /**
     * 封装异常处理机制
     * @return T
     */
    @JsonIgnore
    public T getDataForEmap() {
        if (state()){
            return data;
        }
        throw new EMapDataException(500, message);
    }
}
