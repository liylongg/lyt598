package com.tct.itd.model;

import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.DisposeDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName CacheDataWrapper
 * @Description Cache包装类，用于本地缓存设置过期时间
 * @Author zhoukun
 * @Date 2022/11/24
 */
@Data
@NoArgsConstructor
public class CacheDataWrapper {

    //缓存内容
    private  Object data;
    //缓存过期时间
    private  long delay;
    //缓存过期时间单位
    private  TimeUnit unit;

    public CacheDataWrapper(Object data, long delay, TimeUnit unit) {
        this.data = data;
        this.delay = delay;
        this.unit = unit;
    }
}
