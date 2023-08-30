package com.tct.itd.adm.controller.adm;

import com.tct.itd.adm.entity.AlarmSourceDto;
import com.tct.itd.common.enums.AlarmSourceEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author kangyi
 * @description 报警源
 * @date 2022年 06月11日 14:41:10
 */
@Slf4j
@RestController()
@RequestMapping("/alarmSource")
public class AlarmSourceController {

    /**
     * @description
     * @date 2022/6/11 14:42
     * @author kangyi
     * @return: java.util.List<com.tct.itd.common.dto.AidDesStepDto>
     */
    @GetMapping("/getAll")
    public List<AlarmSourceDto> getAll() {
        List<AlarmSourceDto> retList = new ArrayList<>();
        Arrays.stream(AlarmSourceEnum.values()).forEach(alarmSourceEnum -> {
            AlarmSourceDto alarmSourceDto = new AlarmSourceDto();
            alarmSourceDto.setAlarmType(String.valueOf(alarmSourceEnum.getType()));
            alarmSourceDto.setAlarmName(alarmSourceEnum.getName());
            retList.add(alarmSourceDto);
        });
        return retList;
    }
}
