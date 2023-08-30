package com.tct.itd.adm.controller.alert;


import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.service.AdmAlertDetailBoxComService;
import com.tct.itd.adm.service.AdmAlertDetailBoxService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

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
@RequestMapping("/alert_detail_box")
public class AdmAlertDetailBoxController {
    @Resource
    private AdmAlertDetailBoxService admAlertDetailBoxService;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AdmAlertDetailBoxComService admAlertDetailBoxComService;

    /**
     * @Description 插入单条detail_box表数据
     * @Date 11:11 2020/11/18
     * @param entity
     * @return int
    **/
    @PostMapping("/insert")
    public int insert(@RequestBody AdmAlertDetailBox entity) {
        log.info("***************AdmAlertDetailBoxController:" + entity.toString());
        return admAlertDetailBoxService.insert(entity);
    }

    /**
     * @Description 根据点击相应详情按钮传回的detailId查询对应的详情框box对象
     * @Date 14:07 2020/11/23
     * @param detailId
     * @return Map
    **/
    @GetMapping("/select_one_by_detailId")
    public Map<String, Object> selectByDetailId(@RequestParam("detailId") Long detailId) {
        AdmAlertDetailBox admAlertDetailBox = admAlertDetailBoxService.selectByDetailId(detailId);
        Assert.notNull(admAlertDetailBox, "传入的detailId不正确,detailId:" + detailId);
        String message = admAlertDetailBox.getMessage();
        AdmIdea admIdea = JsonUtils.jsonToObject(message, AdmIdea.class);
        admIdea.setAlarmTypeStr(admAlertDetailTypeService.getDescription(admIdea.getAlarmType()));//设置故障类型文本信息
        //设置故障子类型文本信息
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        Map<String, Object> map = new HashMap<>();
        map.put("id", admAlertDetailBox.getId().toString());
        map.put("detailId", admAlertDetailBox.getDetailId().toString());
        map.put("message", admIdea);
        map.put("status", admAlertDetailBox.getStatus());
        return map;
    }

    /**
     * @Description 根据前端传回的点击信息更新info表和box表的状态信息
     * @Date 16:54 2020/11/18
     * @param infoId
     * @param tag
     * @return void
    **/
    @PostMapping("/update_status")
    public void updateStatus(@RequestParam("infoId") Long infoId,
                             @RequestParam("tag") int tag){
        admAlertDetailBoxComService.updateStatus(infoId, tag);
    }
}

