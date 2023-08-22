package com.tct.itd;

import com.tct.itd.common.map.AlarmTypeHashMapCache;
import com.tct.itd.common.xml.alarm.type.AlarmTypeXmlRoot;
import com.tct.itd.utils.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;

@Slf4j
public class ItdApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        //在bean容器启动前初始化故障类型模板参数
        this.initAlarmType(event);
    }

    /**
     * 初始化获取故障类型模板参数
     * @param event 系统配置参数对象
     */
    public void initAlarmType(ApplicationEnvironmentPreparedEvent event) {
        String alarmTypeXmlPath = event.getEnvironment().getProperty("iids.ini.alarmType");
        log.info("获取配置文件路径地址:{}",alarmTypeXmlPath);
        AlarmTypeXmlRoot alarmTypeXmlRoot = new AlarmTypeXmlRoot();
        //1.读取xml文件转换为字符串
        StringWriter sw = new StringWriter();
        try {
            //读取xml
            XmlUtil.readXMLToStrByPath(alarmTypeXmlPath, sw);
            //2.将xml字符串转换为对象
            alarmTypeXmlRoot = (AlarmTypeXmlRoot) XmlUtil.xmlStrToObject(AlarmTypeXmlRoot.class, sw.toString());
        } catch (Exception e) {
            log.error("读取故障类型模板配置xml失败", e);
        } finally {
            try {
                sw.close();
            } catch (IOException e) {
                log.error("关流异常:", e);
            }
        }
        if (Objects.isNull(alarmTypeXmlRoot)) {
            log.error("读取故障类型模板配置xml为空");
        }
        AlarmTypeHashMapCache.setAlarmTypeXmlRoot(alarmTypeXmlRoot);
    }
}
