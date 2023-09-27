package com.tct.itd.adm.msgRouter.service;

import com.google.common.collect.Lists;
import com.tct.itd.adm.convert.AidDesSubStepConvert;
import com.tct.itd.adm.entity.AdmAlertDetail;
import com.tct.itd.adm.entity.AdmAlertDetailBox;
import com.tct.itd.adm.entity.AidDesSubStepEntity;
import com.tct.itd.adm.iconstant.AlarmInfoEnum;
import com.tct.itd.adm.iconstant.AlarmTypeConstant;
import com.tct.itd.adm.iconstant.CommandEnum;
import com.tct.itd.adm.service.*;
import com.tct.itd.adm.util.AidDesSubStepUtils;
import com.tct.itd.common.cache.Cache;
import com.tct.itd.common.constant.IidsConstPool;
import com.tct.itd.common.dto.Info;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.DisposeDto;
import com.tct.itd.enums.MsgTypeEnum;
import com.tct.itd.exception.BizException;
import com.tct.itd.model.AdmIdea;
import com.tct.itd.util.TitleUtil;
import com.tct.itd.utils.BasicCommonCacheUtils;
import com.tct.itd.utils.JsonUtils;
import com.tct.itd.utils.UidGeneratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/***
 * @Author yuelei
 * @Desc 车门车站弹窗，处理逻辑
 * @Date 14:44 2022/9/21
 */
@Slf4j
@Service
public class TrainDoorAlarmService {

    @Resource
    private AdmAlertInfoSubService admAlertInfoSubService;
    @Resource
    private AdmAlertDetailService admAlertDetailService;
    @Resource
    private AidDesSubStepUtils aidDesSubStepUtils;
    @Resource
    private AdmAlertDetailTypeService admAlertDetailTypeService;
    @Resource
    private AdmAlertDetailBoxService admAlertDetailBoxService;
    @Resource
    private AidDesSubStepConvert aidDesSubStepConvert;
    @Resource
    private AppPushService appPushService;
    @Resource
    private AlarmFlowchartService alarmFlowchartService;

    public void dealStationConfirm(AlarmInfo alarmInfo, Integer code) {
        //故障类型
        int alarmTypeDetail = alarmInfo.getAlarmTypeDetail();
        if (AlarmTypeConstant.ONLY_DOOR_CANNOT_OPEN.equals(String.valueOf(alarmTypeDetail))) {
            BasicCommonCacheUtils.delKey(Cache.TRAIN_DOOR_AUTO_CHECK_RECOVERY);
            log.info("车门故障 - 单车门无法打开 生成第二次推荐指令提示:{}", JsonUtils.toJSONString(alarmInfo));
            //车门故障告警超时 继续执行超时推荐指令
            long detailId = UidGeneratorUtils.getUID();
            AdmAlertDetail alertDetail3 = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令", new Date(), "系统产生运行图推荐指令", "1", 1, System.currentTimeMillis());
            admAlertDetailService.insert(alertDetail3);
            //查询故障决策指令
            List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), 2, 0);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第二步
            alarmInfo.setExecuteStep(2);
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
            admAlertDetailBoxService.insert(alertDetailBox);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            alarmInfo.setTableBoxId2(boxId);
            //设置状态未执行
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
            admAlertInfoSubService.updateById(alarmInfo);
            log.info("车门故障 - 单车门无法打开,生成第二次推荐指令提示成功");
            //更新流程图
            if(code == 1){
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
                    add(8);
                    add(15);
                }});
            }else{
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
                    add(7);
                    add(10);
                }});
            }
        } else if (AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE.equals(String.valueOf(alarmTypeDetail))) {
            BasicCommonCacheUtils.delKey(Cache.TRAIN_DOOR_AUTO_CHECK_RECOVERY);
            log.info("车门故障 - 单车门无法关闭 生成第二次推荐指令提示:{}", JsonUtils.toJSONString(alarmInfo));
            //车门故障告警超时 继续执行超时推荐指令
            long detailId = UidGeneratorUtils.getUID();
            //应急事件列表记录title
            String title = "第二次推荐指令";
            //推荐指令步骤执行第二步
            int step = code + 1;
            if (step == IidsConstPool.EXECUTE_STEP_5) {
                if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_4) {
                    alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_5);
                    //设置折返恢复车次到缓存
                    //onlyDoorCannotCloseStrategy.getRetraceNumber(alarmInfo);
                    title = "第四次推荐指令";
                    //删除抬车后取消扣车标志
                    BasicCommonCacheUtils.delKey(Cache.CHANGE_GRAPH_CANCEL_HOLD_TRAIN_FLAG);
                } else {
                    alarmInfo.setExecuteStep(IidsConstPool.EXECUTE_STEP_6);
                    step = IidsConstPool.EXECUTE_STEP_6;
                    title = "第三次推荐指令";
                    BasicCommonCacheUtils.delKey(Cache.DOOR_TIME_ADJUST_CASE_FLAG);
                }
            } else {
                alarmInfo.setExecuteStep(step);
                alarmInfo.setCaseCode(0);
            }
            AdmAlertDetail alertDetail3 = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), title, new Date(), "系统产生运行图推荐指令", "1", 1, System.currentTimeMillis());
            admAlertDetailService.insert(alertDetail3);
            //查询故障决策指令
            List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 0);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            long boxId = UidGeneratorUtils.getUID();
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            admIdea.setTitle(aidDesSubStepUtils.getTitle(alarmInfo, step, 0));
            //推荐指令入库
            AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
            admAlertDetailBoxService.insert(alertDetailBox);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            alarmInfo.setTableBoxId2(boxId);
            //设置状态未执行
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
            admAlertInfoSubService.updateById(alarmInfo);
            log.info("车门故障 - 单车门无法关闭,生成第二次推荐指令提示成功");
            //  code =1 : 车门经人工处置后，手动关闭已隔离;人工处置后可具备运营条件
            //  code =2 : 车门经人工处置后，车门仍无法关闭;人工处置后不具备运营条件
            if (Objects.equals(code, 2)) {
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(8,11));
            }else if (Objects.equals(code, 1)){
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(9,26));
            }else if(step == IidsConstPool.EXECUTE_STEP_5){
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(19,20));
            }
        } else if (AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_OPEN.equals(String.valueOf(alarmTypeDetail))) {
            BasicCommonCacheUtils.delKey(Cache.TRAIN_DOOR_AUTO_CHECK_RECOVERY);
            log.info("车门故障 - 全列无法打开无法打开 生成第二次推荐指令提示:{}", JsonUtils.toJSONString(alarmInfo));
            //车门故障告警超时 继续执行超时推荐指令
            long detailId = UidGeneratorUtils.getUID();
            AdmAlertDetail alertDetail3 = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), "第二次推荐指令", new Date(), "系统产生运行图推荐指令", "1", 1, System.currentTimeMillis());
            admAlertDetailService.insert(alertDetail3);
            //查询故障决策指令
            List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), 2, 0);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第二步
            alarmInfo.setExecuteStep(2);
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
            admAlertDetailBoxService.insert(alertDetailBox);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            alarmInfo.setTableBoxId2(boxId);
            //设置状态未执行
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
            admAlertInfoSubService.updateById(alarmInfo);
            log.info("车门故障 - 全列车门无法打开,生成第二次推荐指令提示成功");
            alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
                add(7);
                add(10);
            }});
        } else if (AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE.equals(String.valueOf(alarmTypeDetail))) {
            BasicCommonCacheUtils.delKey(Cache.TRAIN_DOOR_AUTO_CHECK_RECOVERY);
            //车门故障告警超时 继续执行超时推荐指令
            long detailId = UidGeneratorUtils.getUID();
            String title = "第二次推荐指令";
            int step = 0;
            if (code == 1) {
                step = 2;
            } else if (code == 2) {
                step = 4;
            }
            //如果executeStep为6，则需要替换折返车次
            if (code == 7) {
                if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_6) {
                    step = IidsConstPool.EXECUTE_STEP_7;
                    //设置折返恢复车次到缓存
                    //allDoorCannotCloseStrategy.getRetraceNumber(alarmInfo);
                    //删除抬车后取消扣车标志
                    BasicCommonCacheUtils.delKey(Cache.CHANGE_GRAPH_CANCEL_HOLD_TRAIN_FLAG);
                    title = "第五次推荐指令";
                } else {
                    step = IidsConstPool.EXECUTE_STEP_8;
                    if (alarmInfo.getExecuteStep() == IidsConstPool.EXECUTE_STEP_5) {
                        title = "第四次推荐指令";
                    } else {
                        title = "第三次推荐指令";
                    }
                    BasicCommonCacheUtils.delKey(Cache.DOOR_TIME_ADJUST_CASE_FLAG);
                }
            }
            AdmAlertDetail alertDetail3 = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), title, new Date(), "系统产生运行图推荐指令", "1", 1, System.currentTimeMillis());
            admAlertDetailService.insert(alertDetail3);
            log.info("车门故障 - 全列无法关闭无法打开 生成第" + step + "次推荐指令提示:{}", JsonUtils.toJSONString(alarmInfo));
            //查询故障决策指令
            List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 0);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            long boxId = UidGeneratorUtils.getUID();
            //推荐指令步骤执行第二步
            alarmInfo.setExecuteStep(step);
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            admIdea.setTitle(aidDesSubStepUtils.getTitle(alarmInfo, step, 0));
            //推荐指令入库
            AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
            admAlertDetailBoxService.insert(alertDetailBox);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            alarmInfo.setTableBoxId2(boxId);
            //设置状态未执行
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
            admAlertInfoSubService.updateById(alarmInfo);
            log.info("车门故障 - 全列车门无法关闭,生成第二次推荐指令提示成功");
            if( step == 2 ){
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
                    add(11);
                    add(8);
                }});
            }else if( step == 4){
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
                    add(9);
                    add(23);
                }});
            }else if( step == IidsConstPool.EXECUTE_STEP_7){
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), new ArrayList<Integer>() {{
                    add(51);
                    add(35);
                }});
            }
        } else if (AlarmTypeConstant.ONLY_DOOR_CANNOT_CLOSE_SLOW_DOWN.equals(String.valueOf(alarmTypeDetail))) {
            BasicCommonCacheUtils.delKey(Cache.TRAIN_DOOR_AUTO_CHECK_RECOVERY);
            log.info("车门故障 - 单车门无法关闭-缓行算法 生成第二次推荐指令提示:{}", JsonUtils.toJSONString(alarmInfo));
            //车门故障告警超时 继续执行超时推荐指令
            long detailId = UidGeneratorUtils.getUID();
            //应急事件列表记录title
            String title = "第二次推荐指令";
            //推荐指令步骤执行第二步
            alarmInfo.setExecuteStep(code + 1);
            alarmInfo.setCaseCode(0);
            AdmAlertDetail alertDetail3 = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), title, new Date(), "系统产生运行图推荐指令", "1", 1, System.currentTimeMillis());
            admAlertDetailService.insert(alertDetail3);
            //查询故障决策指令
            List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), code + 1, 0);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            long boxId = UidGeneratorUtils.getUID();
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
            admAlertDetailBoxService.insert(alertDetailBox);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            alarmInfo.setTableBoxId2(boxId);
            //设置状态未执行
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
            admAlertInfoSubService.updateById(alarmInfo);
            log.info("车门故障 - 单车门无法关闭-缓行,生成第二次推荐指令提示成功");
            if(code == 1){
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(8,22));
            }else{
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(7,10));
            }
        } else if (AlarmTypeConstant.ALL_TRAIN_DOOR_CANNOT_CLOSE_SLOW_DOWN.equals(String.valueOf(alarmTypeDetail))) {
            BasicCommonCacheUtils.delKey(Cache.TRAIN_DOOR_AUTO_CHECK_RECOVERY);
            log.info("车门故障 - 全列车门无法关闭-缓行算法 生成第二次推荐指令提示:{}", JsonUtils.toJSONString(alarmInfo));
            //车门故障告警超时 继续执行超时推荐指令
            long detailId = UidGeneratorUtils.getUID();
            //应急事件列表记录title
            String title = "第二次推荐指令";
            int step = 0;
            if (code == 1) {
                step = 2;
            } else if (code == 2) {
                step = 4;
            }
            //推荐指令步骤执行第二步
            alarmInfo.setExecuteStep(step);
            alarmInfo.setCaseCode(0);
            AdmAlertDetail alertDetail3 = new AdmAlertDetail(detailId, alarmInfo.getTableInfoId(), title, new Date(), "系统产生运行图推荐指令", "1", 1, System.currentTimeMillis());
            admAlertDetailService.insert(alertDetail3);
            //查询故障决策指令
            List<AidDesSubStepEntity> entities = aidDesSubStepUtils.getAidDesSubStep(alarmInfo.getAlarmTypeDetail(), step, 0);
            //定义推荐指令执行单元
            List<DisposeDto> stepList = aidDesSubStepUtils.getStepList(entities, alarmInfo);
            long boxId = UidGeneratorUtils.getUID();
            //向push推送推荐指令指令 等待指令执行
            AdmIdea admIdea = AdmIdea.getAdmIdeaForPush(alarmInfo, stepList);
            admIdea.setAlarmTypeStr(AdmAlertDetailTypeService.getDescription(alarmInfo.getAlarmType()));
            admIdea.setAlarmTypeDetailStr(admAlertDetailTypeService.getDescByCode(admIdea.getAlarmTypeDetail()));
            admIdea.setAidDesSubStepDtoList(aidDesSubStepConvert.entitiesToDtoList(entities));
            admIdea.setTitle(TitleUtil.getTitle(alarmInfo));
            //推荐指令入库
            AdmAlertDetailBox alertDetailBox = new AdmAlertDetailBox(boxId, detailId, JsonUtils.toJSONString(admIdea), "方案待执行");
            admAlertDetailBoxService.insert(alertDetailBox);
            Info admInfo = new Info(CommandEnum.ADM_IDEA.getcmdId(), admIdea);
            appPushService.sendMessage(MsgTypeEnum.REPORTED_MSG, admInfo);
            alarmInfo.setTableBoxId2(boxId);
            //设置状态未执行
            alarmInfo.setExecuteEnd(AlarmInfoEnum.EXECUTE_END_0.getCode());
            admAlertInfoSubService.updateById(alarmInfo);
            log.info("车门故障 - 全列车门无法关闭-缓行,生成第二次推荐指令提示成功");
            if(code == 1){
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(7,10));
            }else{
                alarmFlowchartService.setExecuteFlags(alarmInfo.getTableInfoId(), Lists.newArrayList(8,36));
            }
        } else {
            throw new BizException("未知得故障类型");
        }
    }
}
