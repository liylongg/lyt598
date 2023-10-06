package com.tct.itd.adm.msgRouter.executeHandle;

import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepRouter;
import com.tct.itd.adm.msgRouter.router.AuxiliaryDecisionHandler;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.basedata.dfsread.service.handle.DoorInfoService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Objects;


/**
 * @Description 列车出站过程中站台门打开-推荐指令
 * @Author zyl
 * @Date 2021/5/17 22:20
 **/

@Slf4j
@Component
public class AllPlatformDoorOpenOutStationAdmHandler implements AuxiliaryDecisionHandler {

    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AidDecSubStepRouter aidDecSubStepRouter;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private DoorInfoService doorInfoService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;
    /**
     * @Author yuelei
     * @Desc 执行推荐指令
     * @Date 14:27 2022/8/8
     */
    @Override
    public void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException {
        int executeStep = auxiliaryDecision.getExecuteStep();
        log.info("执行扣车推荐指令:{}", auxiliaryDecision);
        switch (executeStep) {
            case 1:
                doOne(auxiliaryDecision);
                //更新流程图
                alarmFlowchartService.setExecuteFlags(auxiliaryDecision.getTableInfoId(), new ArrayList<Integer>() {{
                    add(3);
                    add(4);
                    add(5);
                    add(6);
                }});
                break;
            case 2:
                doTwo(auxiliaryDecision);
                alarmFlowchartService.setExecuteFlags(auxiliaryDecision.getTableInfoId(), new ArrayList<Integer>() {{
                    add(10);
                    add(11);
                }});
                break;
            case 3:
                doThree(auxiliaryDecision);
                alarmFlowchartService.setExecuteFlags(auxiliaryDecision.getTableInfoId(), new ArrayList<Integer>() {{
                    add(12);
                    add(13);
                }});
                break;
            default:
                log.error("推荐指令命令步骤有误,请查看数据是否正确--{}", JsonUtils.toJSONString(auxiliaryDecision));
                throw new RuntimeException("推荐指令命令步骤有误,请查看数据是否正确");
        }
    }

    /**
     * @Author yuelei
     * @Desc 执行第三次推荐指令
     * @Date 14:48 2022/8/5
     */
    private void doThree(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 1);
        //执行第一次推荐指令后，插入第四条告警信息 运行图预览信息
        alarmInfoOprtService.insertAdmAlertDetail(UidGeneratorUtils.getUID(),
                alarmInfo.getTableInfoId(), "系统产生运行图方案", "2", "运行图方案选择");
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
    }

    /**
     * @Author yuelei
     * @Desc 执行第二次推荐指令
     * @Date 14:48 2022/8/5
     */
    private void doTwo(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        log.info("列车进站过程中站台门故障产生故障,执行第一次扣车推荐指令,alarmInfo-" + alarmInfo);
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 1);
        //更新子表状态为已执行
        alarmInfo.setEndLife(false);
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_1.getCode());
        admAlertInfoSubService.updateById(alarmInfo);
        //流程结束，删除站台门上报缓行
        this.deleteAutoReportCache(alarmInfo);
    }

    //故障恢复删除已经上报过的故障
    private void deleteAutoReportCache(AlarmInfo alarmInfo) {
        int platformId = Integer.parseInt(alarmInfo.getPlatformId());
        if (platformId == 0) {
            return;
        }
        Integer doorIndex = doorInfoService.getDoorIndexByPlatformId(platformId);
        if (Objects.isNull(doorIndex)) {
            return;
        }
        BasicCommonCacheUtils.delMapKey(Cache.ALREADY_REPORT_PLATFORM_DOOR, String.valueOf(doorIndex));
        log.info("删除站台门缓存:{}", doorIndex);
    }

    /***
     * @Author yuelei
     * @Desc 执行第一次推荐指令
     * @Date 14:47 2022/8/5
     */
    private void doOne(AuxiliaryDecision auxiliaryDecision) {
        long infoId = auxiliaryDecision.getTableInfoId();
        //获取故障信息
        AlarmInfo alarmInfo = admAlertInfoSubService.queryByInfoId(infoId);
        Assert.notNull(alarmInfo,"生命周期已结束，推荐指令流程结束");
        log.info("列车进站过程中站台门故障产生故障,执行第一次扣车推荐指令,alarmInfo-" + alarmInfo);
        //执行推荐指令，调用对应handle处理
        aidDecSubStepRouter.aidDecSubStepRouter(alarmInfo, auxiliaryDecision);
        //更新推荐指令方案状态为已执行
        alarmInfoOprtService.updateStatus(alarmInfo.getTableInfoId(), alarmInfo.getTableBoxId(), 1);
        log.info("等待监测列车恢复正常");
        //更新子表状态为已执行
        admAlertInfoSubService.updateExecuteEnd(infoId, AlarmInfoEnum.EXECUTE_END_1.getCode());
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.ALL_PLATFORM_DOOR_OPEN_OUT_STATION;
    }
}
