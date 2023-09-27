package com.tct.itd.adm.msgRouter.router;


import com.tct.itd.dto.AuxiliaryDecision;
import org.springframework.core.Ordered;

/**
 * @Title: iids
 * @Description:
 * @Author: zhangyinglong
 * @Date:2021/5/18 10:15
 */
public interface AuxiliaryDecisionHandler{
    /**
     * 处理推荐指令
     * @param auxiliaryDecision
     * @throws RuntimeException
     */
    void handle(AuxiliaryDecision auxiliaryDecision) throws RuntimeException;

    /**
     * 用于映射推荐指令类型和handler
     * @return
     */
    String channel();

}
