package com.tct.itd.adm.convert;

import com.tct.itd.adm.entity.AdmAlertInfoSubEntity;
import com.tct.itd.dto.AlarmInfo;
import org.mapstruct.Mapper;

/**
 * @Description 应急事件子表转换
 * @Author yuelei
 * @Date 2021/9/14 11:32
 * @return
 */
@Mapper(componentModel = "spring")
public interface AdmAlertInfoSubConvert {


    AdmAlertInfoSubEntity dtoToEntity(AlarmInfo alarmInfo);

    AlarmInfo entityToDto(AdmAlertInfoSubEntity entity);


}
