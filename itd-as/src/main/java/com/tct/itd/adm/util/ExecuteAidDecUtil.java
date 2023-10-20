package com.tct.itd.adm.util;

import com.tct.itd.adm.annotation.AutoWaiveNotifyLog;
import com.tct.itd.adm.msgRouter.executeHandle.WaiveAidDecisionAdmHandler;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @ClassName HoldTrainUtil
 * @Description 扣车相关工具类
 * @Author yl
 * @Date 2021/4/29 14:59
 */
@Slf4j
@Component
public class ExecuteAidDecUtil {
    /**
     * @Description 放弃推荐指令
     * @Author yuelei
     * @Date 2021/10/21 10:49
     * @return java.util.List<java.lang.Integer>
     */
    @AutoWaiveNotifyLog
    public static void giveUp(long tableInfoId){
        WaiveAidDecisionAdmHandler handler = SpringContextUtil.getBean(WaiveAidDecisionAdmHandler.class);
        AuxiliaryDecision auxiliaryDecision = new AuxiliaryDecision();
        auxiliaryDecision.setTableInfoId(tableInfoId);
        try{
            handler.handle(auxiliaryDecision);
        }catch (Exception e){
            log.info("应急事件放弃出错：{}{}", e, e.getMessage());
            //更新主表状态生命周期为结束
            AdmAlertInfoService admAlertInfoService = SpringContextUtil.getBean(AdmAlertInfoService.class);
            admAlertInfoService.giveUp(tableInfoId);
            //设置生命周期置为false时，删除key；
            RedisKit.endLifeDeleteRedis();
        }
    }

    /**
     * @Author yuelei
     * @Desc 放弃处于生命周期的应急事件
     * @Date 14:51 2022/9/26
     */
    public static void giveUp(){
        AdmAlertInfoSubService admAlertInfoSubService = SpringContextUtil.getBean(AdmAlertInfoSubService.class);
        //放弃当前处于生命周期的应急事件
        AlarmInfo infoInLife = admAlertInfoSubService.getInfoInLife();
        if(!Objects.isNull(infoInLife)){
            AuxiliaryDecision auxiliaryDecision = new AuxiliaryDecision();
            auxiliaryDecision.setTableInfoId(infoInLife.getTableInfoId());
            WaiveAidDecisionAdmHandler handler = SpringContextUtil.getBean(WaiveAidDecisionAdmHandler.class);
            try{
                handler.handle(auxiliaryDecision);
            }catch (Exception e){
                log.info("应急事件放弃出错：{}{}", e, e.getMessage());
                //更新主表状态生命周期为结束
                AdmAlertInfoService admAlertInfoService = SpringContextUtil.getBean(AdmAlertInfoService.class);
                admAlertInfoService.giveUp(infoInLife.getTableInfoId());
                //设置生命周期置为false时，删除key；
                RedisKit.endLifeDeleteRedis();
            }
        }
    }
}
