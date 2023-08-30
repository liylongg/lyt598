package com.tct.itd.adm.controller.alert;


import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.service.AdmAlertDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author LYH
 * @since 2020-11-16
 */
@Slf4j
@RestController
@RequestMapping("/alert_detail")
public class AdmAlertDetailController {
    /**
     * 服务对象
     */
    @Resource
    private AdmAlertDetailService admAlertDetailService;

    /**
     * @Description 插入单条detail数据
     * @Date 11:09 2020/11/18
     * @param entity
     * @return int
    **/
    @PostMapping("/insert")
    public int insert(@RequestBody AdmAlertDetail entity) {
        log.info("***************AdmAlertDetailController:" + entity.toString());
        return admAlertDetailService.insert(entity);
    }

    /**
     * 根据infoId查询所有符合条件的AdmAlertDetail对象，并增加预览运行图信息
     * @param infoId
     * @return 符合条件的对象集合
     * @date 2020-11-12 15:04:38
     */
    @GetMapping("/select_list_by_infoId")
    public List<AdmAlertDetail> selectListByInfoId(@RequestParam("infoId") Long infoId) {
        return admAlertDetailService.selectListByInfoId(infoId);
    }
}

