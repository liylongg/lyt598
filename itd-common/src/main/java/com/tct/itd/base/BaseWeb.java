package com.tct.itd.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @Description 基类web
 * @Author zhaoke
 * @Date 2020-12-01 14:05
 */
public class BaseWeb<TDto extends BaseDto,TService extends BaseService> {

    @Autowired
    protected TService service;

    @PostMapping
    public Integer save(@RequestBody TDto dto){
       return this.service.save(dto);
    }

    @PutMapping
    public Integer update(@RequestBody TDto dto){
        return this.service.update(dto);
    }

    @DeleteMapping()
    public Integer delete(@RequestParam("id") Long id){
        return this.service.delete(id);
    }

    @GetMapping("one")
    public TDto findOne(@RequestParam Map<String,Object> params){
        return (TDto)this.service.findOne(params);
    }

    @GetMapping("list")
    public List<TDto> list(@RequestParam Map<String,Object> params){
        return (List<TDto>)this.service.findAll(params);
    }

    @GetMapping("page")
    public IPage<TDto> page(@RequestParam("page") Integer page,@RequestParam("size") Integer size,@RequestParam Map<String,Object> params){
        return this.service.findByPage(page,size,params);
    }
}
