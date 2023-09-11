package com.tct.itd.adm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tct.itd.common.dto.AdmStation;
import org.apache.ibatis.annotations.Mapper;

/**
 * (AdmStation)表数据库访问层
 * @author Liyuanpeng
 * @version 1.0
 * @date 2020-11-12 15:00:43
 */
@Mapper
public interface AdmStationMapper extends BaseMapper<AdmStation> {

}