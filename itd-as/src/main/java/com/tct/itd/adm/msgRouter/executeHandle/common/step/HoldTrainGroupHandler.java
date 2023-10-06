package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.google.common.collect.Lists;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.service.AdmAlertInfoUpgradeService;
import com.tct.itd.adm.service.HoldTrainService;
import com.tct.itd.constant.NumStrConstant;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @classname: HoldTrainGroupHandler
 * @description: 分组扣车
 * @author: liyunlong
 * @date: 2022/8/4 17:51
 */
@Service("holdTrainGroup")
@Slf4j
public class HoldTrainGroupHandler implements AidDecSubStepHandler {

    @Resource
    private HoldTrainService holdTrainService;

    @Resource
    private AdmAlertInfoUpgradeService admAlertInfoUpgradeService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        log.info("执行扣车推荐指令,alarmInfo:{},AidDesSubStepOutDto:{}",
                JsonUtils.toJSONString(alarmInfo), JsonUtils.toJSONString(dto));
        String holdParam = dto.getParam();
        if (StringUtils.isEmpty(holdParam)) {
            throw new BizException("扣车参数为空");
        }
        //获取id
        String platformId = getPlatformId(alarmInfo);
        if (StringUtils.isEmpty(platformId) || NumStrConstant.ZERO.equals(platformId)) {
            log.warn("未设置扣车站台！");
            return;
        }
        String[] platformIds = platformId.split("/");
        Arrays.asList(platformIds).parallelStream().forEach(p -> {
            List<Integer> platformIdList = Lists.newArrayList();
            platformIdList.add(Integer.parseInt(p));
            holdTrainService.holdTrain(platformIdList, holdParam);
        });
    }

    private String getPlatformId(AlarmInfo alarmInfo) {
        if (alarmInfo.getAlarmTypeDetail() != Integer.parseInt(AlarmTypeConstant.AXLE_COUNTER_SWITCH)) {
            return alarmInfo.getPlatformId();
        }
        //道岔区段计轴故障platformId特殊处理
        if (-1L != alarmInfo.getUpgradeId()) {
            AlarmInfo upgrade = admAlertInfoUpgradeService.getById(alarmInfo.getUpgradeId());
            if(!Objects.isNull(upgrade)){
                return upgrade.getPlatformId();
            }
        }
        return alarmInfo.getPlatformId();
    }
}
