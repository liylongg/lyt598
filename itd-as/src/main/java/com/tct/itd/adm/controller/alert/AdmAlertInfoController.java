package com.tct.itd.adm.controller.alert;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertInfo;
import com.tct.itd.adm.msgRouter.executeHandle.WaiveAidDecisionAdmHandler;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.RedisKit;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.dto.AdmAlertInfoDto;
import com.tct.itd.common.dto.NotifyParam;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.excel.AlarmInfoExcel;
import com.tct.itd.tias.service.SendNotifyService;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author LYH
 * @since 2020-11-16
 */
@Slf4j
@RestController
@RequestMapping("/alert_info")
public class AdmAlertInfoController {
    @Resource
    private AdmAlertInfoService admAlertInfoService;
    @Resource
    private AdmAlertDetailService admAlertDetailService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private WaiveAidDecisionAdmHandler waiveAidDecisionAdmHandler;
    @Resource
    private AppPushService appPushService;
    @Resource
    private SendNotifyService sendNotifyService;

    /**
     * @param entity
     * @return int
     * @Description
     * @Date 11:07 2020/11/18
     **/
    @PostMapping("/insert")
    public int insert(@RequestBody AdmAlertInfo entity) {
        log.info("***************AdmAlertDetailController:" + entity.toString());
        return admAlertInfoService.insert(entity);
    }

    /**
     * @param
     * @return java.util.ArrayList<com.tct.iids.entity.AdmAlertInfo>
     * @Description 查询info表所有的数据
     * @Date 14:45 2020/11/18
     **/
    @GetMapping("/select_all")
    public List<AdmAlertInfo> selectAll() {
        return admAlertInfoService.selectList(null);
    }

    /**
     * @param params
     * @return java.util.List<com.tct.model.vo.flatAdm.AdmAlertInfo>
     * @Description 根据不同的字段组合条件查询对应的info表信息，目前不进行分页
     * @Date 14:08 2020/11/24
     **/
    @GetMapping("/select_by_params")
    public Page<AdmAlertInfoDto> selectByParams(@RequestParam Map<String, Object> params) {
        return admAlertInfoService.selectAll(params);
    }


    /**
     * @Author yuelei
     * @Desc 获取最新的一条数据
     * @Date 17:19 2023/4/3
     */
    @GetMapping("/select_one_latest")
    public Long selectByParams() {
        return admAlertInfoService.selectOneLatest();
    }

    /**
     * @Author yuelei
     * @Desc 根据ID获取应急事件
     * @Date 11:27 2023/4/4
     */
    @GetMapping("/select_by_id")
    public AdmAlertInfoDto getInfoById(@RequestParam("infoId") Long infoId){
        return admAlertInfoService.getInfoById(infoId);
    }

    /**
     * @param status
     * @param alertDetail
     * @return void
     * @Description 修改info表的状态信息，并向detail表中新增一条代表状态信息的详情信息
     * @Date 14:32 2021/1/12
     **/
    @PostMapping("/update_status_add_detail")
    public void updateStatusAndAddDetail(@RequestParam("infoId") Long infoId, @RequestParam("status") String status,
                                         @RequestBody AdmAlertDetail alertDetail) {
        admAlertInfoService.updateStatusById(infoId, status);
        admAlertDetailService.insert(alertDetail);
    }

    @PostMapping("/give_up")
    public Integer giveUp(@RequestBody AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AdmAlertInfo admAlertInfo = admAlertInfoService.selectById(infoId);
        //流程已经结束
        if (Objects.isNull(admAlertInfo) || !admAlertInfo.getEndLife()) {
            //刷新前台页面
            appPushService.sendWebNoticeMessageToAny(
                    new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
            return 1;
        }
        //执行放弃
        try {
            waiveAidDecisionAdmHandler.handle(auxiliaryDecision);
        }catch (Exception e){
            //更新主表状态生命周期为结束
            admAlertInfoService.giveUp(infoId);
            //设置生命周期置为false时，删除key；
            RedisKit.endLifeDeleteRedis();
            //故障放弃，车站存在弹窗，关闭车站弹窗
            waiveAidDecisionAdmHandler.closeStationWin(infoId);
        }
        //插入日志放弃推荐指令
        NotifyParam notifyParam = new NotifyParam();
        notifyParam.setInfoId(infoId);
        notifyParam.setMsgPushEnum(MsgPushEnum.EXECUTE_AID_LOG_MSG);
        notifyParam.setType(0);
        notifyParam.setMsg("用户操作放弃应急事件");
        sendNotifyService.sendNotify(notifyParam);
        return 0;
    }

    @GetMapping("/export")
    public List<AlarmInfoExcel> exportAlertInfo(@RequestParam Map<String, Object> params) {
        log.info("开始导出应急事件列表参数:{}", JsonUtils.toJSONString(params));
        return admAlertInfoService.getExportList(params);
    }
}

