package com.tct.itd.adm.convert;

import com.tct.itd.common.dto.StatisticsAlgorithmData;
import com.tct.itd.common.dto.StatisticsAlgorithmDataDto;
import org.mapstruct.Mapper;

/**
 * @Author yuelei
 * @Desc 调图后算法统计
 * @Date 9:40 2022/8/29
 */
@Mapper(componentModel = "spring")
public interface StatisticsAlgorithmDataConvert {

    StatisticsAlgorithmData dtoToVo(StatisticsAlgorithmDataDto dto);
}
