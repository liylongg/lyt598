package com.tct.itd.adm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tct.itd.adm.entity.AdmDispatchCmd;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * (AdmDispatchCommand)表数据库访问层
 * @author Liyuanpeng
 * @version 1.0
 * @date 2020-11-12 15:03:54
 */
@Mapper
public interface AdmDisCmdMapper extends BaseMapper<AdmDispatchCmd> {

    List<AdmDispatchCmd> selectList(Map<String, Object> params);
}