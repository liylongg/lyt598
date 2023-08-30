package com.tct.itd.adm.controller.discmd;

import com.tct.itd.adm.service.AdmStationService;
import com.tct.itd.common.dto.AdmStation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * (TAdmStation)表控制层
 *
 * @author Liyuanpeng
 * @version 1.0
 * @date 2020-11-12 15:00:46
 */
@RestController
@RequestMapping("/adm_station")
public class AdmStationController {
    /**
     * 服务对象
     */
    @Resource
    private AdmStationService admStationService;

    /**
     * 查询车站名称计集合
     *
     * @param admStation 对象
     * @return 对象集合
     * @date 2020-11-12 15:00:46
     */
    @PostMapping("/selectList")
    public List<AdmStation> selectList(@RequestBody AdmStation admStation) {
        return admStationService.selectList(admStation);
    }

    /**
     * 查询车站/段/调/区间/中心/维护名称集合
     *
     * @param stationType -1:维护0:中心1:站 2:段 3:调 4:区间  查询多种类型用”,“分割
     * @return 对象集合
     * @date 2020-11-12 15:00:46
     */
    @GetMapping("/selectListByType")
    public List<AdmStation> selectListByType(@RequestParam(value = "stationType") String stationType) {
        return admStationService.selectListByType(stationType);
    }

    @GetMapping("/selectByStationId")
    public AdmStation selectByStationId(@RequestParam("stationId") Integer stationId) {
        return admStationService.selectByStationId(stationId);
    }

    @GetMapping("/selectAllByStationId")
    public List<AdmStation> selectAllByStationId(@RequestParam("stationId") Integer stationId) {
        return admStationService.selectAllByStationId(stationId);
    }



    /**
     * 随电子地图导入,更新该表的车站相关
     * 这里必须用@RequestParam, 否则会导致HttpMediaTypeNotSupportedException
     *
     * @param stationList
     * @return
     */
    @PostMapping("/insert/station/batch")
    public int insertStationBatch(@RequestParam("stationList") List<String> stationList) {
        return admStationService.insertStationBatch(stationList);
    }

    /**
     * @Description 获取区间对象
     * @Author kangyi
     * @Date 2022/5/16 15:16
     * @param upDown
     * @param upDownId
     * @return com.tct.itd.adm.dto.AdmStation
     */
    @PostMapping("/getSectionById")
    public AdmStation getSectionById(@RequestParam("upDown") int upDown, @RequestParam("upDownId") int upDownId) {
        return admStationService.getSectionById(upDown, upDownId);
    }


}