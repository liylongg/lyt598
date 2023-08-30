package com.tct.itd.adm.convert;

import com.tct.itd.dto.TrainGraphDto;
import com.tct.itd.xml.TrainGraph;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @Description
 * @Author zhaoke
 * @Date 2022/6/1 16:42
 */
@Mapper(componentModel = "spring")
public interface TrainGraphConvert {

    List<TrainGraph> dtosToXmls(List<TrainGraphDto> trainNumberAdjustList);

    TrainGraph dtoToXml(TrainGraphDto trainGraphDto);

    TrainGraphDto trainGraphDataToDto(Object trainGraphData);
}
