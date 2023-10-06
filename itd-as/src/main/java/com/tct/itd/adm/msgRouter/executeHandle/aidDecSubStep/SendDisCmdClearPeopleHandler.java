package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.api.AlgorithmClient;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.adm.runGraph.stategy.IPrePlanRunGraphStrategy;
import com.tct.itd.adm.service.AdmStationService;
import com.tct.itd.adm.util.DisCmdSendUtils;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.AlgorithmData;
import com.tct.itd.common.dto.AlgorithmResult;
import com.tct.itd.common.dto.TrainNumberAdjustDto;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Description 发送电子调度命令- 针对清客
 * @Author yuelei
 * @Date 2021/9/15 14:25
 */
@Service(value = "sendDisCmdClearPeople")
@Slf4j
public class SendDisCmdClearPeopleHandler implements AidDecSubStepHandler {

    @Resource
    private DisCmdSendUtils disCmdSendUtils;

    @Resource
    private AdmStationService admStationService;

    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Resource
    private AlgorithmClient algorithmClient;
    @Resource
    private List<IPrePlanRunGraphStrategy> graphDataStrategyList;

    private Map<String, IPrePlanRunGraphStrategy> graphDataStrategyHashMap = new HashMap<>();

    @PostConstruct
    public void init() {
        graphDataStrategyList.forEach(
                graphDataStrategy -> graphDataStrategyHashMap.put(graphDataStrategy.strategy(), graphDataStrategy)
        );
    }

    /**
     * @param alarmInfo
     * @param dto
     * @return void
     * @Description 发送车站类型：所有车站
     * @Author yuelei
     * @Date 2021/9/30 12:20
     */
    @Override
    @Async("taskExecutor")
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        String alarmTypeDetail = String.valueOf(alarmInfo.getAlarmTypeDetail());
        if (Objects.equals(AlarmTypeConstant.AIR_CONDITIONING_FAILURE, alarmTypeDetail)
                || Objects.equals(AlarmTypeConstant.BROADCAST_FAILURE_CANNOT_MANUAL, alarmTypeDetail)
                || Objects.equals(AlarmTypeConstant.AIR_CONDITIONING_VENTILATE_FAILURE, alarmTypeDetail)) {
            if (!needSendClearPeople(alarmInfo)) {
                log.info("算法未返回清客站台，不需要发送清客调令");
                return;
            }
        }
        log.info("调度命令对象{},执行单元对象{}", alarmInfo, JsonUtils.toJSONString(dto));
        //受令处所
        String station = disCmdSendUtils.getStationByAcceptType(alarmInfo, dto.getAcceptStation());
        log.info("开始发送调度命令，受令处所为：{}", station);
        if (station.split(",").length != 1) {
            throw new BizException("清客调度命令受令处所不唯一");
        }
        Integer stationId = Integer.valueOf(station.replace(",", ""));
        String stationName = admStationService.selectByStationId(stationId).getStationName();
        //上下行
        String upDown = alarmInfo.getUpDown() == IidsConstPool.TRAIN_UP ? "上行" : "下行";
        //清客
        String cmd = String.format(dto.getDisCmdContent(), alarmInfo.getOrderNum(), stationName, upDown, stationName);
        disCmdSendUtils.sendDisCmd(alarmInfo, cmd, station);
        log.info("调度命令发送成功");
    }

    /**
     * 通风机和人工广播故障调用算法判断是否需要清客
     *
     * @param alarmInfo 故障信息
     * @return 是否需要清客
     */
    private boolean needSendClearPeople(AlarmInfo alarmInfo) {
        IPrePlanRunGraphStrategy graphDataStrategy = graphDataStrategyHashMap.get(String.valueOf(alarmInfo.getAlarmTypeDetail()));
        if (graphDataStrategy == null) {
            log.error("未找到故障类型对应的运行图预览策略类");
            throw new BizException(CodeEnum.ALARM_TYPE_UNHANDLED);
        }
        //车门恢复统一设置晚点算法
        alarmInfo.setEndAlarmTime(graphDataStrategy.getEndAlarmTime(alarmInfo));
        alarmInfo.setAlarmState(graphDataStrategy.getAlarmState(alarmInfo));
        AlgorithmData algorithmData = aidDecisionExecService.getAlgorithmData(alarmInfo);
        Assert.notNull(algorithmData, "调整运行图时,未获取到获取算法参数");
        //算法自检
        BaseResponse<String> baseResponse = algorithmClient.check(algorithmData);
        if (baseResponse.getCode() != CodeEnum.SUCCESS.getCode()) {
            // 算法返回业务异常(如上一个故障影响范围内等), 该故障执行放弃流程
            if (baseResponse.getCode() == CodeEnum.ALG_CHECK_MSG.getCode()) {
                log.info("算法返回业务异常, 推荐指令执行放弃流程, 该alarmInfo为:{}", alarmInfo);
                aidDecisionExecService.waiveAidDecision(alarmInfo);
            }
            log.info("算法自检发现异常,错误码{},信息:{}", baseResponse.getCode(), baseResponse.getMessage());
            throw new BizException(CodeEnum.ALG_CHECK_ERROR.getCode(), baseResponse.getMessage());
        } else {
            log.info("算法自检通过:{}", baseResponse);
        }
        log.info("获取算法参数成功,准备调整运行图");
        log.info("获取算法参数成功,获取所有备车信息：{}", JsonUtils.toJSONString(algorithmData.getDepotInfoDtoList()));
        //调用算法服务，获取调整后运行图和调整车次列表
        BaseResponse<AlgorithmResult> result = algorithmClient.apiSolver(algorithmData);
        if (!result.getSuccess()) {
            aidDecisionExecService.waiveAidDecision(alarmInfo);
            log.error("准备获取新的运行图，调用算法异常：{}", result.getMessage());
            throw new BizException(CodeEnum.ALG_REQUEST_FAIL);
        }
        AlgorithmResult algorithmResult = result.getData();
        if (algorithmResult == null) {
            log.error("调整运行图时,请求算法服务失败");
            log.error("算法错误返回结果为空");
            throw new BizException(CodeEnum.ALG_CHANGE_GRAPH_FAIL);
        } else {
            log.info("请求算法成功");
        }
        //获取调整车次列表 dto 转 model
        List<TrainNumberAdjustDto> trainNumberAdjustDtoList = algorithmResult.getTrainNumberAdjustDtoList();
        for (TrainNumberAdjustDto trainNumberAdjust : trainNumberAdjustDtoList) {
            if (trainNumberAdjust.getCleanPassengerStopAreaId() == null
                    || trainNumberAdjust.getCleanPassengerStopAreaId() == 0
                    || StringUtils.isEmpty(trainNumberAdjust.getTrainId())) {
                continue;
            }
            return true;
        }
        return false;
    }


}
