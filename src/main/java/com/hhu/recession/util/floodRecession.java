package com.hhu.recession.util;
import com.alibaba.fastjson.JSONObject;

import java.text.DecimalFormat;
import java.util.*;

/**
 * flood recession:
 * 通过此类来描述一个场次的洪水退水过程
 *
 * --- by jiamin.luu@hhu.edu.cn
 */
public class floodRecession {

    String id;
    List floodDischarges;           //泄洪流量的观测序列
    int moniterSize;                //观测序列长度
    double recDischarge;            //人为设定的退水流量值
    double recInstant;              //退水曲线与退水流量的交汇时刻
    boolean legal;                //标识当前退水序列是否有效

    ArrayList<Map.Entry<Double, Double>> dischargePairs;            //连续的流量观测值对
    Map<Long, Double> translatedDischarges;                         //根据平均退水时刻平移后得到的退水曲线
    Long translatedInstant_min;                                     //平移后的起始时刻
    Long translatedInstant_max;                                     //平移后的终止时刻
    Map.Entry<Double, Double> closestPair;                          //最接近设定退水位的观测值；
    Map.Entry<Integer, Integer> closetInterval;                     //最接近设定退水位的时段


    public floodRecession() {
        id = "退水曲线";
        floodDischarges = new ArrayList<Double>();
        moniterSize = 0;

        recDischarge = -1.0;
        closestPair = new AbstractMap.SimpleEntry<>(0.0, 0.0);
        closetInterval = new AbstractMap.SimpleEntry<>(0, 0);
        translatedDischarges = new HashMap<>();
        translatedInstant_min = translatedInstant_max = 0l;

        recInstant = 0.0;
        legal = false;
    }

    public floodRecession(String _id, ArrayList<Double> _values){
        id = _id;
        floodDischarges = _values;
        moniterSize = _values.size();
        prepareMonitorPairs();

        recDischarge = -1.0;
        closestPair = new AbstractMap.SimpleEntry<>(0.0, 0.0);
        closetInterval = new AbstractMap.SimpleEntry<>(0, 0);
        translatedDischarges = new HashMap<>();
        translatedInstant_min = translatedInstant_max = 0l;

        recInstant = 0.0;
        legal = false;
    }

    public String getId(){
        return id;
    }

    public int getMoniterSize(){
        return moniterSize;
    }

    /**
     * 将观测值转换为观测对；
     */
    private void prepareMonitorPairs(){
        dischargePairs = new ArrayList<>();

        for (int i = 0; i < moniterSize - 1; i++){
            AbstractMap.SimpleEntry<Double, Double> pair =
                new AbstractMap.SimpleEntry<>(getMonitorValue(i), getMonitorValue(i+1));
            dischargePairs.add(pair);
        }
    }

    /**
     * 取得特定点位的水位观测值
     * @param _no : 观测点序号
     * @return 水位值
     */
    public Double getMonitorValue(int _no){
        if ( _no > moniterSize ){
            throw new ArrayIndexOutOfBoundsException("当前点位超出观测水位值序列");
        }
        //return (Double) floodDischarges.get(_no).toString();
        return Double.valueOf(floodDischarges.get(_no).toString());
    }

    /**
     * 从一行字符串中读取洪水序列
     * 默认第一列记录为洪水编号，后续为观点时段点位，以 _delimiter 为分隔符
     * @param _sequence : 观测序列；
     * @param _delimiter : 字符串分隔符
     * @return 观测序列
     */
    public static floodRecession readFromString(String _sequence, String _delimiter){
        String[] val = _sequence.split(_delimiter);
        String _id = val[0];
        ArrayList values = new ArrayList<Float>(val.length);
        Collections.addAll(values,val);
        values.remove(0);
        return new floodRecession(_id, values);
    }

    public String toString(){
        StringBuffer ss = new StringBuffer("退水过程 " + id + " 的监测序列如下： \n");
        for (int i = 0 ; i < moniterSize; i++){
            ss.append("" + i + " : " + floodDischarges.get(i) + "\n");
        }
        return ss.toString();
    }

    public String toPairString(){
        StringBuffer ss = new StringBuffer("退水过程 " + id + " 的监测对序列如下：\n");
        int count = 0;
        for (Map.Entry<Double, Double> pair: dischargePairs ) {
            ss.append("" + (count++) + ": ( " + pair.getKey() + ", " + pair.getValue() + " )\n");
        }
        return ss.toString();
    }

    /**
     * 输出平移后的监测曲线
     * @return
     */
    public JSONObject toTranslatedString(){
        JSONObject jsonObject = new JSONObject();
        if (translatedDischarges.isEmpty()){
            System.err.println("无法得到退水过程 " + id + " 的平移后观测序列");
            jsonObject.put("msg","无法得到退水过程 " + id + " 的平移后观测序列");
            return jsonObject;
        }
        StringBuffer ss = new StringBuffer("退水过程 " + id + " 平移后得到的监测序列如下：\n");
        int count = 0;
        long[] key = new long[moniterSize];
        double[] value = new double[moniterSize];
        for (int i=0; i < moniterSize; i++){
            long instant = translatedInstant_min + i;
            key[i] = instant;
            value[i] = translatedDischarges.get(instant);
            ss.append("" + instant + ": " + translatedDischarges.get(instant) + "\n");
        }
        jsonObject.put("key1",key);
        jsonObject.put("value1",value);
        jsonObject.put("id",id);

        return jsonObject;
    }

    /**
     * 计算退水系数
     * @return
     */
    public Double getRecessionCoefficient(){

        double sum1 = 0.0, sum2 = 0.0;
        for (Map.Entry<Double, Double> pair: dischargePairs){
            sum1 += pair.getKey()*pair.getValue();
        }

        for (Object discharge : floodDischarges){
            sum2 += Math.pow(Double.valueOf(discharge.toString()), 2);
        }

        DecimalFormat df = new DecimalFormat("0.000");
        return Double.valueOf(df.format(sum1/sum2));
    }


    /**
     * 设置退水流量值，同时计算交汇时段、时刻和流量区段
     * @param _recD
     */
    public void setRecDischarge(double _recD){
        recDischarge = _recD;
        calClosestPair(_recD);
        calRecessInstant();
    }

    /**
     * 返回最接近人为设定的退水流量的序列对。
     * 如果 _recD 大于最大的流量值，返回第一对观测值；如果 _recD 小于最小的流量值，返回最后一对观测值
     * @param _recD ： 人为设定的退水流量值
     * @return
     */
    private void calClosestPair(double _recD){
        if ( _recD > getMonitorValue(0)){
            System.err.println("洪水场次： " + id + ", 设定的退水流量 " + _recD + " 高于所有监测值。");
            closetInterval = new AbstractMap.SimpleEntry<>(0,0);
            closestPair = dischargePairs.get(0);
            return;
        }

        if ( _recD < getMonitorValue(moniterSize - 1 )){
            System.err.println("洪水场次： " + id + ", 设定的退水流量 " + _recD + " 低于所有监测值。");
            closetInterval = new AbstractMap.SimpleEntry<>(moniterSize - 2, moniterSize -1);
            closestPair = dischargePairs.get(dischargePairs.size() - 1);
            return;
        }

        closestPair = null;
        int start = 0, end = 1;
        for (Map.Entry<Double, Double> pair: dischargePairs ) {
            if (pair.getKey() >= _recD && pair.getValue() <= _recD){
                closestPair = pair;
                closetInterval = new AbstractMap.SimpleEntry<>(start, end);
                legal = true;           //只有找到退水区段，才说明该退水曲线是合理的
                break;
            }
            start++;
            end++;
        }
    }

    public Map.Entry<Double, Double> getClosestPair(double _recD){
        if (recDischarge == -1.0){
            calClosestPair(_recD);
        }
        return closestPair;
    }

    public Map.Entry<Integer, Integer> getCloestInterval(double _recD) {
        if (recDischarge == -1.0){
            calClosestPair(_recD);
        }
        return closetInterval;
    }

    /**
     * 计算当前场次退水过程与设定的退水线的交汇时间点
     * @return
     */
    private void calRecessInstant(){
        if (recDischarge == -1.0){
            System.err.println("退水位尚未设置，无法计算到达退水位的时刻点");
            return;
        }

        if (!legal){
            System.err.println("当前退水序列" + id + "不合理，无法计算退水时刻");
            return;
        }

        double[] start1 = {closetInterval.getKey(), recDischarge};
        double[] end1 = {closetInterval.getValue(), recDischarge};
        double[] start2 = {closetInterval.getKey(), closestPair.getKey()};
        double[] end2 = {closetInterval.getValue(), closestPair.getValue()};

        double[] crossPoint = CrossPoint.intersection(start1, end1, start2, end2);
        recInstant = crossPoint[0];
    }

    public double getRecessInstant() {
        return recInstant;
    }

    public  boolean isLegal(){
        return legal;
    }

    /**
     * 根据平均退水时刻，对当前监测序列进行平移
     * @param avgRecInstant
     */
    public boolean calTranslatedRecession(double avgRecInstant){
        if (!legal){
            System.err.println("当前场次 " + id + "无效，无法平移");
            return false;
        }

        double deltaT = avgRecInstant - recInstant;                 //计算需要平移的距离
        translatedInstant_min = Math.round(deltaT);
        translatedInstant_max = Math.round(moniterSize - 1 + deltaT);
        for (int i = 0 ; i < moniterSize; i++){
            Long instant = Math.round(i + deltaT);
            translatedDischarges.put(instant, getMonitorValue(i));
        }
        return true;
    }

    public long getTranslatedInstant_Min(){
        return translatedInstant_min;
    }

    public Long getTranslatedInstant_Max() {
        return translatedInstant_max;
    }

    public double getTranslatedDischarge(long _instant){
        if (_instant < translatedInstant_min || _instant > translatedInstant_max){
            return -1.0;
        } else {
            return  translatedDischarges.get(_instant);
        }
    }
}
