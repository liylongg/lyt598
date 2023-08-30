package com.tct.itd.adm.controller.alert;

import com.tct.itd.adm.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author kangyi
 * @description 上传
 * @date 2023年 02月13日 10:22:07
 */
@RestController
@RequestMapping("upload")
@Slf4j
public class UploadController {

    @Resource
    private UploadService uploadService;


    @RequestMapping(value = "/fileUpload")
    public ModelMap saveVideoAddress(@RequestParam("fileAddress") MultipartFile[] files, @RequestParam("uploadId") String uploadId) {
        log.info("收到上传的alarmInfo文件开始保存");
        ModelMap map = new ModelMap();
        if (StringUtils.isEmpty(uploadId)) {
            log.error("文件保存失败，未获取到保存故障信息的id");
            map.put("flag", "false");
            return map;
        }
        List<MultipartFile> fileList = Arrays.asList(files);
        if (CollectionUtils.isEmpty(fileList)) {
            log.error("文件保存失败，未获取到上传的文件");
            map.put("flag", "false");
            return map;
        }
        try {
            uploadService.saveFile(uploadId, fileList);
        } catch (Exception e) {
            log.error("文件保存失败", e);
            map.put("flag", "false");
            return map;
        }
        /*StringBuilder filePath = new StringBuilder(uploadAlarmInfo);
        filePath.append(uploadId);
        filePath.append("/");
        //创建保存文件夹
        File saveDic = new File(filePath.toString());
        if (!saveDic.exists()) {
            saveDic.mkdirs();
        }
        List<String> failFiles = new ArrayList<>();
        for (MultipartFile file : fileList) {
            String paFileName = file.getOriginalFilename();
            filePath.append(paFileName);
            try {
                File targetFile = new File(filePath.toString());
                boolean flag = true;
                if (targetFile.exists()) {
                    flag = targetFile.delete();
                }
                if (flag) {
                    file.transferTo(targetFile);
                }
            } catch (Exception e) {
                log.error("{}文件保存失败！", paFileName, e);
                failFiles.add(paFileName);
                continue;
            }
        }*/
        map.put("flag", "true");
        return map;
    }
}
