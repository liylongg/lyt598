package com.tct.itd.adm.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Description: 应急事件流程图节点对象
 * @Author: yuzhenxin
 * @CreateTime: 2022-07-22  17:52
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class FlowchartNode {

    private Integer id;

    private String name;

    private List<FlowchartNode> group;

    private List<FlowchartNode> child;

    private Integer parentId;

    private String describe1;

    private String describe2;

}
