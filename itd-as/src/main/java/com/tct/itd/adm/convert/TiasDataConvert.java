package com.tct.itd.adm.convert;

import com.tct.itd.common.dto.TrainNumberAdjust;
import com.tct.itd.common.dto.TrainNumberAdjustDto;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @Description
 * @Author YHF
 * @Date 2020/5/27 16:44
 **/
@Mapper(componentModel = "spring")
public interface TiasDataConvert {

    List<TrainNumberAdjust> dtosToEntitys(List<TrainNumberAdjustDto> trainNumberAdjustList);

    TrainNumberAdjust dtoToEntity(TrainNumberAdjustDto trainNumberAdjustDto);
}
