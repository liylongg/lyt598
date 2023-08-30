package com.tct.itd.adm.convert;

import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.common.dto.AidDesSubStepDto;
import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.common.dto.SubStepDto;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @Description 执行单元对象转换
 * @Author yuelei
 * @Date 2021/9/14 11:32
 * @return
 */
@Mapper(componentModel = "spring")
public interface AidDesSubStepConvert {

    List<AidDesSubStepOutDto> entitiesToDtoList(List<AidDesSubStepEntity> entities);

    AidDesSubStepEntity dtoToEntity(AidDesSubStepDto dto);

    List<AidDesSubStepEntity> subStepDtoToEntity(List<SubStepDto> subStepDto);
}
