package com.tct.itd.adm.msgRouter.service;


import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmDispatchCmdDTO;
import com.tct.itd.adm.iconstant.AdmCmdTemplateCodeConstant;
import com.tct.itd.adm.iconstant.ReplaceNameConstant;
import com.tct.itd.adm.service.AdmAlertDetailService;
import com.tct.itd.adm.service.AdmCmdTemplateService;
import com.tct.itd.adm.service.AdmDisCmdService;
import com.tct.itd.adm.service.AdmStationService;
import com.tct.itd.adm.util.DisCmdSendUtils;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.WebNoticeCodeConst;
import com.tct.itd.common.dto.AdmCmdTemplateDto;
import com.tct.itd.common.dto.TrainNumberAdjust;
import com.tct.itd.dto.WebNoticeDto;
import com.tct.itd.enums.MsgPushEnum;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName DisCmdService
 * @Description TODO
 * @Author YHF
 * @Date 2021/5/28 16:39
 */
@Slf4j
@Service
public class DisCmdService {
    @Resource
    private AdmStationService admStationService;
    @Resource
    private AdmCmdTemplateService admCmdTemplateService;
    @Resource
    private AppPushService appPushService;
    @Resource
    private DisCmdSendUtils disCmdSendUtils;
    @Resource
    private AdmDisCmdService admDisCmdService;
    @Resource
    private AdmAlertDetailService admAlertDetailService;

    @Async("taskExecutor")
    public void sendAutoCmd(List<TrainNumberAdjust> trainNumberAdjusts, long infoId) {
        try {
            AdmCmdTemplateDto dto = new AdmCmdTemplateDto();
            //默认0 自动下发
            dto.setType(2);
            Set<TrainNumberAdjust> collect = trainNumberAdjusts.stream().filter(x -> x.getCmdAdjustType() != 0).collect(Collectors.toSet());
            log.info("自动下发调度命令集合：{}", JsonUtils.toJSONString(collect));
            //筛选出调表的命令类型
            List<TrainNumberAdjust> changeServerList = collect.stream().filter(adjust -> adjust.getCmdAdjustType() == AdmCmdTemplateCodeConstant.ADD_6 || adjust.getCmdAdjustType() == AdmCmdTemplateCodeConstant.ADD_7)
                    .collect(Collectors.toList());
            if (!changeServerList.isEmpty()) {
                changeServerList.forEach(changeServer -> {
                    String key = Integer.parseInt(changeServer.getServerNumber()) + "" + Integer.parseInt(changeServer.getTrainOrder());
                    BasicCommonCacheUtils.hPut(Cache.ADJUST_SERVER_SEND_CMD_TIMER, key, changeServer);
                });
            }
            //删除调表调令
            changeServerList.forEach(collect::remove);
            //调度命令内容
            for (TrainNumberAdjust trainNumberAdjust : collect) {
                log.info("自动下发调度命令车次调整信息：{}", JsonUtils.toJSONString(trainNumberAdjust));
                //接收车站
                Set<Integer> set = new HashSet<>();
                String cmd = "";
                if (!Objects.isNull(trainNumberAdjust.getCmdAdjustType())) {
                    Integer cmdAdjustType = trainNumberAdjust.getCmdAdjustType();
                    dto.setCode(cmdAdjustType);
                    cmd = admCmdTemplateService.getContent(dto);
                    if (cmd.contains(ReplaceNameConstant.DROP_STATION_NAME)) {
                        String dropStationName = trainNumberAdjust.getTrainNumberAdjustSubDto().getOffLineStationName();
                        cmd = cmd.replaceAll(ReplaceNameConstant.DROP_STATION_NAME, dropStationName);
                    }
                    if (cmd.contains(ReplaceNameConstant.TRAIN_NUM)) {
                        cmd = cmd.replaceAll(ReplaceNameConstant.TRAIN_NUM, trainNumberAdjust.getTrainId());
                    }
                    if (cmd.contains(ReplaceNameConstant.IS_UP)) {
                        cmd = cmd.replaceAll(ReplaceNameConstant.IS_UP, trainNumberAdjust.getIsUp() ? "上行" : "下行");
                    }
                    if (cmd.contains(ReplaceNameConstant.END_DEPOT_NAME)) {
                        String endDepotName = admStationService.selectByStationId(trainNumberAdjust.getEndCarDepotId()).getStationName();
                        cmd = cmd.replaceAll(ReplaceNameConstant.END_DEPOT_NAME, endDepotName);
                    }
                    if (cmd.contains(ReplaceNameConstant.END_STATION_NAME)) {
                        String endStationName = admStationService.selectByStationId(trainNumberAdjust.getEndStationId()).getStationName();
                        cmd = cmd.replaceAll(ReplaceNameConstant.END_STATION_NAME, endStationName);
                    }
                    if (cmd.contains(ReplaceNameConstant.NEXT_ORDER_NUM)) {
                        cmd = cmd.replaceAll(ReplaceNameConstant.NEXT_ORDER_NUM,  trainNumberAdjust.getNextServerNumber() + trainNumberAdjust.getNextTrainNumber());
                    }
                    if(cmd.contains(ReplaceNameConstant.ADD_TRAIN_START_STATION_NAME)){
                        List<String> list = trainNumberAdjust.getAddStartAndEndStationName();
                        cmd = cmd.replaceAll(ReplaceNameConstant.ADD_TRAIN_START_STATION_NAME, list.get(0));
                    }
                    if(cmd.contains(ReplaceNameConstant.ADD_TRAIN_END_STATION_NAME)){
                        List<String> list = trainNumberAdjust.getAddStartAndEndStationName();
                        cmd = cmd.replaceAll(ReplaceNameConstant.ADD_TRAIN_END_STATION_NAME, list.get(1));
                    }
                    if(cmd.contains(ReplaceNameConstant.START_PASSENGER_STATION_NAME)){
                        cmd = cmd.replaceAll(ReplaceNameConstant.START_PASSENGER_STATION_NAME, trainNumberAdjust.getTrainNumberAdjustSubDto().getAddTrainStationNameList());
                    }
                    if(cmd.contains(ReplaceNameConstant.ORDER_NUM)){
                        cmd = cmd.replaceAll(ReplaceNameConstant.ORDER_NUM, trainNumberAdjust.getServerNumber() + trainNumberAdjust.getTrainOrder());
                    }
                    if(cmd.contains(ReplaceNameConstant.ADD_TRAIN_STATION_LAUNCH_TIME)){
                        cmd = cmd.replaceAll(ReplaceNameConstant.ADD_TRAIN_STATION_LAUNCH_TIME, trainNumberAdjust.getAddTrainStationLaunchTime());
                    }
                    if(cmd.contains(ReplaceNameConstant.START_STATION_NAME)){
                        String startStationName = admStationService.selectByStationId(trainNumberAdjust.getStartStationId()).getStationName();
                        cmd = cmd.replaceAll(ReplaceNameConstant.START_STATION_NAME, startStationName);
                    }
                    if(cmd.contains(ReplaceNameConstant.PRE_ORDER_NUM)){
                        cmd = cmd.replaceAll(ReplaceNameConstant.PRE_ORDER_NUM, trainNumberAdjust.getPreServerNumber() + trainNumberAdjust.getPreTrainNumber());
                    }
                    if(cmd.contains(ReplaceNameConstant.FIRST_ADD_VENT_ORDER_NUM)){
                        cmd = cmd.replaceAll(ReplaceNameConstant.FIRST_ADD_VENT_ORDER_NUM, trainNumberAdjust.getFirstAddVentServerNumber() + trainNumberAdjust.getFirstAddVentTrainOrder());
                    }
                    if(cmd.contains(ReplaceNameConstant.FIRST_ADD_VENT_RETURN_STATION_NAME)){
                        cmd = cmd.replaceAll(ReplaceNameConstant.FIRST_ADD_VENT_RETURN_STATION_NAME, trainNumberAdjust.getFirstAddVentReturnStationName());
                    }

                    if (cmdAdjustType == AdmCmdTemplateCodeConstant.DROP_LINE_1) {
                        //受令处所
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.DROP_LINE_2) {
                        //受令处所
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.DROP_LINE_5) {
                        //受令处所
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.END_DROP_LINE_6) {
                        //受令处所
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.END_DROP_LINE_7) {
                        //受令处所
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.DROP_LINE_8) {
                        //受令处所
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.DROP_LINE_9) {
                        //trainNum车不停站通过运行至endDepotName车厂，各站做好客服。
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.DROP_LINE_10) {
                        //trainNum车不停站通过运行endStationName折返，加开空车nextOrderNum次返回车厂，各站做好客服。
                        //受令处所
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.DROP_LINE_11) {
                        //受令处所
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.DROP_LINE_12) {
                        //trainNum车运行回endDepotName车厂，各站做好客服。
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if(cmdAdjustType == AdmCmdTemplateCodeConstant.DROP_LINE_13){
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.DROP_LINE_14){
                        set.addAll(trainNumberAdjust.getCmdStationIdList());
                        set.add(trainNumberAdjust.getEndCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.ADD_1) {
                        //受令处所
                        set.addAll(disCmdSendUtils.getAllTrainStationList());
                        set.add(trainNumberAdjust.getStartCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.ADD_2) {
                        //受令处所
                        set.addAll(disCmdSendUtils.getAllTrainStationList());
                        set.add(trainNumberAdjust.getStartCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.ADD_3) {
                        //受令处所
                        set.addAll(disCmdSendUtils.getAllTrainStationList());
                        set.add(trainNumberAdjust.getStartCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.ADD_4) {
                        //受令处所
                        set.addAll(disCmdSendUtils.getAllTrainStationList());
                        set.add(trainNumberAdjust.getStartCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.ADD_5) {
                        //受令处所
                        set.addAll(disCmdSendUtils.getAllTrainStationList());
                        set.add(trainNumberAdjust.getStartCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.ADD_8) {
                        //受令处所
                        set.addAll(disCmdSendUtils.getAllTrainStationList());
                        set.add(trainNumberAdjust.getStartCarDepotId());
                    } else if (cmdAdjustType == AdmCmdTemplateCodeConstant.ADD_9) {
                        set.addAll(disCmdSendUtils.getAllTrainStationList());
                        set.add(trainNumberAdjust.getStartCarDepotId());
                    }
                    if (StringUtils.isEmpty(cmd)) {
                        log.info("调度命令内容为空");
                        continue;
                    }
                    if (set.size() == 0) {
                        log.info("受令处所为空");
                        continue;
                    }
                    StringBuilder sb = new StringBuilder();
                    set.forEach(x -> {
                        sb.append(x).append(",");
                    });
                    AdmDispatchCmdDTO admDispatchCmdDTO = new AdmDispatchCmdDTO();
                    admDispatchCmdDTO.setReceiveStation(sb.toString());
                    //生成cmdId
                    long cmdId = UidGeneratorUtils.getUID();
                    admDispatchCmdDTO.setId(cmdId);
                    admDispatchCmdDTO.setCommandContext(cmd);
                    admDispatchCmdDTO.setCommandDate(DateUtil.getNowDateShort(new Date()));
                    admDispatchCmdDTO.setCommandTime(DateUtil.getHms(new Date()));
                    admDispatchCmdDTO.setCommandType(999L);
                    admDisCmdService.insert(admDispatchCmdDTO);

                    //设置上一步生成的电子调度命令ID为提醒ID，用于查询电子调用命令详情
                    AdmAlertDetail alertDetail = new AdmAlertDetail(cmdId, infoId, "已发送电子调度命令", new Date(), cmd, "3", 1, System.currentTimeMillis());
                    admAlertDetailService.insert(alertDetail);
                    //发往中心，前端进行刷新，并且带上推荐指令信息
                    appPushService.sendWebNoticeMessageToAny(
                            new WebNoticeDto(WebNoticeCodeConst.REFRESH_NULL_MSG, "0", ""));
                }
            }
        } catch (Exception e) {
            log.error("调度命令自动下发异常：" + e.getMessage(), e);
            //发往中心，前端进行刷新，并且带上推荐指令信息
            appPushService.sendWebNoticeMessageToAny(
                    new WebNoticeDto(MsgPushEnum.ERROR_MSG.getCode(), "0", "调度命令自动下发出错"));
        }
    }

}
