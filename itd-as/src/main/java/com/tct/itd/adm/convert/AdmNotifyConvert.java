package com.tct.itd.adm.convert;

import com.tct.itd.adm.entity.NotifyEntity;
import com.tct.itd.common.dto.Notify;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdmNotifyConvert {

    NotifyEntity dtoToEntity(Notify notify);

}
