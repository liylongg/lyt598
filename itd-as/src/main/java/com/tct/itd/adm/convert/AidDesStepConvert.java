package com.tct.itd.adm.convert;

import com.tct.itd.adm.entity.AidDesStepEntity;
import com.tct.itd.common.dto.AidDesStepDto;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @Description 执行单元对象转换
 * @Author yuelei
 * @Date 2021/9/14 11:32
 * @return
 */
@Mapper(componentModel = "spring")
public interface AidDesStepConvert {

    List<AidDesStepDto> entitiesToDtoList(List<AidDesStepEntity> entities);

    AidDesStepEntity dtoToEntity(AidDesStepDto dto);

}
