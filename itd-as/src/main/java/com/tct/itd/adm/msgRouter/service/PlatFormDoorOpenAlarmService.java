package com.tct.itd.adm.msgRouter.service;

import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.service.*;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.constant.IidsSysParamPool;
import com.tct.itd.common.dto.Info;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.utils.SysParamKit;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/***
 * @Author yuelei
 * @Desc 进出站过程中站台门打开故障车站弹窗，处理逻辑
 * @Date 14:44 2022/9/21
 */
@Slf4j
@Service
public class PlatFormDoorOpenAlarmService {

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AdmAlertDetailService admAlertDetailService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AdmAlertDetailBoxService admAlertDetailBoxService;
    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    public void dealStationConfirm(AlarmInfo alarmInfo, Integer code){
        //故障发生时间
        long startTime = DateUtil.getStringToDate(alarmInfo.getStartAlarmTime(), "yyyy-MM-dd HH:mm:ss.SSS").getTime();
        //调用接口查出车辆预计出发时间 是否晚于现在2分钟以上
        long intervalTime = System.currentTimeMillis() - startTime;
        //推荐指令步骤
        int step;
        //列车晚点间隔时间单位：毫秒
        int time = Integer.parseInt(SysParamKit.getByCode(IidsSysParamPool.TRAIN_LATE_TIME));
        if (intervalTime > time ){
            //故障恢复-车晚点
            step = IidsConstPool.EXECUTE_STEP_3;
        }else{
            //故障恢复-车未晚点
            step = IidsConstPool.EXECUTE_STEP_2;
        }
        alarmInfo.setExecuteStep(step);
        //车门故障告警超时 继续执行超时推荐指令
        long detailId = UidGeneratorUtils.getUID();
        //应急事件列表记录title
        String title = "第二次推荐指令";
        //推荐指令步骤执行第二步
        alarmInfo.setExecuteStep(step);
        alarmInfo.setCaseCode(0);
        AdmAlertDetail alertDetail3 = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), title, new Date(), "系统产生运行图推荐指令", "1", 1, System.currentTimeMillis());
        admAlertDetailService.insert(alertDetail3);
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step,  0);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        long boxId = UidGeneratorUtils.getUID();
        //向push推送推荐指令指令 等待指令执行
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        admIdea.setTitle(aidDesSubStepUtils.getTitle(alarmInfo, step,  0));
        //推荐指令入库
        AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
        admAlertDetailBoxService.insert(alertDetailBox);
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmInfo.setTableBoxId2(boxId);
        //设置状态未执行
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
        admAlertInfoSubService.updateById(alarmInfo);
        log.info("进出站场景,生成第二次推荐指令提示成功");
        alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
            add(7);
            add(8);
            add(9);
        }});
    }
}
