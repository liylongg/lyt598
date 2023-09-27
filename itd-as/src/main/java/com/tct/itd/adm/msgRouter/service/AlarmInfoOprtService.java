package com.tct.itd.adm.msgRouter.service;

import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.entity.AdmAlertInfo;
import com.tct.itd.adm.service.AdmAlertDetailBoxService;
import com.tct.itd.adm.service.AdmAlertDetailService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @ClassName AlarmInfoInsertImpl
 * @Description 故障信息入库
 * @Author YHF
 * @Date 2021/4/29 14:24
 */
@Slf4j
@Service
public class AlarmInfoOprtService {

    @Resource
    private AdmAlertDetailBoxService admAlertDetailBoxService;

    @Resource
    private AdmAlertInfoService admAlertInfoService;

    @Resource
    private AdmAlertDetailService admAlertDetailService;

    /**
     * @param id
     * @param type
     * @param location
     * @param message
     * @param status
     * @param alarmTime
     * @return void
     * @Description 录入故障信息
     * @Date 10:28 2021/1/12
     **/

    public void insertAdmAlertInfo(Long id, String type, String typeDetail, String location, String message, String status, String alarmTime, int allowFailover, String source) {
        AdmAlertInfo admAlertInfo = new AdmAlertInfo();
        admAlertInfo.setId(id);
        admAlertInfo.setType(type);
        admAlertInfo.setTypeDetail(typeDetail);
        admAlertInfo.setLocation(location);
        admAlertInfo.setMessage(message);
        //admAlertInfo.setSource("TCMS");
        admAlertInfo.setSource(source);
        admAlertInfo.setStatus(status);
        admAlertInfo.setTime(DateUtil.getStringToDate(alarmTime, "yyyy-MM-dd HH:mm:ss"));
        //是否允许故障恢复
        admAlertInfo.setAllowFailover(allowFailover);
        //生命周期状态为true
        admAlertInfo.setEndLife(true);
        //插入数据库
        admAlertInfoService.insert(admAlertInfo);
        //录入故障，设置生命周期为true时，存入redis
        BasicCommonCacheUtils.set(Cache.END_LIFE, id);
    }

    /**
     * @param id
     * @param infoId
     * @param message
     * @param tag
     * @param title
     * @return void
     * @Description 插入告警详细信息
     * @Date 10:32 2021/1/12
     **/
    public void insertAdmAlertDetail(Long id, Long infoId, String message, String tag, String title) {
        AdmAlertDetail alertDetail = new AdmAlertDetail();
        alertDetail.setId(id);
        alertDetail.setInfoId(infoId);
        alertDetail.setMessage(message);
        alertDetail.setTag(tag);
        alertDetail.setTime(new Date());
        alertDetail.setTitle(title);
        alertDetail.setTs(new Date());
        //插入数据库
        admAlertDetailService.insert(alertDetail);
    }

    /**
     * @param id
     * @param infoId
     * @param message
     * @param tag
     * @param title
     * @return void
     * @Description 插入告警详细信息
     * @Date 10:32 2021/6/2
     **/
    public void insertAdmAlertDetail(Long id, Long infoId, String message, String tag, String title, String alarmTime) {
        AdmAlertDetail alertDetail = new AdmAlertDetail();
        alertDetail.setId(id);
        alertDetail.setInfoId(infoId);
        alertDetail.setMessage(message);
        alertDetail.setTag(tag);
        alertDetail.setTime(DateUtil.getStringToDate(alarmTime, "yyyy-MM-dd HH:mm:ss"));
        alertDetail.setTitle(title);
        alertDetail.setTs(new Date());
        //插入数据库
        admAlertDetailService.insert(alertDetail);
    }


    public void insertAdmAlertDetail(Long id, Long infoId, String message, String tag, String title, Integer buttonStatus) {
        AdmAlertDetail alertDetail = new AdmAlertDetail();
        alertDetail.setId(id);
        alertDetail.setInfoId(infoId);
        alertDetail.setMessage(message);
        alertDetail.setTag(tag);
        alertDetail.setTime(new Date());
        alertDetail.setTitle(title);
        alertDetail.setTs(new Date());
        alertDetail.setButtonStatus(1);
        //插入数据库
        admAlertDetailService.insert(alertDetail);
    }

    /**
     * @param id
     * @param detailId
     * @param status
     * @param message
     * @return void
     * @Description 插入推荐指令详情
     * @Date 10:37 2021/1/12
     **/

    public void insertAdmAlertDetailBox(Long id, Long detailId, String status, String message) {
        //推荐指令入库
        AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox();
        alertDetailBox.setId(id);
        alertDetailBox.setDetailId(detailId);
        alertDetailBox.setStatus(status);
        alertDetailBox.setMessage(message);
        //插入数据库
        admAlertDetailBoxService.insert(alertDetailBox);
    }

    public void updateStatus(Long infoId, Long boxId, int tag) {
        String message1 = "";
        String message2 = "";
        if (tag == 0) {
            message1 = "已放弃";
            message2 = "已放弃";
            admAlertInfoService.updateStatusById(infoId, message2);
            admAlertDetailBoxService.updateStatusById(boxId, message1);
        } else if (tag == 1) {
            message1 = "已执行";
            message2 = "已执行";
            admAlertInfoService.updateStatusById(infoId, message2);
            admAlertDetailBoxService.updateStatusById(boxId, message1);
        } else if (tag == 2) {
            message1 = "超时已放弃";
            message2 = "超时已放弃";
            admAlertInfoService.updateStatusById(infoId, message2);
            admAlertDetailBoxService.updateStatusById(boxId, message1);
        }
    }

    public void failureRecovery(Long infoId) {
        admAlertInfoService.updateStatusById(infoId, "故障已恢复");
    }
}
