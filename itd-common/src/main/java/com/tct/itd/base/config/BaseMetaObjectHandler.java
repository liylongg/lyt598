package com.tct.itd.base.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @Description 基类自动填充
 * @Author zhaoke
 * @Date 2020-12-02 9:21
 */
@Slf4j
@Component
public class BaseMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("start insert fill ....");
        this.fillStrategy(metaObject, "createTime", new Date());
        this.fillStrategy(metaObject, "updateTime", new Date());
        this.fillStrategy(metaObject, "ts", new Date());
        this.fillStrategy(metaObject, "dr", 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("start update fill ....");
        this.fillStrategy(metaObject, "updateTime", new Date());
        this.fillStrategy(metaObject, "ts", new Date());
    }
}
