package com.tct.itd.adm.util;

import com.tct.itd.common.dto.AlgorithmData;
import com.tct.itd.utils.HttpUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.JsonZip;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Description 模拟算法请求参数
 * @Author zhangyinglong
 * @Date 2021/6/8 19:56
 */
public class AlgParamMockUtils {

    private static AlgorithmData algorithmData;

    static {
//        Resource resource = new ClassPathResource("json/algtTestParam.json");
        try (InputStream resourceAsStream = AlgParamMockUtils.class.getClassLoader().getResourceAsStream("json/algtTestParam.json")){
            String jsonStr = IOUtils.toString(resourceAsStream, Charsets.UTF_8.toString());
            algorithmData = JsonUtils.jsonToObject(jsonStr, AlgorithmData.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static AlgorithmData getMockParamAlgorithmData(){
        return algorithmData;
    }

    public static void main(String[] args) {
        try (InputStream resourceAsStream = AlgParamMockUtils.class.getClassLoader().getResourceAsStream("json/algtTestParam.json")){
            String jsonStr = IOUtils.toString(resourceAsStream, Charsets.UTF_8.toString());
            String s = HttpUtil.sendPostRequest("http://172.18.39.165:5003/api/solver/train/getFollowingTrains", jsonStr);
            //压缩运行图
            System.out.println(s);
            String zipGraph = JsonZip.zipString(JsonUtils.toJSONString(s));
            System.out.println(zipGraph);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
