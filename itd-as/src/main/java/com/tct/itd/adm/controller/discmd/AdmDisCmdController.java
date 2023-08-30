package com.tct.itd.adm.controller.discmd;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tct.itd.adm.entity.AdmDispatchCmd;
import com.tct.itd.adm.entity.AdmDispatchCmdDTO;
import com.tct.itd.adm.service.AdmDisCmdService;
import com.tct.itd.common.dto.AdmDispatchCmdParam;
import com.tct.itd.common.dto.SignCommandDto;
import com.tct.itd.restful.BaseResponse;
import com.tct.itd.util.CommandCodeUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * (AdmDispatchCommand)表控制层
 *
 * @author Liyuanpeng
 * @version 1.0
 * @date 2020-11-12 15:03:54
 */
@RestController
@RequestMapping("/adm_dispatch")
public class AdmDisCmdController {
    /**
     * 服务对象
     */
    @Resource
    private AdmDisCmdService admDisCmdService;

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     * @date 2020-11-12 15:03:54
     */
    @GetMapping("/findById")
    public AdmDispatchCmdDTO findById(@RequestParam(value = "id") Long id) {
        return admDisCmdService.selectById(id);
    }

    /**
     * 插入数据
     *
     * @param admDispatchCmdDTO 插入对象
     * @return 影响条数
     * @date 2020-11-13 15:03:54
     */
    @PostMapping("/save")
    public int save(@RequestBody AdmDispatchCmdDTO admDispatchCmdDTO) {
        return admDisCmdService.insert(admDispatchCmdDTO);
    }

    /**
     * 根据 map集合 条件，查询全部记录
     *
     * @param admDispatchCmdParam 条件
     * @return List集合记录
     * @date 2020-11-128 15:03:54
     */
    @PostMapping("/findDisCmdList")
    public Page<AdmDispatchCmdDTO> findDisCmdList(@RequestBody AdmDispatchCmdParam admDispatchCmdParam) {
        //分页参数
        int current = !Objects.isNull(admDispatchCmdParam.getCurrent()) ? admDispatchCmdParam.getCurrent() : 1;
        int size = !Objects.isNull(admDispatchCmdParam.getSize()) ? admDispatchCmdParam.getSize() : 10;
        Page<AdmDispatchCmd> page = new Page<>(current, size);
        return admDisCmdService.selectList(admDispatchCmdParam, page);
    }

    /**
     * @param signCommandDto
     * @return com.tct.iids.entity.AdmDispatchCmdDTO
     * @Description 签收调度命令
     * @Author yuelei
     * @Date 2021/9/9 15:34
     */
    @PostMapping("/modifyById")
    public AdmDispatchCmdDTO modifyById(@RequestBody SignCommandDto signCommandDto) {
        return admDisCmdService.updateById(signCommandDto);
    }

    /**
     * 获取commandCode 命令编号
     *
     * @return commandCode 编号
     * @date 2020-11-26 15:03:54
     */
    @GetMapping("/getCommandCode")
    public BaseResponse<String> getCommandCode() {
        return BaseResponse.success(CommandCodeUtil.getCommandCode());
    }


}
