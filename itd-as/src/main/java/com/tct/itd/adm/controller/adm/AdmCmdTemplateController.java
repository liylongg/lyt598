package com.tct.itd.adm.controller.adm;

import com.tct.itd.adm.service.AdmCmdTemplateService;
import com.tct.itd.common.dto.AdmCmdTemplateDto;
import com.tct.itd.restful.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Description 故障类型模板
 * @Author yuelei
 * @Date 2021/9/1 15:21
 */
@Slf4j
@RestController
@RequestMapping("cmd_template")
public class AdmCmdTemplateController {

    @Resource
    private AdmCmdTemplateService admCmdTemplateService;

    /**
     * @Description 查询调度命令模板
     * @Author yuelei
     * @Date 2021/11/1 16:15
     * @return com.tct.iids.restful.BaseResponse<java.util.List<com.tct.iids.dto.AdmAlertTypeDto>>
     */
    @GetMapping("/one")
    public BaseResponse<String> getOne(AdmCmdTemplateDto dto) {
        return BaseResponse.success(admCmdTemplateService.getContent(dto));
    }

}
