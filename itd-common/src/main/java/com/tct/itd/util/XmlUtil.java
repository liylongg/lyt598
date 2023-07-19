package com.tct.itd.util;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;


public class XmlUtil {

    /**
     * 读取指定路径的xml文件转换为Java对象
     *
     * @param xmlPath
     * @param load
     * @return
     */
    public static Object xmlToBean(String xmlPath, Class<?> load) {
        JAXBContext context = null;
        Object object = null;
        try {
            context = JAXBContext.newInstance(load);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            object = unmarshaller.unmarshal(new File(xmlPath));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     * java对象转换为XML字符串
     *
     * @param obj    Java对象
     * @param load   Java对象类
     * @param coding 编码
     * @return
     * @throws JAXBException
     */
    public static String beanToXml(Object obj, Class<?> load, String coding) {
        JAXBContext context = null;
        StringWriter writer = null;
        try {
            context = JAXBContext.newInstance(load);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, coding);
            writer = new StringWriter();
            marshaller.marshal(obj, writer);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return writer != null ? writer.toString() : null;
    }

    /**
     * java对象转换为XML字符串,并上传到指定路径
     *
     * @param obj     Java对象
     * @param load    Java对象类
     * @param coding  编码
     * @param xmlPath 存放地址
     */
    public static void uploadByBeanToXml(Object obj, Class<?> load, String coding, String xmlPath) {
        String xmlString = beanToXml(obj, load, coding);
        System.out.println(xmlString);
        //写入到xml文件中
        BufferedWriter bfw = null;
        try {
            bfw = new BufferedWriter(new FileWriter(new File(xmlPath)));
            bfw.write(xmlString);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bfw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
