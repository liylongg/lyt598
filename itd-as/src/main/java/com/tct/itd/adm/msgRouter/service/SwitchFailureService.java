package com.tct.itd.adm.msgRouter.service;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.entity.AdmAlertInfo;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.runGraph.PrePlanRunGraphContext;
import com.tct.itd.adm.service.*;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.basedata.dfsread.service.handle.StopRegionDataService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.dto.AdmRunGraphCases;
import com.tct.itd.common.dto.Info;
import com.tct.itd.common.dto.SlowyTrainDto;
import com.tct.itd.common.enums.AlarmSourceEnum;
import com.tct.itd.common.enums.SwitchFailureTypeEnum;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.constant.NumStrConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @classname: SwitchFailureServiceImpl
 * @description: 道岔故障服务
 * @author: liyunlong
 * @date: 2021/12/23 16:49
 */
@Slf4j
@Service
public class SwitchFailureService {

    @Resource
    private AdmAlertDetailService admAlertDetailService;

    @Resource
    private AppPushService appPushService;

    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;

    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;

    @Resource
    private AdmAlertDetailBoxService admAlertDetailBoxService;

    @Resource
    private AdmCommonMethodService admCommonMethodService;

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;

    @Resource
    private AdmAlertInfoService admAlertInfoService;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private PrePlanRunGraphContext prePlanRunGraphContext;

    @Resource
    private StopRegionDataService stopRegionDataService;

    @Resource
    private AdmStationService admStationService;

    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    public void sendAuxiliaryDecisionThree(Long tableInfoId) {
        // 续报表示列车具备通过条件
        AdmAlertInfo admAlertInfo = admAlertInfoService.selectById(tableInfoId);
        if (Boolean.FALSE.equals(admAlertInfo.getEndLife())) {
            throw new BizException("生命周期已结束，推荐指令流程结束");
        }
        AlarmInfo alarmInfo = admAlertInfoSubService.queryOnlyByInfoId(tableInfoId);
        long detailId = UidGeneratorUtils.getUID();
        AdmAlertDetail alertDetail3 = new AdmAlertDetail(detailId, tableInfoId, "第三次推荐指令",
                new Date(), "系统产生第三次推荐指令", NumStrConstant.ONE, NumConstant.ONE, System.currentTimeMillis());
        admAlertDetailService.insert(alertDetail3);
        //查询故障决策指令
        List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(),
                NumConstant.THREE, NumConstant.ZERO);
        //定义推荐指令执行单元
        List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
        //推荐指令步骤执行第三步
        alarmInfo.setExecuteStep(NumConstant.THREE);
        alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
        alarmInfo.setSource(AlarmSourceEnum.ATS.getType());
        admAlertInfoSubService.updateById(alarmInfo);
        // 调用算法获取调整方案列表
        AdmRunGraphCases admRunGraphCases = prePlanRunGraphContext.listPreviewRunGraph(alarmInfo);
        stepList = rebuildStepList(admRunGraphCases, stepList, alarmInfo);
        AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
        admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
        admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
        admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
        admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
        long boxId = UidGeneratorUtils.getUID();
        //推荐指令入库
        AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea),
                "方案待执行");
        admAlertDetailBoxService.insert(alertDetailBox);
        Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
        //置灰预览图按钮
        admCommonMethodService.disablePreviewButton(alarmInfo);
        appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
        alarmInfo.setTableBoxId2(boxId);
        admAlertInfoSubService.updateById(alarmInfo);
        if (!Integer.valueOf(AlarmTypeConstant.SWITCH_FAILURE_TERMINAL_BACK_HAS_PRE).equals(alarmInfo.getAlarmTypeDetail())) {
            admAlertInfoService.updateReportById(alarmInfo.getTableInfoId(), 2);
        }
        // 刷新流程图
        int alarmTypeDetail = alarmInfo.getAlarmTypeDetail();
        switch (alarmTypeDetail) {
            case 1202:
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(16, 17));
                break;
            case 1203:
            case 1204:
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(20, 21));
                break;
            default:
                break;
        }
    }

    private List<DisposeDto> rebuildStepList(AdmRunGraphCases admRunGraphCases,List<DisposeDto> list,AlarmInfo alarmInfo) {
        if (Objects.isNull(admRunGraphCases)) {
            log.error("调用算法获取缓行信息失败");
            return list;
        }
        List<SlowyTrainDto> slowlyTrainDtoList = admRunGraphCases.getAdmRunGraphCases().get(0).getSlowlyTrainDtoList();
        if (CollectionUtils.isEmpty(slowlyTrainDtoList)) {
            log.error("调用算法返回的缓行车次信息为空！");
            return list;
        }
        // 缓行车次号
        String slowlyTrainNumber;
        // 缓行车次缓行到的停车区域id
        String slowlyNextStopRegionId;
        // 缓行车次缓行到站台名称
        String stationName;
        if (slowlyTrainDtoList.size() == NumConstant.ONE) {
            slowlyTrainNumber = slowlyTrainDtoList.get(NumConstant.ZERO).getSlowlyTrainNumber();
            slowlyNextStopRegionId =
                    String.valueOf(slowlyTrainDtoList.get(NumConstant.ZERO).getSlowlyNextStopRegionId());
            Integer stationId = stopRegionDataService.getStationIdByStopAreaId(Integer.parseInt(slowlyNextStopRegionId));
            stationName = admStationService.selectByStationId(stationId).getStationName();
        }
        // 车次号+上下行 ex:01101(上行)
        else {
            slowlyTrainNumber =
                    StringUtils.join(slowlyTrainDtoList.stream().map(s -> (s.getSlowlyTrainNumber() + "(" + s.getUpDown() + ")")).collect(Collectors.toList()), "、");
            Set<Integer> slowlyNextStopRegionIdList =
                    slowlyTrainDtoList.stream().map(SlowyTrainDto::getSlowlyNextStopRegionId).collect(Collectors.toSet());
            slowlyNextStopRegionId = StringUtils.join(slowlyNextStopRegionIdList, ",");
            List<String> stationNameList = Lists.newArrayList();
            slowlyNextStopRegionIdList.forEach(s -> {
                Integer stationId = stopRegionDataService.getStationIdByStopAreaId(s);
                stationNameList.add(admStationService.selectByStationId(stationId).getStationName());
            });
            stationName = StringUtils.join(stationNameList, "、");
        }
        BasicCommonCacheUtils.hPut(Cache.SLOWLY_TRAIN_NUMBER, alarmInfo.getSwitchNo(), slowlyTrainNumber);
        BasicCommonCacheUtils.hPut(Cache.SLOWLY_NEXT_STOP_REGIONID, alarmInfo.getSwitchNo(), slowlyNextStopRegionId);
        BasicCommonCacheUtils.hPut(Cache.SLOWLY_NEXT_STATION_NAME, alarmInfo.getSwitchNo(), stationName);
        List<DisposeDto> newList = new ArrayList<>();
        String finalSlowlyTrainNumber = slowlyTrainNumber;
        list.forEach(s -> {
            String step = s.getStep();
            if (step.contains("%s号")) {
                step = step.replace("%s号", alarmInfo.getSwitchName() + "号");
            }
            if (step.contains("%s次")) {
                step = step.replace("%s次", finalSlowlyTrainNumber + "次");
            }
            if (step.contains("%s站")) {
                step = step.replace("%s站", stationName);
            }
            s.setStep(step);
            newList.add(s);
        });
        return newList;
    }

    public void waiveAidDecision(AlarmInfo alarmInfo) {
        if (Objects.isNull(alarmInfo) || alarmInfo.getAlarmType() != Integer.parseInt(AlarmTypeConstant.SWITCH_FAILURE)) {
            return;
        }
        // 故障放弃,删除道岔相关的缓存
        // 道岔故障运行图方案执行,运行图预览执行标识
        BasicCommonCacheUtils.delKey(Cache.SWITCH_FAILURE_PREVIEW_SIGN);
        // 道岔故障,中断图故障未恢复继续调图标识
        BasicCommonCacheUtils.delKey(Cache.SWITCH_TEN_PREVIEW_SIGN);
        // 道岔故障,中断图循环调图开始时间
        BasicCommonCacheUtils.delKey(Cache.SWITCH_FAILURE_TEN_TIME);
        // 道岔故障,缓行图故障未恢复继续调图标识
        BasicCommonCacheUtils.delKey(Cache.SWITCH_TWENTY_PREVIEW_SIGN);
        // 道岔故障,缓行图循环调图开始时间
        BasicCommonCacheUtils.delKey(Cache.SWITCH_FAILURE_TWENTY_TIME);
        // 道岔故障调是否调缓行图
        BasicCommonCacheUtils.delKey(Cache.SLOWLY_SIGN);
        // 终端站折返道岔故障-具备本站折返故障时,停在区间的车需要运行至下一站的车次号
        BasicCommonCacheUtils.delKey(Cache.TRAIN_NUMBER_LIST);
        // 终端站折返道岔故障-具备本站折返故障时,停在区间的车需要运行至下一站的停车区域id
        BasicCommonCacheUtils.delKey(Cache.NEXT_STOP_REGION_ID);
        // 终端道岔故障具备本站折返,第一个折返车次号
        BasicCommonCacheUtils.delKey(Cache.BACK_TRAIN_NUMBER);
        // 终端道岔故障具备本站折返,恢复正常折返车次
        BasicCommonCacheUtils.delKey(Cache.RECOVERY_TRAIN_NUMBER);
        // 开始缓行车次号
        BasicCommonCacheUtils.delKey(Cache.SLOWLY_TRAIN_NUMBER);
        // 缓行车次缓行至站台ID
        BasicCommonCacheUtils.delKey(Cache.SLOWLY_NEXT_STOP_REGIONID);
        // 缓行/中断到恢复车次号
        BasicCommonCacheUtils.delKey(Cache.SLOWLY_RECOVERY_TRAIN_NUMBER);
        // 缓行车次缓行至站台名称
        BasicCommonCacheUtils.delKey(Cache.SLOWLY_NEXT_STATION_NAME);
        // 道岔故障故障恢复Key
        BasicCommonCacheUtils.delKey(Cache.RECOVERY_SWITCH_FAILURE);
        // 道岔故障第一次推荐指令执行后故障恢复,是否需要调图
        BasicCommonCacheUtils.delKey(Cache.SWITCH_FAILURE_RECOVERY_FIRST_CHANGE_GRAPH);
        // 道岔调中断图时,返回跑小交路的停车区域id
        BasicCommonCacheUtils.delKey(Cache.SWITCH_FAILURE_STOP_AREA_LIST);
        // 道岔故障循环推图标识
        BasicCommonCacheUtils.delKey(Cache.SWITCH_FAILURE_ADJUST_LIST_SING);
        // 算法调图时间差,预览图和调图故障结束时间一致
        BasicCommonCacheUtils.delKey(Cache.CHANGE_GRAPH_TIME_DIFFERENCE);
        // 道岔故障执行第一次推荐指令时间
        BasicCommonCacheUtils.delKey(Cache.SWITCH_FAILURE_EXECUTE_ONE_TIME);
        // 道岔车站单操超过次数标记
        BasicCommonCacheUtils.delKey(Cache.SWITCH_OPERATE_SIGN);
        // 监测道岔操作故障恢复标识
        BasicCommonCacheUtils.delKey(Cache.SWITCH_OPERATE_RECOVERY_FLAG);
        // 道岔单操故障恢复
        BasicCommonCacheUtils.delKey(Cache.SWITCH_OPERATE_RECOVERY);
        // 道岔故障放弃,监测道岔状态变换缓存标识
        BasicCommonCacheUtils.hPut(Cache.SWITCH_GIVE_UP_SIGN, alarmInfo.getSwitchNo(), NumStrConstant.ONE);
        int alarmTypeDetail = alarmInfo.getAlarmTypeDetail();
        if (SwitchFailureTypeEnum.BEHIND_FAILURE_CHANGE.getCode() != alarmTypeDetail) {
            // 具备通车条件按钮禁用
            admAlertInfoService.updateReadyById(alarmInfo.getTableInfoId(), NumConstant.TWO);
        }
        // 故障恢复按钮禁用
        admAlertInfoService.updateAllowFailoverById(alarmInfo.getTableInfoId(), NumConstant.TWO);
    }
}
