package com.tct.itd.adm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description 自定义日志注解-执行推荐指令
 * @Author yuelei
 * @Date 2022/2/9 17:47
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecAidNotifyLog {

}
