package com.tct.itd.adm.msgRouter.router;

import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Description 执行单元路由入口
 * @Author yuelei
 * @Date 2021/9/15 14:27
 */
@Component
@Slf4j
public class AidDecSubStepRouter {
    /**
     * @Description 执行路由策略
     * @Author yuelei
     * @Date 2021/9/15 14:56
     * @param alarmInfo
     * @param auxiliaryDecision
     * @return void
     */
    public void aidDecSubStepRouter(AlarmInfo alarmInfo, AuxiliaryDecision auxiliaryDecision){
        AlarmInfo alarmInfo1 = new AlarmInfo();
        BeanUtils.copyProperties(alarmInfo, alarmInfo1);
        List<AidDesSubStepOutDto> dtoList = auxiliaryDecision.getAidDesSubStepDtoList();
        if(dtoList == null || dtoList.isEmpty()){
            log.info("未获取到执行单元内容");
            throw new BizException(CodeEnum.NO_FOUND_AID_SUB_STEP);
        }
        log.info("推荐指令执行单元内容：{}", dtoList);
        //循环执行单元，过滤掉类型0只显示的执行单元
        dtoList.stream().filter(dto -> dto.getSubStepType() != 0).forEach(dto->{
            log.info("执行此步骤单元的对象为：{}", dto.getBeanName());
            AidDecSubStepHandler aidDecSubStepHandler = (AidDecSubStepHandler) SpringContextUtil.getBean(dto.getBeanName());
            aidDecSubStepHandler.handle(alarmInfo1, dto);
        });
    }
}
