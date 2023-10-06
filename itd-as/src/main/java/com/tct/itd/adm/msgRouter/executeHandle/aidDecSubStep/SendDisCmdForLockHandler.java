package com.tct.itd.adm.msgRouter.executeHandle.aidDecSubStep;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.util.DisCmdSendUtils;
import com.tct.itd.basedata.dfsread.service.base.MapLinkBaseService;
import com.tct.itd.basedata.dfsread.service.handle.ConStationInfoService;
import com.tct.itd.basedata.dfsread.service.handle.PlatformInfoService;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 联锁双机发送电子调度命令
 * @Author zhangjiarui
 * @Date 2022-4-11 14:33:20
 */
@Service(value = "sendDisCmdForLock")
@Slf4j
public class SendDisCmdForLockHandler implements AidDecSubStepHandler {

    private static final Integer MINUS_ONE = -1;
    private static final Integer MINUS_TWO = -2;
    @Resource
    private PlatformInfoService platformInfoService;
    @Resource
    private DisCmdSendUtils disCmdSendUtils;
    @Resource
    private ConStationInfoService conStationInfoService;
    @Resource
    private MapLinkBaseService linkBaseService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        //根据联锁id获取集中区id
        List<Integer> rtuIdList=linkBaseService.getRtuIdsByCiId(Integer.parseInt(alarmInfo.getAlarmConStation()));
        Integer acceptStation = dto.getAcceptStation();
        StringBuilder stations = new StringBuilder();

        // 发送给联锁内所有车站
        if (MINUS_ONE.equals(acceptStation)) {
            // 获取故障集中区所有车站
            for (Integer  r: rtuIdList) {
                List<Integer> stationList = platformInfoService.getStationIdsByManageCi(r);
                stations.append(StringUtils.join(stationList, ","));
                stations.append(",");
            }
            stations.deleteCharAt(stations.length()-1);
        }
        // 发送给集中站
        if (MINUS_TWO.equals(acceptStation)) {
        List<String> list = new ArrayList<>();
            for (Integer rtuId : rtuIdList) {
                list.add(conStationInfoService.getStationByConStationId(rtuId).getStationId()+"");
            }
            stations.append(StringUtils.join(list, ","));
       }
        log.info("开始发送调度命令，受令处所为：{}", stations);
        disCmdSendUtils.sendDisCmd(alarmInfo, dto.getDisCmdContent(), stations.toString());
        log.info("调度命令发送成功");
    }
}
