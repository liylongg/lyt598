package com.tct.itd.adm.msgRouter.router;

import com.tct.itd.adm.entity.AdmAlertInfo;
import com.tct.itd.adm.msgRouter.check.router.CheckConfigRouter;
import com.tct.itd.adm.service.AdmAlertInfoService;
import com.tct.itd.adm.service.AdmAlertInfoSubService;
import com.tct.itd.adm.util.AlarmUtil;
import com.tct.itd.adm.util.RedisKit;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.FireInfoPushAlertMsgConstant;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.TiasTraceInfo;
import com.tct.itd.common.enums.AlarmSourceEnum;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.enums.CodeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import com.tct.itd.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Description: 故障信息消息处理
 * @Author: zhangyinglong
 * @Date:2021/5/18 14:48
 */
@Component
@Slf4j
public class AlarmInfoRouter {

    @Resource
    private List<AlarmInfoMessageHandler> alarmInfoMessageHandlerList;

    private Map<String, AlarmInfoMessageHandler> alarmInfoMessageHandlerMap = new HashMap<>();

    @Resource
    private CheckConfigRouter checkConfigRouter;

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;

    @Resource
    private AdmAlertInfoService alertInfoService;

    /**
     * 数字常量0
     */
    public static final Integer ZERO = 0;
    /**
     * 数字常量1
     */
    public static final Integer ONE = 1;

    @Resource
    private AdmAlertInfoService admAlertInfoService;

    @Resource(name = "trainTraceCache")
    private com.github.benmanes.caffeine.cache.Cache<String, TiasTraceInfo> trainTraceCache;

    /**
     * @Description: 建立映射关系
     * @Author: zhangyinglong
     * @Date:2021/5/18 11:26
     * @return: void
     */
    @PostConstruct
    public void init() {
        alarmInfoMessageHandlerList.forEach(
                alarmInfoMessageHandler -> alarmInfoMessageHandlerMap.put(alarmInfoMessageHandler.channel(), alarmInfoMessageHandler)
        );
    }

    /**
     * @param alarmInfo
     * @description 根据终端录入车组号，设置车次号表号和故障地点
     * @date 2022/7/14 9:36
     * @author kangyi
     * @return: void
     */
    public void setAlarmInfoParam(AlarmInfo alarmInfo) {
        //设置故障来源为人工
        alarmInfo.setSource(AlarmSourceEnum.MAN_MADE.getType());
        //产生故障时，才设置table主键
        if (alarmInfo.getTableInfoId() == -1L || alarmInfo.getTableInfoId() == 0) {
            alarmInfo.setTableInfoId(UidGeneratorUtils.getUID());
        }
        if (StringUtils.isEmpty(alarmInfo.getTrainId()) || alarmInfo.getTrainId().equals("-1")) {
            return;
        }
        String trainId = alarmInfo.getTrainId();
        TiasTraceInfo traceInfo = trainTraceCache.getIfPresent(trainId);
        if (Objects.isNull(traceInfo)) {
            log.error("根据车组号【{}】获取车次追踪数据失败！", trainId);
            throw new BizException(CodeEnum.NO_GET_TRAIN_TRACE);
        }
        //只针对计划车
        if(traceInfo.getTrainType() != IidsConstPool.TRAIN_TYPE_PLAN){
            throw new BizException(CodeEnum.TRAIN_TYPE_NOT_PLAN);
        }
        //设置OrderNumber
        alarmInfo.setOrderNum(traceInfo.getOrderNumber());
        //设置上下行
        alarmInfo.setUpDown(traceInfo.getActiveEnd() == IidsConstPool.TRAIN_RUN_DIRECTION_UP ? IidsConstPool.TRAIN_UP : IidsConstPool.TRAIN_DOWN);
        //设置故障地点
        alertInfoService.setStationIdOrAreaId(traceInfo, alarmInfo);
        log.info("设置前台录入故障信息alarmInfo:{}", JsonUtils.toJSONString(alarmInfo));
    }


    /**
     * @Description: 人工故障上报消息处理
     * @Author: zhangyinglong
     * @Date:2021/5/18 11:27
     * @param: auxiliaryDecision
     * @return: void
     */
    public void alarmInfoRouter(AlarmInfo alarmInfo) {
        //默认为0
        alarmInfo.setFailureRecoveryStep(IidsConstPool.FAILURE_RECOVERY_STEP_0);
        //获取完整的故障信息
        boolean infoByEndLife = admAlertInfoSubService.getInfoByEndLife();
        //校验是否存在未处理故障
        if (alarmInfo.getAlarmState() == 1 && infoByEndLife) {
            throw new BizException(CodeEnum.ALARM_TYPE_NOT_FOUND);
        }
        //折返轨重复录入故障校验
        if (alarmInfo.getAlarmState() == 8 && infoByEndLife) {
            throw new BizException(CodeEnum.ALARM_TYPE_NOT_FOUND);
        }
        if (!infoByEndLife) {
            //故障录入校验
            checkConfigRouter.aidDecSubStepRouter(alarmInfo);
        }
        long start = System.currentTimeMillis();
        String channel = String.valueOf(alarmInfo.getAlarmTypeDetail());
        AlarmInfoMessageHandler alarmInfoMessageHandler = alarmInfoMessageHandlerMap.get(channel);
        if (alarmInfoMessageHandler == null) {
            log.error("未找到故障类型处理类:{}", JsonUtils.toJSONString(alarmInfo));
            throw new BizException(CodeEnum.ALARM_TYPE_UNHANDLED);
        }
        //录入故障，删除车站弹窗缓存
        BasicCommonCacheUtils.delKey(Cache.STATION_CONFIRM_IDS);
        try{
            //故障路由
            alarmInfoMessageHandler.handle(alarmInfo);
        } catch (BizException e) {
            this.giveUpAlarmInfo();
            throw new BizException(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.info("推送推荐指令出错：{}", e.getMessage(), e);
            this.giveUpAlarmInfo();
            throw new BizException(CodeEnum.SEND_AID_ERROR);
        }
        long spend = (System.currentTimeMillis() - start) / 1000;
        log.info("{} handler exec success, spend:{}s", channel, spend);
        //推送和利时
        AlarmUtil.sendFireInfoPush(alarmInfo, FireInfoPushAlertMsgConstant.ALARM_SEND_SUCCESS);
    }

    /**
     * @Author yuelei
     * @Desc 放弃处于生命周期的应急事件
     * @Date 11:04 2023/2/21
     */
    private void giveUpAlarmInfo(){
        //更新主表状态生命周期为结束
        AdmAlertInfo infoInLife = admAlertInfoService.getInfoInLife();
        if(infoInLife != null){
            admAlertInfoService.giveUp(infoInLife.getId());
            //设置生命周期置为false时，删除key；
            RedisKit.endLifeDeleteRedis();
        }
    }
}