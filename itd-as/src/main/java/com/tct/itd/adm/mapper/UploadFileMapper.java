package com.tct.itd.adm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tct.itd.adm.entity.AdmFlowchartDataEntity;
import com.tct.itd.adm.entity.UploadFileEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author kangyi
 * @date 2022-11-11 10:08:06
 * @desc 上传文件
 */
@Mapper
public interface UploadFileMapper extends BaseMapper<UploadFileEntity> {

}
