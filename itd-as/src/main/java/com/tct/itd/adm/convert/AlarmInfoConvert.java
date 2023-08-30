package com.tct.itd.adm.convert;

import com.tct.itd.adm.entity.AdmAlertInfo;
import com.tct.itd.common.dto.AdmAlertInfoDto;
import com.tct.itd.common.dto.AdmRunGraphCases;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @Description 应急事件子表转换
 * @Author yuelei
 * @Date 2021/9/14 11:32
 * @return
 */
@Mapper(componentModel = "spring")
public interface AlarmInfoConvert {

    AuxiliaryDecision infoToAuxiliary(AlarmInfo alarmInfo);

    AdmRunGraphCases alarmInfoToGraphCases(AlarmInfo alarmInfo);

    AdmAlertInfoDto admAlertInfoToDto(AdmAlertInfo admAlertInfo);

    List<AdmAlertInfoDto> admAlertInfosToDtos(List<AdmAlertInfo> alarmInfo);
}
