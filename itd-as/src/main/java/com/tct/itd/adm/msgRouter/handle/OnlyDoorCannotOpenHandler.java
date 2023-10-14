package com.tct.itd.adm.msgRouter.handle;

import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.msgRouter.router.AlarmInfoMessageHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.msgRouter.service.AlarmInfoOprtService;
import com.tct.itd.adm.msgRouter.service.AppPushService;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.service.AlarmFlowchartService;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.basedata.dfsread.service.handle.StopRegionDataService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.Info;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 车门故障-单车门无法打开
 * @Author yl
 * @Date 2022/6/15 14:15
 **/
@Slf4j
@Component
public class OnlyDoorCannotOpenHandler implements AlarmInfoMessageHandler {

    @Resource
    private AidDecisionExecService aidDecisionExecService;
    @Resource
    private AlarmInfoOprtService alarmInfoOprtService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AlarmUtil alarmUtil;
    @Resource
    private StopRegionDataService stopRegionDataService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;
    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    @Override
    public void handle(AlarmInfo alarmInfo) {
        //产生故障
        if (alarmInfo.getAlarmState() == 1){
            //删除上次故障可能存在的定时检测恢复标记，避免导致推荐指令未执行就推送恢复。
            BasicCommonCacheUtils.delKey(Cache.TRAIN_DOOR_AUTO_CHECK_RECOVERY);
            log.info("车门故障-单车门无法打开,产生故障:{}", alarmInfo);
            // 设置站台,停车区域编号信息
            Integer platformId =alarmUtil.getPlatformIdByTrainTrace(alarmInfo.getTrainId());
            alarmInfo.setPlatformId(String.valueOf(platformId));
            alarmInfo.setStopAreaNumber(stopRegionDataService.getStopAreaByPlatformId(platformId));
            //设置推荐指令步骤
            alarmInfo.setExecuteStep(1);
            alarmInfo.setTableBoxId2(-1L);
            long detailId = aidDecisionExecService.doExecAidDecision(alarmInfo);
            //查询故障决策指令
            List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), 1, 0);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            aidDesSubStepUtils.getTrainDoorStep(stepList, alarmInfo.getAutoMsg());
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = new AdmIdea(alarmInfo.getTableInfoId(),alarmInfo.getTrainId(),alarmInfo.getOrderNum(),alarmInfo.getStartAlarmTime(),alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(),alarmInfo.getAlarmTypeDetail(),stepList,alarmInfo.getStationId(),alarmInfo.getAlarmState(),alarmInfo.getExecuteStep());
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescribeByCode(admIdea.getAlarmType()));
            //设置故障子类型文本信息,前端显示
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            //设置推荐指令标题
            admIdea.setTitle(aidDesSubStepUtils.getTitle(alarmInfo, 1, 0));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(),admIdea);
            //插入推荐指令数据
            long boxId = UidGeneratorUtils.getUID();
            alarmInfo.setTableBoxId(boxId);
            //推荐指令入库
            alarmInfoOprtService.insertAdmAlertDetailBox(boxId,detailId,"方案待执行", JsonUtils.toJSONString(admIdea));
            //告警信息存入子表
            admAlertInfoSubService.insert(alarmInfo);
            log.info("第一次推荐指令推送内容admInfo：{}", JsonUtils.toJSONString(admInfo));
            //上报故障后,记录该车次在该站已经上报故障,防止重复上报故障
            BasicCommonCacheUtils.hPut(Cache.ALREADY_REPORT_TRAIN_DOOR, alarmInfo.getTrainId() + ":" + platformId, "1");
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            //更新流程图
            alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
                add(1);
                add(2);
            }});
            // 车门屏蔽门故障报警
            BasicCommonCacheUtils.set(Cache.TRAIN_PLATFORM_DOOR_ALARM, alarmInfo.getTrainId());
        }
        //点击解除故障按钮，推送故障恢复推荐指令
        else {
            //获取调度命令对象
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo);
            //推荐指令步骤
            int step;
            //列车晚点间隔时间单位：毫秒
            TiasTraceInfo tiasTraceInfo = trainTraceCache.getIfPresent(alarmInfo.getTrainId());
            int otpTime = tiasTraceInfo.getOtpTime() * 1000;
            log.info("列车【{}】晚点时间【{}】", tiasTraceInfo.getTrainId(), otpTime);
            //列车晚点间隔时间单位：毫秒
            int time = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_LATE_TIME));
            if (otpTime > time ){
                //故障恢复-车晚点
                admIdea.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_2);
                step = IidsConstPool.EXECUTE_STEP_0_2;
            }else{
                //故障恢复-车未晚点
                admIdea.setExecuteStep(IidsConstPool.EXECUTE_STEP_0_1);
                step = IidsConstPool.EXECUTE_STEP_0_1;
            }
            //查询故障决策指令-列车进站过程中站台门打开
            List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 0);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            admIdea.setDispose(stepList);
            //赋值执行单元信息
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            //设置推荐指令标题
            admIdea.setTitle(aidDesSubStepUtils.getTitle(alarmInfo, step, 0));
            //推送弹窗显示时长: 默认30s
            admIdea.setShowSecond(30);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode((admIdea.getAlarmTypeDetail())));
            //是否故障恢复
            admIdea.setIsRecovery(1);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(),admIdea);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG,admInfo);
            log.info("推送故障恢复推荐指令指令:{}", JsonUtils.toJSONString(admInfo));
            //推送车站提示框，车门故障已恢复
            appPushService.sendMessage(MsgTypeEnum.STATION_RECOVER_ALERT_WID,
                    new WebNoticeDto(CommandEnum.STATION_RECOVER_ALERT_WID.getMsgCode(), String.valueOf(alarmInfo.getStationId()),
                            admIdea.getAlarmTypeDetailStr() + "已恢复"));
            alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
                add(9);
                add(26);
            }});
        }
    }

    @Override
    public String channel() {
        return AlarmTypeConstant.ONLY_DOOR_CANNOT_OPEN;
    }
}