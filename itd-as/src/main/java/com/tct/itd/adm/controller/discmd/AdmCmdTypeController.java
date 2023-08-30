package com.tct.itd.adm.controller.discmd;

import com.tct.itd.adm.service.AdmCmdTypeService;
import com.tct.itd.common.dto.AdmCmdTypeDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * (AdmCommandType)表控制层
 * @author yl
 * @version 1.0
 * @date 2020-11-12 15:04:38
 */
@RestController
@RequestMapping("adm_cmd_type")
public class AdmCmdTypeController {
    /**
     * 服务对象
     */
    @Resource
    private AdmCmdTypeService admCmdTypeService;

    /**
     * @Description 根据父类型查询所有子类型
     * @Author yuelei
     * @Date 2021/11/1 14:46
     * @param parentCode
     * @return com.tct.iids.restful.BaseResponse<java.util.List<com.tct.iids.dto.AdmCmdTypeDto>>
     */
    @GetMapping("/list/{parentCode}")
    public List<AdmCmdTypeDto> listByParentCode(@PathVariable(value = "parentCode") String parentCode) {
        return admCmdTypeService.listByParentCode(parentCode);
    }

    /**
     * @Description 查询树形结构
     * @Author kangyi
     * @Date 2022/7/18 14:46
     * @return com.tct.iids.restful.BaseResponse<java.util.List<com.tct.iids.dto.AdmCmdTypeDto>>
     */
    @GetMapping("/getAll")
    public List<AdmCmdTypeDto> getAll() {
        return admCmdTypeService.getAll();
    }

}