package com.tct.itd.adm.convert;

import com.tct.itd.adm.entity.AdmDispatchCmd;
import com.tct.itd.adm.entity.AdmDispatchCmdDTO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @Description
 * @Author zhaoke
 * @Date 2020/5/27 16:44
 **/
@Mapper(componentModel = "spring")
public interface AdmDisCmdConvert {

    AdmDispatchCmdDTO entityToDto(AdmDispatchCmd admDispatchCmd);

    AdmDispatchCmd dtoToEntity(AdmDispatchCmdDTO admDispatchCmdDTO);

    List<AdmDispatchCmdDTO> entityToDtoList(List<AdmDispatchCmd> listDisCmd);

    List<AdmDispatchCmd> dtoToEntityList(List<AdmDispatchCmdDTO> listDisCmd);
}
