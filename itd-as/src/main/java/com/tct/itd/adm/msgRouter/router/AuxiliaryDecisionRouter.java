package com.tct.itd.adm.msgRouter.router;

import com.tct.itd.adm.annotation.ExecAidNotifyLog;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailService;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.ExecuteAidDecUtil;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*

 */
@Component
@Slf4j
public class AuxiliaryDecisionRouter {

    @Resource
    private List<AuxiliaryDecisionHandler> auxiliaryDecisionHandlerList;

    @Resource
    private AppPushService appPushService;

    @Resource
    private AdmAlertDetailService admAlertDetailService;

    @Resource
    private AdmAlertInfoService admAlertInfoService;

    private Map<String, AuxiliaryDecisionHandler> auxiliaryDecisionHandlerMap = new HashMap<>();

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    /**
     * @Description: 建立映射关系
     * @Author: zhangyinglong
     * @Date:2021/5/18 11:26
     * @return: void
     */
    @PostConstruct
    public void init() {
        //推荐指令方案执行路由策略
        auxiliaryDecisionHandlerList.forEach(auxiliaryDecisionHandler -> {
            auxiliaryDecisionHandlerMap.put(auxiliaryDecisionHandler.channel(), auxiliaryDecisionHandler);
        });
    }

    /**
     * @Description: 故障消息处理
     * @Author: zhangyinglong
     * @Date:2021/5/18 11:27
     * @param: auxiliaryDecision
     * @return: void
     */
    @ExecAidNotifyLog
    public void auxiliaryRouter(AuxiliaryDecision auxiliaryDecision) {
        long start = System.currentTimeMillis();
        int executeStep = auxiliaryDecision.getExecuteStep();
        //不排除全列车门故障推荐指令 全列放弃也和其他一致
        if (executeStep == 0) {
            //放弃推荐指令
            String channel = String.valueOf(executeStep);
            //公共放弃方法WaiveAidDecisionAdmHandler.java
            AuxiliaryDecisionHandler auxiliaryDecisionHandler = auxiliaryDecisionHandlerMap.get(channel);
            this.handle(auxiliaryDecision, auxiliaryDecisionHandler);
            return;
        }
        String channel = String.valueOf(auxiliaryDecision.getAlarmTypeDetail());
        AuxiliaryDecisionHandler auxiliaryDecisionHandler = auxiliaryDecisionHandlerMap.get(channel);
        if (auxiliaryDecisionHandler == null) {
            throw new BizException(CodeEnum.ALARM_TYPE_UNHANDLED);
        }
        this.handle(auxiliaryDecision, auxiliaryDecisionHandler);
        long spend = (System.currentTimeMillis() - start) / 1000;
        log.info("{} handler exec success, spend:{}s", auxiliaryDecision, spend);
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        //只有车门故障才有故障恢复
        //故障恢复与预览图一开始直接禁用置灰，第一次推荐指令执行后，可以使用；
        //在第二次推荐指令推送时再次禁用 位置： AlarmCloseDoorTimer
        //如果auxiliaryDecision.getExecuteStep() = -3，表示放弃故障恢复推荐指令，需要置灰按钮，不走以下逻辑。
        if (IidsConstPool.EXECUTE_STEP_1 == alarmInfo.getExecuteStep() && auxiliaryDecision.getExecuteStep() != IidsConstPool.EXECUTE_STEP_0_99) {
            //执行第一次推荐指令后，将预览图按钮可用，且能执行
            log.info("预览图按钮可用");
            admAlertDetailService.updateButtonStatus(1, alarmInfo.getTableInfoId(), "2");
        }
        //当录入大客流故障且执行或者放弃第一次推荐指令时，推送给中心一个消息，来提醒前端进行刷新
        if (IidsConstPool.EXECUTE_STEP_1 == alarmInfo.getExecuteStep()
                && alarmInfo.getAlarmTypeDetail() == Integer.parseInt(AlarmTypeConstant.LARGE_PASSENGER_FLOW)) {
            log.debug("提醒前端进行Box刷新");
            //后面有推送前端简单信息的服务，code是208003，只用添加改变WebNoticeDto的noticeCode就行
            appPushService.sendWebNoticeMessageToAny(
                    new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
        }
    }

    /**
     * @param auxiliaryDecision
     * @param auxiliaryDecisionHandler
     * @return void
     * @Description 全局处理异常，
     * @Author yuelei
     * @Date 2021/12/30 15:48
     */
    public void handle(AuxiliaryDecision auxiliaryDecision, AuxiliaryDecisionHandler auxiliaryDecisionHandler) {
        try {
            auxiliaryDecisionHandler.handle(auxiliaryDecision);
        } catch (BizException e) {
            log.error("执行推荐指令出错:{}", e.getMessage(), e);
            ExecuteAidDecUtil.giveUp(auxiliaryDecision.getTableInfoId());
            throw new BizException(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("执行推荐指令出错:{}", e.getMessage(), e);
            ExecuteAidDecUtil.giveUp(auxiliaryDecision.getTableInfoId());
            throw new BizException(CodeEnum.EXEC_AID_ERROR);
        }
    }

}
