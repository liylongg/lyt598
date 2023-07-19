package com.tct.itd.base;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tct.itd.base.eumn.BaseQueryOperate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Description 基类service
 * @Author zhaoke
 * @Date 2020-12-01 14:07
 */
public abstract class BaseService<TDto extends BaseDto,TEntity extends BaseEntity,TConvert extends BaseConvert<TDto,TEntity>,TMapper extends IidsBaseMapper<TEntity>> {

    @Autowired
    protected TConvert convert;

    @Autowired
    protected TMapper mapper;

    public Integer save(TDto dto) {
        TEntity entity = this.convert.dtoToEntity(dto);
        return mapper.insert(entity);
    }

    public Integer update(TDto dto) {
        TEntity entity = this.convert.dtoToEntity(dto);
        return this.mapper.updateById(entity);
    }

    public Integer delete(Long id){
       return this.mapper.deleteById(id);
    }

    public TDto findOne(Map<String, Object> params) {
        QueryWrapper<TEntity> wrapper = this.assemblyParams(params);
        return this.convert.entityToDto(this.mapper.selectOne(wrapper));
    }

    public List<TDto> findAll(Map<String, Object> params) {
        QueryWrapper<TEntity> wrapper = this.assemblyParams(params);
        return this.convert.entitiesToDtos(this.mapper.selectList(wrapper));
    }

    public IPage<TDto> findByPage(Integer page,Integer size, Map<String, Object> params) {
        params.remove("page");
        params.remove("size");
        Page<TEntity> entityPage = new Page<>(page,size);
        QueryWrapper<TEntity> wrapper;
        if(CollectionUtils.isEmpty(params)){
            wrapper = new QueryWrapper<TEntity>().orderByDesc("ts");
        }else {
            wrapper = this.assemblyParams(params);
        }
        entityPage = this.mapper.selectPage(entityPage, wrapper);
        Page<TDto> pageAble = new Page<>(page,size);
        pageAble.setRecords(this.convert.entitiesToDtos(entityPage.getRecords()));
        pageAble.setTotal(entityPage.getTotal());
        return pageAble;
    }

    /**
     * 组装查询参数
     * @param params 查询参数
     * @return Wrapper
     */
    private QueryWrapper<TEntity> assemblyParams(Map<String, Object> params) {
        Assert.notEmpty(params,"参数不能为空");
        QueryWrapper<TEntity> wrapper = new QueryWrapper<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            if(!key.contains("@")){
                throw new RuntimeException("参数格式错误，必须遵循 【column@operate】格式");
            }
            String column = key.split("@")[0];
            String operate = key.split("@")[1];
            Object value = entry.getValue();
            if(value == null){
                throw new RuntimeException("参数格式错误，value不能为null");
            }
            //前置查询对象组装
            this.preAssemblyWrapper(wrapper);
            this.assemblyWrapper(wrapper,column,operate,value);
            //后置查询对象组装
            this.afterAssemblyWrapper(wrapper);
        }
        return wrapper;
    }


    /**
     * 构建查询条件
     * @param wrapper 查询条件对象
     * @param column 字段名
     * @param operate 操作符
     * @param value 查询值
     */
    private void assemblyWrapper(QueryWrapper<TEntity> wrapper, String column, String operate, Object value) {
        //等于
        if(BaseQueryOperate.EQ.equals(operate)){
            wrapper.eq(column, value);
        }
        //不等于
        if(BaseQueryOperate.NE.equals(operate)){
            wrapper.ne(column, value);
        }
        //大于
        if(BaseQueryOperate.GT.equals(operate)){
            wrapper.gt(column,value);
        }
        //大于等于
        if(BaseQueryOperate.GE.equals(operate)){
            wrapper.ge(column,value);
        }
        //小于
        if(BaseQueryOperate.LT.equals(operate)){
            wrapper.lt(column,value);
        }
        //小于等于
        if(BaseQueryOperate.LE.equals(operate)){
            wrapper.le(column,value);
        }
        //模糊%%
        if(BaseQueryOperate.LIKE.equals(operate)){
            wrapper.like(column,value);
        }
        //模糊%%取反
        if(BaseQueryOperate.NOT_LIKE.equals(operate)){
            wrapper.notLike(column,value);
        }
        //左模糊%s
        if(BaseQueryOperate.LIKE_LEFT.equals(operate)){
            wrapper.likeLeft(column,value);
        }
        //右模糊s%
        if(BaseQueryOperate.LIKE_RIGHT.equals(operate)){
            wrapper.likeRight(column,value);
        }
        //范围与不在范围
        if(BaseQueryOperate.BETWEEN.equals(operate) || BaseQueryOperate.NOT_BETWEEN.equals(operate)){
            if(!value.toString().contains("@")){
                throw new RuntimeException("使用between和notBetween操作符,value值必须是[value1@value2]格式");
            }
            String start = value.toString().split("@")[0];
            String end = value.toString().split("@")[1];
            if (BaseQueryOperate.BETWEEN.equals(operate)) {
                wrapper.between(column, start, end);
            } else {
                wrapper.notBetween(column, start, end);
            }
        }
        //in
        if(BaseQueryOperate.IN.equals(operate) || BaseQueryOperate.NOT_IN.equals(operate)){
            if(!value.toString().contains(",")){
                throw new RuntimeException("使用in操作符,value值必须是[value1,value2.....]");
            }
            List<String> list = new ArrayList<>(Arrays.asList(value.toString().split(",")));
            if(BaseQueryOperate.IN.equals(operate)){
                wrapper.in(column,list);
            }else {
                wrapper.notIn(column,list);
            }

        }
        //排序
        if(BaseQueryOperate.ORDER_BY.equals(operate)){
            if(value.toString().equals(BaseQueryOperate.ASC)){
                wrapper.orderByAsc(column);
            }else {
                wrapper.orderByDesc(column);
            }
        }else {
            wrapper.orderByDesc("ts");
        }
    }

    /**
     * 前置查询对象组装
     * @param wrapper 查询对象
     */
    public void preAssemblyWrapper(QueryWrapper<TEntity> wrapper) {
    }

    /**
     * 后置查询对象组装
     * @param wrapper 查询对象
     */
    public void afterAssemblyWrapper(QueryWrapper<TEntity> wrapper) {
    }

}
