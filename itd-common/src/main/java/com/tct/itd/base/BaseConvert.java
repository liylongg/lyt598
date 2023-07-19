package com.tct.itd.base;

import java.util.List;

/**
 * @Description 基类Convert
 * @Author zhaoke
 * @Date 2020-12-01 14:11
 */
public abstract class BaseConvert<TDto extends BaseDto,TEntity extends BaseEntity> {

    /**
     * 将实体转换为数据传输对象。
     *
     * @param entity 实体。
     * @return 数据传输对象。
     */
    public abstract TDto entityToDto(TEntity entity);

    /**
     * 将多个实体转换为数据传输对象。
     *
     * @param entities 实体列表。
     * @return 数据传输对象列表。
     */
    public abstract List<TDto> entitiesToDtos(List<TEntity> entities);

    /**
     * 将数据传输对象转换为实体。
     *
     * @param dto 数据传输对象。
     * @return 实体。
     */
    public abstract TEntity dtoToEntity(TDto dto);

    /**
     * 将多个数据传输对象转换为实体。
     *
     * @param dtos 数据传输对象列表。
     * @return 实体列表。
     */
    public abstract List<TEntity> dtosToEntities(List<TDto> dtos);

}
