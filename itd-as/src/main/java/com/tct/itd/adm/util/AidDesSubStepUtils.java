package com.tct.itd.adm.util;

import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AidStepTitleEnum;
import com.tct.itd.adm.iconstant.DesSubStepBeanConstant;
import com.tct.itd.adm.iconstant.ReplaceNameConstant;
import com.tct.itd.adm.iconstant.SubStepTypeConstant;
import com.tct.itd.adm.service.AdmAlertDetailTypeService;
import com.tct.itd.adm.service.AdmCmdTypeService;
import com.tct.itd.adm.service.AdmStationService;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.DetailTypeDto;
import com.tct.itd.common.dto.StepDto;
import com.tct.itd.common.dto.SubStepDto;
import com.tct.itd.constant.StringConstant;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.utils.BasicCommonCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Description 推荐指令工具类
 * @Author yuelei
 * @Date 2021/9/16 15:48
 */
@Slf4j
@Component
public class AidDesSubStepUtils {

    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AdmStationService admStationService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AdmCmdTypeService admCmdTypeService;

    @Resource(name = "aidDesStepCache")
    private com.github.benmanes.caffeine.cache.Cache<Integer, DetailTypeDto> aidDesStepCache;

    /**
     * @param alarmTypeDetail
     * @param step
     * @return java.util.List<com.tct.iids.entity.AidDesSubStepEntity>
     * @Description
     * @Author yuelei
     * @Date 2021/9/17 10:17
     */
    public List<AidDesSubStepEntity> getAidDesSubStep(Integer alarmTypeDetail, Integer step, Integer type) {
        DetailTypeDto detailTypeDto = aidDesStepCache.getIfPresent(alarmTypeDetail);
        List<StepDto> stepDtoList = detailTypeDto.getStepDtoList();
        List<StepDto> collect = stepDtoList.stream().filter(dto -> Objects.equals(dto.getStep(), step) && Objects.equals(dto.getType(), type))
                .collect(Collectors.toList());
        List<SubStepDto> subStepDtoList = collect.get(0).getSubStepDtoList();
        List<SubStepDto> subStepDtoListCopy = new ArrayList<>();
        subStepDtoList.forEach(subStepDto -> {
            SubStepDto dto = new SubStepDto();
            BeanUtils.copyProperties(subStepDto, dto);
            subStepDtoListCopy.add(dto);
        });
        //循环查询调度命令内容，通过code找content
        subStepDtoListCopy.forEach(subStepDto -> {
            if (!StringUtils.isEmpty(subStepDto.getDisCmdContent())) {
                String auxiliaryDecisionContentByCode = admCmdTypeService.getAuxiliaryDecisionContentByCode(subStepDto.getDisCmdContent());
                subStepDto.setDisCmdContent(auxiliaryDecisionContentByCode);
            }
        });
        return aidDesSubStepConvert.subStepDtoToEntity(subStepDtoListCopy);
    }

    /**
     * @Author yuelei
     * @Desc 获取推荐指令title
     * @Date 20:18 2022/6/13
     */
    public String getTitle(AlarmInfo alarmInfo, int step, int type) {
        int alarmTypeDetail = alarmInfo.getAlarmTypeDetail();
        DetailTypeDto detailTypeDto = aidDesStepCache.getIfPresent(alarmTypeDetail);
        List<StepDto> stepDtoList = detailTypeDto.getStepDtoList();
        List<StepDto> collect = stepDtoList.stream().filter(dto -> dto.getStep() == step && dto.getType() == type)
                .collect(Collectors.toList());
        String title = collect.get(0).getAidContent();
        //赋值title
        String alarmTypeDetailStr = admAlertDetailTypeService.getDescByCode(alarmTypeDetail);
        title = AidStepTitleEnum.replace(title, alarmInfo, alarmTypeDetailStr);
        return title;
    }

    /**
     * @param entities
     * @return java.util.List<java.lang.String>
     * @Description 获取推荐指令内容
     * @Author yuelei
     * @Date 2021/10/8 15:38
     */
    public List<DisposeDto> getStepList(List<AidDesSubStepEntity> entities, AlarmInfo alarmInfo) {
        //组装执行单元
        for (AidDesSubStepEntity entity : entities) {
            if (DesSubStepBeanConstant.SEND_DIS_CMD_CLEAR_PEOPLE.equals(entity.getBeanName())) {
                //站台名称
                String stationName = admStationService.selectByStationId(alarmInfo.getStationId()).getStationName();
                //上下行
                String upDown = alarmInfo.getUpDown() == IidsConstPool.TRAIN_UP ? "上行" : "下行";
                entity.setSubStepContent(String.format(entity.getSubStepContent(), alarmInfo.getOrderNum(), stationName, upDown));
                break;
            }
        }
        List<DisposeDto> dtoList = new ArrayList<>();
        entities.stream()
                .filter(entity -> entity.getSubStepType() != SubStepTypeConstant.HIDE_EXEC).forEach(entity -> {
                    String step = entity.getSubStepContent();
                    if (step.contains(ReplaceNameConstant.ALARM_STATION)) {
                        step = step.replaceAll(ReplaceNameConstant.ALARM_STATION, alarmInfo.getAlarmSite());
                    }
                    if (step.contains(ReplaceNameConstant.ORDER_NUM)) {
                        step = step.replaceAll(ReplaceNameConstant.ORDER_NUM, alarmInfo.getOrderNum());
                    }
                    if (step.contains(ReplaceNameConstant.RETRACE_NUM)) {
                        log.info("获取到折返车次信息为：{}", BasicCommonCacheUtils.get(Cache.RECOVERY_TRAIN_NUMBER));
                        step = step.replaceAll(ReplaceNameConstant.RETRACE_NUM, (String) BasicCommonCacheUtils.get(Cache.RECOVERY_TRAIN_NUMBER));
                    }
                    dtoList.add(new DisposeDto(step, entity.getOptType()));
                });
        return dtoList;
    }


    /**
     * @Author yuelei
     * @Desc 车门场景-替换几车厢几号车门描述
     * @Date `11:21` 2022/6/16
     */
    public void getTrainDoorStep(List<DisposeDto> stepList, String autoMsg) {
        //特殊处理全列车门故障
        String doorDesc;
        if(autoMsg.equals(StringConstant.ALL_TRAIN_DOOR_FAILURE)){
            doorDesc = StringConstant.ALL_TRAIN_DOOR_FAILURE;
        }else {
            //解析几车几号门描述
            StringBuilder sb = new StringBuilder();
            if (!StringUtils.isEmpty(autoMsg)) {
                String[] split = autoMsg.split(",");
                for (String str : split) {
                    String[] door = str.split("-");
                    sb.append(door[0]).append("车厢").append(door[1]).append("号门").append(",");
                }
            }
            //几车厢几号车门描述
            doorDesc = sb.substring(0, sb.length());
        }
        //替换车门相关描述
        for (DisposeDto dto : stepList) {
            String step = dto.getStep();
            if (step.contains(ReplaceNameConstant.DOOR_DESC)) {
                step = step.replaceAll(ReplaceNameConstant.DOOR_DESC, doorDesc);
                dto.setStep(step);
            }
        }
    }
}
