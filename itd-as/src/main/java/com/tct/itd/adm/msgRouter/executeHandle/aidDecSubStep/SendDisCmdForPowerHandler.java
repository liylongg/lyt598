package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.msgRouter.handle.TractionPowerHandler;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.util.DisCmdSendUtils;
import com.tct.itd.basedata.dfsread.service.base.MapStationBaseService;
import com.tct.itd.basedata.dfsread.service.base.MapTrackBaseService;
import com.tct.itd.common.dto.TractionPowerDto;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 接触网失电发送电子调度命令
 * @Author zhangjiarui
 * @Date 2022-3-8 14:33:20
 */
@Service(value = "sendDisCmdForPower")
@Slf4j
public class SendDisCmdForPowerHandler implements AidDecSubStepHandler {

    private static final int MINUS_ONE = -1;
    private static final int MINUS_TWO = -2;
    private static final String NULL = "无电区段不存在列车";

    @Resource
    private DisCmdSendUtils disCmdSendUtils;
    @Resource
    private TractionPowerHandler tractionPowerHandler;
    @Resource
    private MapTrackBaseService mapTrackService;
    @Resource
    private MapStationBaseService stationBaseService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        // 获取电子调度命令的车站
        TractionPowerDto powerDto = mapTrackService.getTractionPowerById(alarmInfo.getTractionSectionId());
        // 根据数据库中字段对推荐指令内容做不同处理
        if (MINUS_ONE == dto.getAcceptStation()) {
            // 站台id映射成车站id字符串
            String stations = StringUtils.join(powerDto.getStationIdList(), ",");
            log.info("开始发送调度命令，受令处所为：{}", stations);
            // 拼接可变字符
            String s = String.format(dto.getDisCmdContent(), powerDto.getSectionName());
            disCmdSendUtils.sendDisCmd(alarmInfo, s, stations);
            log.info("调度命令发送成功");
        }

        if (MINUS_TWO == dto.getAcceptStation()) {
            List<String> names=new ArrayList<>();
            String stations = StringUtils.join(powerDto.getStationIdList(), ",");
            log.info("开始发送调度命令，受令处所为：{}", stations);
            // 获取当前在无电区段的列车
            String trainForPower = tractionPowerHandler.getTrainForPower(alarmInfo);
            if ("".equals(trainForPower)) {
                trainForPower = NULL;
            }
            // 拼接可变字符
            List<Integer> stationList=powerDto.getStationIdList();
            for (Integer station : stationList) {
                names.add(stationBaseService.getStaionNameByStaionId(station));
            }
            String s = String.format(dto.getDisCmdContent(), StringUtils.join(names, ","),trainForPower);
            disCmdSendUtils.sendDisCmd(alarmInfo, s, stations);
            log.info("调度命令发送成功");
        }
    }

}
