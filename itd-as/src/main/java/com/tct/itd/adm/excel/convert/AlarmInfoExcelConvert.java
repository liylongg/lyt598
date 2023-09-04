package com.tct.itd.adm.excel.convert;

import com.tct.itd.excel.AlarmInfoExcel;
import com.tct.itd.common.dto.AdmAlertInfoDto;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @Description 导出应急事件
 * @Author kangyi
 * @Date 2022/9/7 11:32
 * @return
 */
@Mapper(componentModel = "spring")
public interface AlarmInfoExcelConvert {

    List<AlarmInfoExcel> dtosToExcel(List<AdmAlertInfoDto> entities);
}
