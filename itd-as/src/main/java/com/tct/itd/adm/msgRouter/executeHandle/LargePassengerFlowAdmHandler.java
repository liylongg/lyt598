package com.tct.itd.adm.msgRouter.executeHandle;


import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.handle.LargePassengerFlowHandler;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.adm.util.DisCmdSendUtils;
import com.tct.itd.client.SpareCarClient;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.DepotInfoDto;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.SpringContextUtil;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;


/**
 * @Description 执行大客流推荐指令
 * @Author zhoukun
 * @Date 2022/7/4 17:32
 */
@Slf4j
@Component
public class LargePassengerFlowAdmHandler implements AuxiliaryDecisionHandler {

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private DisCmdSendUtils disCmdSendUtils;
    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private LargePassengerFlowHandler largePassengerFlowHandler;
    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;
    @Resource
    private SpareCarClient depotDataClient;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        int executeStep = auxiliaryDecision.getExecuteStep();
        switch(executeStep){
            case 1:
                log.info("大客流：执行第一次推荐指令:{}", auxiliaryDecision);
                doAidDecisionOne(auxiliaryDecision);
                break;
            case 2:
                log.info("大客流：执行第二次推荐指令:{}", auxiliaryDecision);
                doAidDecisionTwo(auxiliaryDecision);
                break;
            case 3:
                log.info("大客流：执行第三次推荐指令:{}", auxiliaryDecision);
                doAidDecisionThree(auxiliaryDecision);
                break;
            default:
                log.error("推荐指令命令步骤有误,请查看数据是否正确--{}", JsonUtils.toJSONString(auxiliaryDecision));
                throw new RuntimeException("推荐指令命令步骤有误,请查看数据是否正确");
        }
    }

    private void doAidDecisionOne(AuxiliaryDecision auxiliaryDecision) {
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        if (alarmInfo.getLargePassFlowCrowdLevel()==NumConstant.ONE ){
            alarmFlowchartService.setExecuteFlag(alarmInfo,8);
        }
        if (alarmInfo.getLargePassFlowCrowdLevel()==NumConstant.TWO){
            alarmFlowchartService.setExecuteFlag(alarmInfo,9);
        }
        if (alarmInfo.getLargePassFlowCrowdLevel()==NumConstant.THREE){
            alarmFlowchartService.setExecuteFlag(alarmInfo,10);
        }
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");

        log.info("{}执行推荐指令,alarmInfo:{}", AdmAlertDetailTypeService.getDescribeByCode(alarmInfo.getAlarmTypeDetail()), JsonUtils.toJSONString(alarmInfo));
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), NumConstant.ONE);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(auxiliaryDecision.getTableInfoId(), AlarmInfoEnum.EXECUTE_END_1.getCode());
        //只要不是三级客流，直接生命周期
        if (NumConstant.THREE !=alarmInfo.getLargePassFlowCrowdLevel()){
            if (alarmInfo.getLargePassFlowCrowdLevel()==NumConstant.ONE){
                alarmFlowchartService.setExecuteFlag(alarmInfo,17);
            }
            if (alarmInfo.getLargePassFlowCrowdLevel()==NumConstant.TWO){
                alarmFlowchartService.setExecuteFlag(alarmInfo,18);
            }
            alarmInfo.setEndLife(false);
        }
        //客流等级为3级时，推送第二次推荐指令
        if (NumConstant.THREE ==alarmInfo.getLargePassFlowCrowdLevel()){
            largePassengerFlowHandler.pushSecondAdm(alarmInfo);
        }
        admAlertInfoSubService.updateById(alarmInfo);

    }

    private void doAidDecisionTwo(AuxiliaryDecision auxiliaryDecision) {
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        log.info("{}执行推荐指令,alarmInfo:{}", AdmAlertDetailTypeService.getDescribeByCode(alarmInfo.getAlarmTypeDetail()), JsonUtils.toJSONString(alarmInfo));
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), NumConstant.ONE);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(auxiliaryDecision.getTableInfoId(), AlarmInfoEnum.EXECUTE_END_1.getCode());
        largePassengerFlowHandler.pushThirdAdm(alarmInfo);
    }

    private void doAidDecisionThree(AuxiliaryDecision auxiliaryDecision) {
        //查询ATS数据库中车辆段的所有备车信息
        BaseResponse<DepotInfoDto> baseResponse = depotDataClient.getSpareCar();
        if(!baseResponse.getSuccess()){
            throw new BizException(CodeEnum.GET_BACK_CAR_FAIL);
        }
        DepotInfoDto depotInfo = baseResponse.getData();
        ConcurrentMap<String, TiasTraceInfo> map = trainTraceCache.asMap();
        if (CollectionUtils.isEmpty(map)) {
            throw new BizException("车次追踪数据不存在！");
        }
        List<TiasTraceInfo> tiastraceInfoList = new ArrayList<>(map.values());
        tiastraceInfoList.forEach(tiasTraceInfo -> tiasTraceInfo.setOrderNumber(AlarmUtil.getOrderNum(tiasTraceInfo.getOrderNumber())));
        //处理备车信息，筛选出处于车辆段或者停车场被激活的车次
        List<DepotInfoDto> spareCar = aidDecisionExecService.getSpareCar(depotInfo, tiastraceInfoList);
        int num=0;
        for (DepotInfoDto s : spareCar) {
            if (!ObjectUtils.isEmpty(s.getSpareTrainInfoList())){
                num=1;
            }
        }
        if (num==0){
            throw new BizException(CodeEnum.END_LIFE.getCode(),"无备车信息");
        }
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        log.info("{}执行推荐指令,alarmInfo:{}",  AdmAlertDetailTypeService.getDescribeByCode(alarmInfo.getAlarmTypeDetail()), JsonUtils.toJSONString(alarmInfo));
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), NumConstant.ONE);
        //调用算法调整运行图
        log.info("准备调整运行图");
        //执行第一次推荐指令后，插入第四条告警信息 运行图预览信息
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(), alarmInfo.getTableInfoId(), "系统产生运行图方案", "2", "运行图方案选择");
        //推送运行图方案列表
        AidDecSubStepHandler sendGraphCaseHandler = (AidDecSubStepHandler) SpringContextUtil.getBean("sendGraphCaseForLargePassengerFlow");
        sendGraphCaseHandler.handle(alarmInfo, null);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(auxiliaryDecision.getTableInfoId(), AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmFlowchartService.setExecuteFlag(alarmInfo,13);
        alarmFlowchartService.setExecuteFlag(alarmInfo,14);
        alarmFlowchartService.setExecuteFlag(alarmInfo,15);
    }


    @Override
    public String channel() {
        return AlarmTypeConstant.LARGE_PASSENGER_FLOW;
    }
}
