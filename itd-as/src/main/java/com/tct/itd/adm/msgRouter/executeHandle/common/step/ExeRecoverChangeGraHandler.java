package com.tct.itd.adm.msgRouter.executeHandle.common.step;

import com.tct.itd.adm.msgRouter.router.AidDecSubStepHandler;
import com.tct.itd.adm.msgRouter.service.AidDecisionExecService;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description 故障恢复推荐指令-晚点-调整运行图
 * @Author yuelei
 * @Date 2021/9/27 15:27
 */
@Service("exeRecoverChangeGra")
@Slf4j
public class ExeRecoverChangeGraHandler implements AidDecSubStepHandler {
    @Resource
    private AidDecisionExecService aidDecisionExecService;

    @Override
    public void handle(AlarmInfo alarmInfo, AidDesSubStepOutDto dto) {
        //设置故障为恢复状态
        alarmInfo.setAlarmState(2);
        //设置故障结束时间
        String currentDateTime = DateUtil.getDate("yyyy-MM-dd HH:mm:ss.SSS");
        alarmInfo.setEndAlarmTime(currentDateTime);
        aidDecisionExecService.changeGraph(alarmInfo);
    }
}
