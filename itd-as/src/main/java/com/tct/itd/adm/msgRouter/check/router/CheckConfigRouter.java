package com.tct.itd.adm.msgRouter.check.router;

import com.tct.itd.adm.msgRouter.service.CheckConfigService;
import com.tct.itd.common.dto.AdmCheckConfigDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @Description 执行单元路由入口
 * @Author yuelei
 * @Date 2021/9/15 14:27
 */
@Component
@Slf4j
public class CheckConfigRouter {
    @Resource
    private CheckConfigService checkConfigService;

    /**
     * @Description 执行路由策略
     * @Author yuelei
     * @Date 2021/9/15 14:56
     * @param alarmInfo
     * @return void
     */
    public void aidDecSubStepRouter(AlarmInfo alarmInfo){
        //获取此故障类型，需要执行的校验
        List<AdmCheckConfigDto> entities =
                checkConfigService.listByAlarmType(String.valueOf(alarmInfo.getAlarmTypeDetail()));
        if(Objects.isNull(entities) ||  entities.isEmpty()){
            log.info("该故障类型录入不需要校验");
            return;
        }
        log.info("获取所有的检验配置：{}", entities);
        try {
            entities.forEach(dto->{
                log.info("进行此校验步骤：{}", dto.getBeanName());
                CheckConfigHandler handler = (CheckConfigHandler) SpringContextUtil.getBean(dto.getBeanName());
                handler.handle(alarmInfo);
            });
        } catch (BizException e) {
            log.error("故障录入校验出错：{}", e.getErrorMsg(), e);
            throw new BizException(e.getErrorCode(), e.getErrorMsg());
        }
        catch (Exception e){
            log.error("故障录入校验出错：{}", e.getMessage(), e);
            throw new BizException(CodeEnum.ALARM_ENTRY_ERROR);
        }
    }
}
