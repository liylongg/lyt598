package com.tct.itd.adm.util;

import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmDispatchCmdDTO;
import com.tct.itd.adm.iconstant.AdmStationTypeConstant;
import com.tct.itd.adm.service.AdmAlertDetailService;
import com.tct.itd.adm.service.AdmDisCmdService;
import com.tct.itd.adm.service.AdmStationService;
import com.tct.itd.basedata.dfsread.service.handle.*;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.AdmStation;
import com.tct.itd.common.dto.ConStationDto;
import com.tct.itd.common.dto.TrainNumberAdjust;
import com.tct.itd.common.enums.TrainStateEnum;
import com.tct.itd.constant.NumConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.UidGeneratorUtils;
import com.tct.itd.utils.DateUtil;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName DisCmdSendUtils
 * @Description 电子调度命令发送工具类(推荐指令发送调度命令使用)
 * @Author YHF
 * @Date 2021/8/19 14:49
 */
@Component
@Slf4j
public class DisCmdSendUtils {

    @Resource
    private AdmAlertDetailService admAlertDetailService;
    @Resource
    private AdmDisCmdService admDisCmdService;
    @Resource
    private AdmStationService admStationService;
    @Resource
    private AlarmUtil alarmUtil;
    @Resource
    private ConStationInfoService conStationInfoService;

    @Resource
    private PlatformInfoService platformInfoService;

    @Resource
    private StopRegionDataService stopRegionDataService;


    /**
     * @param alarmInfo  告警对象
     * @param cmd        命令内容
     * @param strationId 受令处所
     * @return
     * @Description 自动发送电子调度命令 并记录推荐指令日志
     * @Date 14:44 2021/8/19
     **/
    public void sendDisCmd(AlarmInfo alarmInfo, String cmd, String strationId) {
        if (StringUtils.isEmpty(strationId)) {
            log.info("受领处所为null,请查证{}{}", alarmInfo, cmd);
            return;
        }
        AdmDispatchCmdDTO admDispatchCmdDTO = new AdmDispatchCmdDTO();
        //生成cmdId
        long cmdId = UidGeneratorUtils.getUID();
        admDispatchCmdDTO.setId(cmdId);
        cmd = getCommandContext(alarmInfo, cmd);
        admDispatchCmdDTO.setCommandContext(cmd);
        admDispatchCmdDTO.setCommandDate(DateUtil.getNowDateShort(new Date()));
        admDispatchCmdDTO.setCommandTime(DateUtil.getHms(new Date()));
        admDispatchCmdDTO.setCommandType(999L);
        admDispatchCmdDTO.setReceiveStation(strationId);
        admDisCmdService.insert(admDispatchCmdDTO);

        //设置上一步生成的电子调度命令ID为提醒ID，用于查询电子调用命令详情
        AdmAlertDetail alertDetail = new AdmAlertDetail(cmdId, alarmInfo.getTableInfoId(), "已发送电子调度命令", new Date(), cmd, "3", 1, new Date().getTime());
        admAlertDetailService.insert(alertDetail);
    }

    /**
     * 道岔故障获取组装后的电子调度命令
     *
     * @param alarmInfo 告警信息
     * @param cmd       调子调布命令
     * @return java.lang.String
     * @author liyunlong
     * @date 2022/2/23 15:44
     */
    private String getCommandContext(AlarmInfo alarmInfo, String cmd) {
        int alarmType = alarmInfo.getAlarmType();
        if (NumConstant.TWELVE != alarmType) {
            return cmd;
        }
        int executeStep = alarmInfo.getExecuteStep();
        if (alarmInfo.getAlarmTypeDetail() == 1201) {
            if (executeStep == 2 && cmd.contains("%s")) {
                String backTurnNumber = (String) BasicCommonCacheUtils.get(Cache.BACK_TRAIN_NUMBER);
                log.info("终端道岔故障具备本站折返,推荐指令步骤:【{}】,获取第一个站前折返车次号:【{}】", executeStep, backTurnNumber);
                BasicCommonCacheUtils.delKey(Cache.BACK_TRAIN_NUMBER);
                return String.format(cmd, backTurnNumber);
            }
            if ((executeStep == 3 || executeStep == -2) && cmd.contains("%s")) {
                String recoverTurnNumber = (String) BasicCommonCacheUtils.get(Cache.RECOVERY_TRAIN_NUMBER);
                log.info("终端道岔故障具备本站折返,推荐指令步骤:【{}】,获取恢复站前折返车次号:【{}】", executeStep, recoverTurnNumber);
                BasicCommonCacheUtils.delKey(Cache.RECOVERY_TRAIN_NUMBER);
                return String.format(cmd, recoverTurnNumber);
            }
        }else if ((executeStep == -2 || executeStep == -3) && cmd.contains("%s")) {
            String slowlyRecoveryTrainNumber = (String) BasicCommonCacheUtils.hGet(Cache.SLOWLY_RECOVERY_TRAIN_NUMBER, alarmInfo.getSwitchNo());
            log.info("道岔故障,推荐指令步骤:【{}】,获取缓行恢复车次号:【{}】", executeStep, slowlyRecoveryTrainNumber);
            BasicCommonCacheUtils.delKey(Cache.SLOWLY_RECOVERY_TRAIN_NUMBER);
            return String.format(cmd, slowlyRecoveryTrainNumber);
        }
        return cmd;
    }

    /*
     * @Description 获取所有车站id 逗号分隔 自动发送电子调度命令使用
     * @Date 15:50 2021/8/19
     * @return java.lang.String 所有车站stationid 逗号分隔
     **/
    public String getAllTrainStation() {
        StringBuilder stationIds = new StringBuilder();
        List<AdmStation> result = admStationService.selectListByType(AdmStationTypeConstant.TRAIN_STATION);
        if (result != null && result.size() > 0) {
            for (int i = 0; i < result.size(); i++) {
                stationIds.append(result.get(i).getStationId()).append(",");
            }
        }
        return stationIds.toString();
    }

    public List<Integer> getAllTrainStationList() {
        StringBuilder stationIds = new StringBuilder();
        List<AdmStation> result = admStationService.selectListByType(AdmStationTypeConstant.TRAIN_STATION);
        return result.stream().map(AdmStation::getStationId).collect(Collectors.toList());
    }

    /*
     * @Description 获取所有车辆段 逗号分隔 自动发送电子调度命令使用
     * @Date 15:50 2021/8/19
     * @return java.lang.String 所有车辆段stationid 逗号分隔
     **/
    public String getAllTrainSegment() {
        StringBuilder stationIds = new StringBuilder();
        List<AdmStation> listBaseResponse = admStationService.selectListByType(AdmStationTypeConstant.TRAIN_SEGMENT);
        if (listBaseResponse != null && listBaseResponse.size() > 0) {
            for (int i = 0; i < listBaseResponse.size(); i++) {
                stationIds.append(listBaseResponse.get(i).getStationId()).append(",");
            }
        }
        return stationIds.toString();
    }

    /*
     * @Description 获取所有车辆调ID 逗号分隔 自动发送电子调度命令使用
     * @Date 15:50 2021/8/19
     * @return java.lang.String 所有车辆调stationid 逗号分隔
     **/
    public String getAllTrainDispatch() {
        StringBuilder stationIds = new StringBuilder();
        List<AdmStation> listBaseResponse = admStationService.selectListByType(AdmStationTypeConstant.TRAIN_DISPATCH);
        if (listBaseResponse != null && listBaseResponse.size() > 0) {
            for (int i = 0; i < listBaseResponse.size(); i++) {
                stationIds.append(listBaseResponse.get(i).getStationId()).append(",");
            }
        }
        return stationIds.toString();
    }

    /*
     * @Description 获取所有车辆段与车辆调ID 逗号分隔 自动发送电子调度命令使用
     * @Date 15:50 2021/8/19
     * @return java.lang.String 所有车辆段与车辆调stationid 逗号分隔
     **/
    public String getAllTrainSegmentAndDispatch() {
        String allTrainSegment = getAllTrainSegment();
        String allTrainDispatch = getAllTrainDispatch();
        return allTrainSegment + allTrainDispatch;
    }

    /*
     * @Description 获取终点站受领处所id
     * @Date 16:26 2021/8/19
     * @param upDown 上下行信息
     * @return java.lang.String 终点站stationid 逗号分隔
     **/
    public String getDestinationStation(int upDown) {
        //获取车站终端数据
        List<AdmStation> admStations = admStationService.selectList(new AdmStation());
        if (upDown == IidsConstPool.TRAIN_DOWN) {
            return admStations.get(0).getStationId() + ",";
        } else if (upDown == IidsConstPool.TRAIN_UP) {
            return admStations.get(admStations.size() - 1).getStationId() + ",";
        } else {
            log.info("未获取到告警对象上下行信息");
        }
        return null;
    }

    /*
     * @Description 获取下一站站受领处所id
     * @Date 16:26 2021/8/19
     * @param upDown 上下行信息
     * @return java.lang.String 下一站stationid 逗号分隔
     **/
    public String getNextStation(int upDown, String stationId) {
        //获取车站终端数据
        List<AdmStation> admStations = admStationService.selectList(new AdmStation());
        if (upDown == IidsConstPool.TRAIN_UP) {
            //上行循环
            for (int i = 0; i < admStations.size(); i++) {
                if (admStations.get(i).getStationId() == Integer.parseInt(stationId) && i + 1 != admStations.size()) {
                    //获取下一站受令处所
                    return admStations.get(i + 1).getStationId() + ",";
                } else if (i + 1 == admStations.size()) {
                    //本站为终点站
                    return admStations.get(i).getStationId() + ",";
                }
            }
        } else if (upDown == IidsConstPool.TRAIN_DOWN) {
            //下行循环
            for (int i = admStations.size() - 1; i >= 0; i--) {
                if (admStations.get(i).getStationId() == Integer.parseInt(stationId) && i != 0) {
                    //获取下一站受令处所
                    return admStations.get(i - 1).getStationId() + ",";
                } else if (i == 0) {
                    //本站为终点站
                    return admStations.get(i).getStationId() + ",";
                }
            }
        }
        return null;
    }

    /**
     * @param stationId
     * @param upDown
     * @return java.lang.String
     * @Description 根据列车站台ID和上下行，获取后续车站ID
     * @Author yuelei
     * @Date 2021/11/16 14:52
     */
    public String getFollowUpStation(Integer stationId, Integer upDown) {
        StringBuilder receiveStation = new StringBuilder();
        //获取车站终端数据
        List<AdmStation> admStations = admStationService.selectFollowUpList(stationId, upDown);
        admStations.forEach(adm -> {
            receiveStation.append(adm.getStationId()).append(",");
        });
        return receiveStation.toString();
    }

    /**
     * @param acceptStation
     * @return java.lang.String
     * @Description 根据接收车站类型，获取车站ID
     * @Author yuelei
     * @Date 2021/10/27 16:33
     */
    public String getStationByAcceptType(AlarmInfo alarmInfo, Integer acceptStation) {
        String station = "";
        switch (acceptStation) {
            case 1:
                station = alarmInfo.getStationId() + ",";
                break;
            case 2:
                station = this.getAllTrainStation();
                break;
            case 3:
                station = this.getAllTrainSegment();
                break;
            case 4:
                station = this.getAllTrainDispatch();
                break;
            case 5:
                station = this.getAllTrainSegmentAndDispatch();
                break;
            case 6:
                station = this.getDestinationStation(alarmInfo.getUpDown());
                break;
            case 7:
                //station = this.getNextStation(alarmInfo.getUpDown(), String.valueOf(alarmInfo.getStationId()));
                 station = alarmUtil.getStationIdByTrainTraceInSection(alarmInfo.getTrainId())+"";
                break;
            case 8:
                station = this.getAllStationByConStation(alarmInfo);
                break;
            case 9:
                station = this.getStationIdByConStation(alarmInfo);
                break;
            case 10:
                station = this.getRetraceStation(alarmInfo);
                break;
            case 11:
                station = this.getAllDepotAndSegment();
                break;
            default:
                throw new BizException(CodeEnum.NO_GET_ACCEPT_STATION);
        }
        return station;
    }

    /**
     * @Author yuelei
     * @Desc  获取清客站台为折返站
     * @Date 17:41 2022/6/19
     */
    private String getRetraceStation(AlarmInfo alarmInfo) {
        StringBuilder stationIds = new StringBuilder();
        Set<Integer> set = new HashSet<>();
        if(BasicCommonCacheUtils.exist(Cache.TRAIN_NUMBER_ADJUST)){
            for (Map.Entry<Object, Object> m : BasicCommonCacheUtils.hmget(Cache.TRAIN_NUMBER_ADJUST,TrainNumberAdjust.class,List.class).entrySet()) {
                //如果不是当前应急事件对应调整信息则返回
                if (!m.getKey().equals(String.valueOf(alarmInfo.getTableInfoId()))) {
                    continue;
                }
                //todo 如果是中折站台是终点站，则不发调度命令，下版本优化
                List<TrainNumberAdjust> trainNumberAdjustList = (List<TrainNumberAdjust>) m.getValue();
                for (TrainNumberAdjust trainNumberAdjust : trainNumberAdjustList) {
                    log.info("车次调整信息为trainNumberAdjust：{}", JsonUtils.toJSONString(trainNumberAdjust));
                    if (Objects.equals(trainNumberAdjust.getAdjustType(), TrainStateEnum.TRAIN_RETURN.getValue())
                    ) {
                        if((trainNumberAdjust.getCleanPassengerStopAreaId() == null
                                || trainNumberAdjust.getCleanPassengerStopAreaId() == 0) ){
                            set.add(trainNumberAdjust.getEndStationId());
                        }else{
                            log.info("车次调整-车组号与故障车匹配,准备清客,车组号:{}", alarmInfo.getTrainId());
                            Integer stationIdByStopAreaId = stopRegionDataService.getStationIdByStopAreaId(trainNumberAdjust.getCleanPassengerStopAreaId());
                            set.add(stationIdByStopAreaId);
                        }
                    }
                }
            }
        }
        set.forEach(stationId -> stationIds.append(stationId).append(","));
        return stationIds.toString();
    }

    //调度命令发给集中站
    private String getStationIdByConStation(AlarmInfo alarmInfo) {
        if (Objects.isNull(alarmInfo) || StringUtils.isEmpty(alarmInfo.getAlarmConStation())) {
            throw new BizException("获取alarmInfo内集中站id，AlarmConStation为空！");
        }
        ConStationDto conStationDto = conStationInfoService.getStationByConStationId(Integer.parseInt(alarmInfo.getAlarmConStation()));
        if (Objects.isNull(conStationDto)) {
            throw new BizException("获取电子地图集中站失败！集中站id【{}】", alarmInfo.getAlarmConStation());
        }
        return String.valueOf(conStationDto.getStationId());
    }

    //调度命令发给集中站下所有车站
    private String getAllStationByConStation(AlarmInfo alarmInfo) {
        if (Objects.isNull(alarmInfo) || StringUtils.isEmpty(alarmInfo.getAlarmConStation())) {
            throw new BizException("获取alarmInfo内集中站id，AlarmConStation为空！");
        }
        List<Integer> stationIds = platformInfoService.getStationIdsByManageCi(Integer.parseInt(alarmInfo.getAlarmConStation()));
        if (CollectionUtils.isEmpty(stationIds)) {
            throw new BizException("获取电子地图集中站下所有车站id失败！");
        }
        StringBuilder sb = new StringBuilder();
        stationIds.forEach(stationId -> {
            sb.append(stationId);
            sb.append(",");
        });
        return sb.toString();
    }

    /***
     * @Author yuelei
     * @Desc  获取所有得停车厂和车辆段
     * @Date 15:33 2022/10/13
     */
    public String getAllDepotAndSegment(){
        //获取所有的车辆段或者停车场
        List<Integer> depotAndPark = admStationService.getDepotAndPark();
        StringBuilder sb = new StringBuilder();
        depotAndPark.forEach(stationId -> {
            sb.append(stationId);
            sb.append(",");
        });
        return sb.toString();
    }

}
