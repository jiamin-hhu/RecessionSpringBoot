package com.hhu.recession.util;

import com.alibaba.fastjson.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * recession history
 * 用于记录历史上一系列洪水退水的时序流量记录；
 *
 * --- by jiamin.luu@hhu.edu.cn
 */
public class recessionHistory {

    List<floodRecession> history;           //系列洪水退水场次
    int amount;                             //历史场次数量
    double recDischarge;                    //人为设定的退水流量值
    double avgRecInstant;                   //平均退水时刻

    Map<Long, Double> avgRecession;         //平均退水过程
    Long startInstant, endInstant;

    public recessionHistory(){
        history = new ArrayList<>();
        amount = 0;
        avgRecInstant = 0.0;
        avgRecession = new HashMap<>();
        startInstant = endInstant = 0l;
    }

    public int append(floodRecession recession){
        history.add(recession);
        amount = history.size();
        return amount;
    }

    public floodRecession get(int _no){
        if (_no > amount){
            System.err.println("当前历史场次少于 " + _no + "场");
            return null;
        }
        return history.get(_no);
    }

    public int getAmount() {
        return amount;
    }

    /**
     * 对所有历史场次的退水过程，都设置相同的退水流量
     * @param _rec
     */
    public boolean setRecDischarge(double _rec) {
        this.recDischarge = _rec;

        //1. 计算各历史退水场次相较于当前设定退水流量值的退水时刻
        for (floodRecession process: history) {
            process.setRecDischarge(_rec);
        }

        //2. 计算各历史退水场次需要平移的时刻距离
        avgRecInstant = calRecInstant_avg();
        List<Boolean> res = new ArrayList<>();
        for (floodRecession process: history) {
            boolean flag = process.calTranslatedRecession(avgRecInstant);
            res.add(flag);
        }

        if(res.contains(true)){
            //3. 计算平移后所得到的平均退水曲线
            calRecession_avg();
            return true;
        }
        return false;
    }

    public double getRecDischarge() {
        return recDischarge;
    }

    public double getRecInstant4flood(String _id){
        for (floodRecession process: history){
            if (process.id == _id){
                if (process.isLegal())
                    return process.getRecessInstant();
                else{
                    System.err.println("当前场次退水过程 " + _id + " 是无效的");
                    return 0.0;
                }
            }
        }
        System.err.println("在本历史过程中无法找到场次：" + _id);
        return 0.0;
    }

    /**
     * 针对相同的退水过程，计算各有效过程的平均退水时刻，以便计算各个过程需要平移的时刻距离
     * @return
     */
    private double calRecInstant_avg(){
        double sum = 0.0;
        int count = 0;
        for (floodRecession process : history) {
            if (process.isLegal()){
                sum += process.getRecessInstant();
                count++;
            }
        }
        return sum/count;
    }

    /**
     * 计算平均退水曲线
     */
    private void calRecession_avg(){
        startInstant = Long.MAX_VALUE;
        endInstant = Long.MIN_VALUE;

        // 获取历史场次中的起始时刻
        for (floodRecession process : history){
            if (!process.isLegal()){
                continue;
            } else {
                if (process.getTranslatedInstant_Min() < startInstant) {
                    startInstant = process.getTranslatedInstant_Min();
                }
                if (process.getTranslatedInstant_Max() > endInstant){
                    endInstant = process.getTranslatedInstant_Max();
                }
            }
        }

        for (long instant = startInstant; instant <= endInstant; instant++){
            double sum = 0.0;
            int count = 0;
            for (floodRecession process : history){
                if (!process.isLegal()){
                    continue;
                } else if (-1.0 != process.getTranslatedDischarge(instant)){
                    sum += process.getTranslatedDischarge(instant);
                    count++;
                }
            }
            DecimalFormat df = new DecimalFormat("0.000");
            double avg = Double.valueOf(df.format(sum/count));
            avgRecession.put(instant, avg);
        }
    }

    public JSONObject toAvgRecessionString(){
        JSONObject jsonObject = new JSONObject();
        long[] key = new long[(int)(endInstant-startInstant)+1];
        double[] value = new double[(int)(endInstant-startInstant)+1];
        StringBuffer ss = new StringBuffer("历史平均退水曲线为：\n");
        int offset = (int)(0-startInstant);
        for (long instant = startInstant; instant <= endInstant; instant++){
            key[(int) instant+offset] = instant;
            value[(int) instant+offset] = avgRecession.get(instant);
            ss.append("" + instant + " : " + avgRecession.get(instant) + "\n");
        }
        jsonObject.put("key2",key);
        jsonObject.put("value2",value);
        return jsonObject;
    }

    public String toAvgRecDSVString(String _delimiter){
        StringBuffer ss = new StringBuffer("平均退水曲线" + _delimiter);
        for (long instant = startInstant; instant <= endInstant; instant++){
            ss.append(avgRecession.get(instant) + _delimiter);
        }
        ss.deleteCharAt(ss.length() - 1);
        return ss.toString();
    }


}
