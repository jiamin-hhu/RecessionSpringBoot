package com.hhu.recession.util;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class CompleteHistory {

	public static JSONObject complete(String[] input, double recDischarge) {
		recessionHistory history = new recessionHistory();
	    for( String floodString : input ){
			floodRecession process = floodRecession.readFromString(floodString, ",");
			history.append(process);
		}

	    boolean flag = history.setRecDischarge(recDischarge);

	    if(!flag){
	    	return null;
		}

		JSONObject jsonObject = new JSONObject();
		List list = new ArrayList();
	    //输出平移后的退水曲线
		for (int i =0; i < history.getAmount(); i++){
			if (history.get(i).isLegal()){
				jsonObject.put(history.get(i).toTranslatedString().getString("id"),history.get(i).toTranslatedString().get("key1"));
				jsonObject.put(history.get(i).toTranslatedString().getString("id")+"_y",history.get(i).toTranslatedString().get("value1"));
				//System.out.println(history.get(i).toTranslatedString());
				list.add(history.get(i).toTranslatedString().getString("id"));

			}
		}
		//输出历史场次的平均退水曲线
	    //System.out.println(history.toAvgRecessionString());
		jsonObject.put("平均退水曲线",history.toAvgRecessionString().get("key2"));
		jsonObject.put("平均退水曲线_y",history.toAvgRecessionString().get("value2"));
		list.add("平均退水曲线");
		jsonObject.put("legend",list);

		//计算平均退水曲线的退水系数
	    floodRecession averageRecession =  floodRecession.readFromString(
	    		history.toAvgRecDSVString(","),",");
		jsonObject.put("ratio",averageRecession.getRecessionCoefficient());
	    System.out.println("历史退水系数为： " + averageRecession.getRecessionCoefficient());

	    return jsonObject;
    }
}
