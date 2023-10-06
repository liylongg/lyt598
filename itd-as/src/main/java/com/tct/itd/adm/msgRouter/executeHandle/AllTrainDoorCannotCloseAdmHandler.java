package com.tct.itd.adm.msgRouter.executeHandle;

import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.executeHandle.failureRecovery.AllTrainDoorCannotCloseAdmFailureRecoveryHandler;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.*;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.dto.Info;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @Description 全列车门无法关闭推荐指令
 * @Author yhf
 * @Date 2021/5/17 22:20
 **/

@Slf4j
@Component
public class AllTrainDoorCannotCloseAdmHandler implements AuxiliaryDecisionHandler {
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AdmAlertDetailService admAlertDetailService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AdmAlertDetailBoxService admAlertDetailBoxService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;
    @Resource
    private AllTrainDoorCannotCloseAdmFailureRecoveryHandler allTrainDoorCannotCloseAdmFailureRecoveryHandler;

    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        int executeStep = auxiliaryDecision.getExecuteStep();
        log.info("执行扣车推荐指令:{}", auxiliaryDecision);
        if( executeStep == 1){
            doOffHoldADM(auxiliaryDecision);
        } else if (executeStep == 2) {
            doGraphADM(auxiliaryDecision);
        } else if (executeStep == 3) {
            doThreeADM(auxiliaryDecision);
        } else if (executeStep == 4) {
            doFourADM(auxiliaryDecision);
        } else if (executeStep == 5) {
            doFiveADM(auxiliaryDecision);
        } else if (executeStep == 6) {
            doSixADM(auxiliaryDecision);
        } else if (executeStep == 7) {
            doSevenADM(auxiliaryDecision);
        } else if (executeStep == 8) {
            doEightADM(auxiliaryDecision);
        } else if (executeStep < 0) {
            allTrainDoorCannotCloseAdmFailureRecoveryHandler.handle(auxiliaryDecision);
        } else {
            log.error("推荐指令命令步骤有误,请查看数据是否正确--{}", JsonUtils.toJSONString(auxiliaryDecision));
            throw new RuntimeException("推荐指令命令步骤有误,请查看数据是否正确");
        }
    }

    /**
     * @Author yuelei
     * @Desc 执行第八次推荐指令
     * @Date 22:26 2022/6/21
     */
    private void doEightADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),1);
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(),"系统产生运行图方案","2","运行图方案选择");
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
    }

    /**
     * @Author yuelei
     * @Desc   执行第7次推荐指令
     * @Date 22:24 2022/6/21
     */
    private void doSevenADM(AuxiliaryDecision auxiliaryDecision){
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),1);
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(),"系统产生运行图方案","2","运行图方案选择");
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(36);
            add(37);
            add(38);
            add(39);
        }});
    }

    private void doSixADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),1);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(32);
            add(33);
            add(34);
        }});
    }

    /**
     * @Author yuelei
     * @Desc  执行第五次推荐指令
     * @Date 20:31 2022/6/20
     */
    private void doFiveADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(),"系统产生运行图方案","2","运行图方案选择");
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),1);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        //执行完第二次中断调图，添加缓存记录重复检测开始时间
        log.info("添加缓存记录重复检测开始时间");
        BasicCommonCacheUtils.set(Cache.DOOR_TIME_ADJUST_CASE_FLAG, System.currentTimeMillis());
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(28);
            add(29);
            add(30);
        }});
    }



    /**
     * @Author yuelei
     * @Desc 执行第四次推荐指令
     * @Date 19:25 2022/6/20
     */
    private void doFourADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(),1);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(24);
            add(25);
            add(26);
        }});
        //执行完第二次推荐指令，马上推送第三次推荐指令
        this.sendFiveAid(alarmInfo);
    }

    private void sendFiveAid(AlarmInfo alarmInfo){
        log.info("车门故障 - 单车门无法打开 生成第二次推荐指令提示:{}", JsonUtils.toJSONString(alarmInfo));
        //车门故障告警超时 继续执行超时推荐指令
        long detailId = UidGeneratorUtils.getUID();
        AdmAlertDetail alertDetail3 = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第三次推荐指令", new Date(), "系统产生运行图推荐指令", "1", 1, System.currentTimeMillis());
        admAlertDetailService.insert(alertDetail3);
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), 5,  0);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        long boxId = UidGeneratorUtils.getUID();
        //推荐指令步骤执行第二步
        alarmInfo.setExecuteStep(5);
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        //推荐指令入库
        AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
        admAlertDetailBoxService.insert(alertDetailBox);
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmInfo.setTableBoxId2(boxId);
        //设置状态未执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
        admAlertInfoSubService.updateById(alarmInfo);
        log.info("车门故障 - 全列车门无法关闭,生成第三次推荐指令提示成功");
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(27);
        }});
    }



    /**
     * @Author yuelei
     * @Desc 执行第三次推荐指令
     * @Date 18:07 2022/6/20
     */
    private void doThreeADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        //获取告警信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        log.info("执行三次推荐指令,alarmInfo-" + alarmInfo);
        //更新方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(),alarmInfo.getTableBoxId2(),1);
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新子表状态为已执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
        alarmInfo.setEndLife(false);
        admAlertInfoSubService.updateById(alarmInfo);
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(18);
            add(19);
            add(20);
            add(21);
            add(22);
        }});
    }

    /**
     * @Author yuelei
     * @Desc  全列车门无法关闭执行第二次推荐指令
     * @Date 18:06 2022/6/20
     */
    public void doGraphADM(AuxiliaryDecision auxiliaryDecision) {
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(auxiliaryDecision.getTableInfoId());
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        log.info("执行全列车门无法关闭故障第二次推荐指令");
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId2(), 1);
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(),"系统产生运行图方案","2","运行图方案选择");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(alarmInfo.getTableInfoId(), AlarmInfoEnum.EXECUTE_END_1.getCode());
        //自动获取ATS自动清客状态消失后，触发第3次推荐指令。
        BasicCommonCacheUtils.set(Cache.CLEAR_PEOPLE_IS_FINISH, alarmInfo);
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(12);
            add(13);
            add(14);
            add(15);
        }});
    }

   /**
    * @Author yuelei
    * @Desc 执行第一次推荐指令
    * @Date 10:46 2022/6/21
    */
    public void doOffHoldADM(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 1);
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
        //存入自动检测故障是否恢复缓存
        BasicCommonCacheUtils.set(Cache.TRAIN_DOOR_AUTO_CHECK_RECOVERY, alarmInfo.getTrainId());
        //更新流程图
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(3);
            add(4);
            add(5);
            add(7);
        }});
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE;
    }
}
