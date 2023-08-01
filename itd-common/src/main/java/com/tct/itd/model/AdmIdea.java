package com.tct.itd.model;

import com.tct.itd.dto.AidDesSubStepOutDto;
import com.tct.itd.dto.AlarmInfo;
import com.tct.itd.dto.AuxiliaryDecision;
import com.tct.itd.dto.DisposeDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * @ClassName AdmOrder
 * @Description
 * @Author YHF
 * @Date 2020/11/18 14:37
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdmIdea {
    //推荐指令父表主键
    private long tableInfoId;
    //车组号
    private String trainId;
    //车次号
    private String orderNum;
    //故障开始时间,修改为 startAlarmTime
//    private String timestamp;
    //故障开始时间 zhangyinglong added on 2021/06/09
    private String startAlarmTime;
    //故障地点
    private String alarmSite;
    //故障类型
    private int alarmType;
    //故障类型文本信息 zhangyinglong added on 2021/05/31
    private String alarmTypeStr;
    //故障子类型
    private int alarmTypeDetail;
    //故障子类型文本信息 zhangyinglong added on 2021/05/31
    private String alarmTypeDetailStr;
    //报警源
    private List<DisposeDto> dispose;
    //站台表号编号
    private int stationId;
    //故障状态
    private int alarmState;
    //推荐指令执行步骤 0:放弃推荐指令 1:故障开始扣车推荐指令 2:故障告警超时,执行第二次调图推荐指令 3:故障恢复,抬车推荐指令(车未晚点,不需调图) 4:故障恢复,抬车推荐指令(车有晚点,需调图) 5:全列车门第三次推荐指令方案步骤
    private int executeStep;
    //推荐指令标题
    private String title;

    //执行单元对象集合
    private List<AidDesSubStepOutDto> aidDesSubStepDtoList;
    /**
     * zhangyinglong added on 2021/07/01
     * 大客流是否需要调图,如果不需要调图,则不用弹出预览运行图弹窗
     * 0:否，1:是
     */
    private int largePassFlowChangeGraph;
    /**
     * zhangyinglong added on 2021/07/01
     * 推送目标车站
     * 0:中心车站,非0则表示车站id
     */
    private int pushTargetStationId = 0;
    /**
     * 是否可执行的推荐指令 0 可以执行，1不能执行
     */
    private int executeFlag = 0;
    /**
     * 道岔名称
     */
    private String switchName;

    /**
     * 计轴名称
     */
    private String axleCounterName;

    /**
     * 牵引供电区段编号
     */
    private Integer tractionSectionId;
    /**
     * 是否自动上报 true 是
     */
    private Boolean autoReport;
    /**
     * 故障录入集中区(可能多个集中区id)
     */
    private String alarmConStation;

    /**
     * yhf added on 2021/07/06
     * 推荐指令弹框显示时间 默认30秒
     */
    private int showSecond = 30;


    /**
     * 是否故障恢复辅助决策（0：不是故障恢复 1：是故障恢复）
     */
    private int isRecovery = 0;

    public AdmIdea(long infoTableId, String trainId, String orderNum, String timestamp, String alarmSite, int alarmType, int alarmTypeDetail, List<DisposeDto> dispose, String vId, int stationId, int alarmState, int executeStep, String switchName, String axleCounterName,Integer tractionSectionId, Boolean autoReport, String alarmConStation) {
        this.tableInfoId = infoTableId;
        this.trainId = trainId;
        this.orderNum = orderNum;
//        this.timestamp = timestamp;
        this.startAlarmTime = timestamp;
        this.alarmSite = alarmSite;
        this.alarmType = alarmType;
        this.alarmTypeDetail = alarmTypeDetail;
        this.dispose = dispose;
        this.stationId = stationId;
        this.alarmState = alarmState;
        this.executeStep = executeStep;
        this.switchName = switchName;
        this.axleCounterName = axleCounterName;
        this.tractionSectionId = tractionSectionId;
        this.alarmConStation = alarmConStation;
    }

    public AdmIdea(long infoTableId, String trainId, String orderNum, String timestamp, String alarmSite, int alarmType, int alarmTypeDetail, List<DisposeDto> dispose,  int stationId, int alarmState, int executeStep) {
        this.tableInfoId = infoTableId;
        this.trainId = trainId;
        this.orderNum = orderNum;
//        this.timestamp = timestamp;
        this.startAlarmTime = timestamp;
        this.alarmSite = alarmSite;
        this.alarmType = alarmType;
        this.alarmTypeDetail = alarmTypeDetail;
        this.dispose = dispose;
        this.stationId = stationId;
        this.alarmState = alarmState;
        this.executeStep = executeStep;
    }

    /**
     * @param alarmInfo
     * @Description 获取调度命令对象，用于推送推荐指令
     * @Author: yuelei
     * @Date: 12:34 2021/8/16
     * @return: com.tct.model.vo.tias.push.AdmIdea
     **/
    public static AdmIdea getAdmIdeaForPush(AlarmInfo alarmInfo) {
        //创建推荐指令推送对象
        return new AdmIdea(alarmInfo.getTableInfoId(), alarmInfo.getTrainId(), alarmInfo.getOrderNum(), alarmInfo.getStartAlarmTime(),
                alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(), alarmInfo.getAlarmTypeDetail(), null, alarmInfo.getStationId(),
                alarmInfo.getAlarmState(), alarmInfo.getExecuteStep());
    }

    /**
     * 组装图送推荐指令对象
     *
     * @param alarmInfo 告警信息
     * @param stepList  推荐指令执行单元
     * @return com.tct.model.vo.tias.push.AdmIdea
     * @author liyunlong
     * @date 2021/12/30 15:59
     */
    public static AdmIdea getAdmIdeaForPush(AlarmInfo alarmInfo, List<DisposeDto> stepList) {
        AdmIdea admIdea = new AdmIdea();
        BeanUtils.copyProperties(alarmInfo, admIdea);
        admIdea.setDispose(stepList);
        return admIdea;
    }

    /**
     * @param
     * @Description 获取调度命令对象，用于插入推荐指令
     * @Author: yuelei
     * @Date: 10:38 2021/8/19
     * @return: com.tct.model.vo.tias.push.AdmIdea
     **/
    public static AdmIdea getAdmIdeaForAlertDetailBox(AlarmInfo alarmInfo, AuxiliaryDecision auxiliaryDecision) {
        return new AdmIdea(alarmInfo.getTableInfoId(), alarmInfo.getTrainId(), alarmInfo.getOrderNum(), alarmInfo.getStartAlarmTime(), alarmInfo.getAlarmSite(), alarmInfo.getAlarmType(), alarmInfo.getAlarmTypeDetail(), auxiliaryDecision.getStepList(), alarmInfo.getStationId(), alarmInfo.getAlarmState(), auxiliaryDecision.getExecuteStep());
    }
}
