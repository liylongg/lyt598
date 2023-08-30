package com.tct.itd.adm.controller.adm;

import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.common.dto.AdmAlertChildTypeDto;
import com.tct.itd.common.dto.AdmAlertTypeChildDto;
import com.tct.itd.common.dto.AdmAlertTypeDto;
import com.tct.itd.dto.AlarmInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description 故障类型信息
 * @Author yuelei
 * @Date 2021/9/1 15:21
 */
@Slf4j
@RestController
@RequestMapping("alert_detail_type")
public class AdmAlertDetailTypeController {

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    /**
     * @Description 获取所有故障类型信息
     * @Author yuelei
     * @Date 2021/9/13 21:20
     * @param
     * @return com.tct.iids.restful.BaseResponse<java.util.List<com.tct.iids.dto.AdmAlertTypeDto>>
     */
    @GetMapping("/list/{parentCode}")
    public List<AdmAlertTypeDto> changeGraph(@PathVariable(value = "parentCode") String parentCode) {
        return admAlertDetailTypeService.list(parentCode);
    }

    /**
     * @Description 获取所有故障类型信息
     * @Author yuelei
     * @Date 2021/9/13 21:20
     * @param
     * @return com.tct.iids.restful.BaseResponse<java.util.List<com.tct.iids.dto.AdmAlertTypeDto>>
     */
    @GetMapping("/query/{parentCode}")
    public List<AdmAlertTypeDto> query(@PathVariable(value = "parentCode") String parentCode) {
        return admAlertDetailTypeService.query(parentCode);
    }

    /**
     * @Description 获取所有子类型
     * @Author yuelei
     * @Date 2021/10/27 14:45
     * @return com.tct.iids.restful.BaseResponse<java.util.List<com.tct.iids.dto.AdmAlertChildTypeDto>>
     */
    @GetMapping("/list/child")
    public List<AdmAlertChildTypeDto> childList() {
        return admAlertDetailTypeService.childList();
    }

    /**
     * @Description 故障类型组合大类和小类
     * @Author yuelei
     * @Date 2021/11/1 16:15
     * @return com.tct.iids.restful.BaseResponse<java.util.List<com.tct.iids.dto.AdmAlertTypeDto>>
     */
    @GetMapping("/list/concat")
    public List<AdmAlertTypeDto> listConcatType() {
        return admAlertDetailTypeService.listConcatType();
    }

    /**
     * @Description 根据故障子类型获取描述
     * @Author yuelei
     * @Date 2021/11/11 11:24
     * @param alarmInfo
     * @return com.tct.iids.restful.BaseResponse<java.lang.String>
     */
    @PostMapping("/desc/code")
    public String getDescByCode(@RequestBody AlarmInfo alarmInfo) {
        return admAlertDetailTypeService.getDescByCode(alarmInfo.getAlarmTypeDetail());
    }


    /**
     * @description 获取故障录入所有类型
     * @date 2022/7/8 11:36
     * @author kangyi
     * @param 
     * @return: void
     */ 
    @PostMapping("/getAll/{type}")
    public List<AdmAlertTypeChildDto> getAll(@PathVariable(value = "type") int type){
        return admAlertDetailTypeService.getAll(type);
    }
}
