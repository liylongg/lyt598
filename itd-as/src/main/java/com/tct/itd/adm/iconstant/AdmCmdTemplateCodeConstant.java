package com.tct.itd.adm.iconstant;

/**
 * 自动下发调度命令模板编码常量类
 * @author yuelei
 */
public class AdmCmdTemplateCodeConstant {
    /**
     * 停运-清客直接回库
     */
    public static final int TRAIN_STOP_BACK_DEPOT = 901;
    /**
     * 停运-清客放空运行回库
     */
    public static final int TRAIN_STOP_EMPTY_BACK_DEPOT = 903;
    /**
     * 停运折返
     */
    public static final int TRAIN_STOP_RETURN_BACK = 902;
    /**
     * 掉线
     */
    public static final int TRAIN_DROP = 1;
    /**
     * 掉线
     */
    public static final int TURN_BACK_DEPOT = 101;
    /**
     * 加开车次-起始车站ID一致
     */
    public static final int TRAIN_ADD_SAME = 201;


    /**
     * 清人后直接回库/清人后折返加开一个回库
     */
    public static final int DROP_LINE_1 = 901;

    /**
     * 清人后折返加开两个车次回库
     */
    public static final int DROP_LINE_2 = 902;

    /**
     * 清人后立即入库
     */
    public static final int DROP_LINE_5 = 905;

    /**
     * 终点直接回库
     */
    public static final int END_DROP_LINE_6 = 906;

    /**
     * 终点折返后加开一个车次回库/终点折返后加开两个车次回库
     */
    public static final int END_DROP_LINE_7 = 907;

    /**
     * 清人后折返加开一个车次回库
     */
    public static final int DROP_LINE_8 = 908;

    /**
     * 折返区间（不涉及清人站台的位置）掉线直接回库
     */
    public static final int DROP_LINE_9 = 909;

    /**
     * 折返区间（不涉及清人站台的位置）掉线直接回库
     */
    public static final int DROP_LINE_10 = 910;

    /**
     * 折返区间（不涉及清人站台的位置）掉线直接回库
     */
    public static final int DROP_LINE_11 = 911;

    /**
     * 折返区间（不涉及清人站台的位置）掉线直接回库(不经过任何站台)
     */
    public static final int DROP_LINE_12 = 912;

    /**
     * 折返区间（不涉及清人站台的位置）掉线后折返回库(加开二个车次)(不经过任何站台)
     */
    public static final int DROP_LINE_13 = 913;

    /**
     * 折返区间（不涉及清人站台的位置）掉线后折返回库(加开一个车次)(不经过任何站台)
     */
    public static final int DROP_LINE_14 = 914;

    /**
     * 第一个车次直接开始载客
     */
    public static final int ADD_1 = 201;

    /**
     * 第一个车次中间站开始载客
     */
    public static final int ADD_2 = 202;

    /**
     * 第一个车次放空，第二个车次直接开始载客
     */
    public static final int ADD_3 = 203;

    /**
     * 第一个车次放空，第二个车次中间站开始载客
     */
    public static final int ADD_4 = 204;

    /**
     * 最后一个车次载客回库
     */
    public static final int ADD_5 = 205;

    /**
     * 加开一个载客车次替开
     */
    public static final int ADD_6 = 206;

    /**
     * 加开一个放空车次替开
     */
    public static final int ADD_7 = 207;

    /**
     * 加开两个放空车次，第三个车次直接载客
     */
    public static final int ADD_8 = 208;

    /**
     * 加开两个放空车次，第三个车次中途载客
     */
    public static final int ADD_9 = 209;

}
