package com.tct.itd.adm.convert;

import com.tct.itd.adm.entity.AdmAlertInfoUpgradeEntity;
import com.tct.itd.dto.AlarmInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdmAlertInfoUpgradeConvert {

    AdmAlertInfoUpgradeEntity dtoToEntity(AlarmInfo admAlertInfoUpgrade);

    AlarmInfo entityToDto(AdmAlertInfoUpgradeEntity entity);
}
