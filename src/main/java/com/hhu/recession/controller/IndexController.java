package com.hhu.recession.controller;

import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import com.alibaba.fastjson.JSONObject;
import com.hhu.recession.entity.ApiResp;
import com.hhu.recession.util.FileUtil;
import org.apache.poi.util.StringUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.hhu.recession.util.CompleteHistory.complete;

@RestController
@RequestMapping("index")
public class IndexController {

    private String[] input = {
            "78919,138,129,120,112,105,99.4,93.7,90.1,88.7,87.3,85.9,84.5,83,80.8,78.1,77.8,74.1,71.7," +
                    "69.8,68.6,67.3,64.6,60.7,59.4,58.2,56.9,55.7,54.1,52.8,51,49.9,48.8",
            "79813,92.1,81.6,77.4,74.8,73.8,68.7,65.3,63.4,59.7,57.5,56.6,55.3,53.9,52.6,51.8,50,47.4,46.1",
            "79916,239,221,204,186,169,166,162,158,153,147,141,137,132,127,125,123,121,119,117,115,114," +
                    "112,110,109,107,106,105,103,101,98.5,95.9,93.2,92.6,92,91.4"
    };

    @PostMapping("upload")
    public ApiResp upload(MultipartFile file) throws Exception {
        if (null == file || file.isEmpty()) {
            return ApiResp.retFail("请上传文件");
        }
        String fileName = file.getOriginalFilename();
        // MultipartFile转成File
        String newFilePath = System.getProperty("user.dir")+"/"+ UUID.randomUUID().toString().replaceAll("-","")+fileName.substring(fileName.lastIndexOf("."));
        int line = 0;
        File newFile = new File(newFilePath);
        JSONObject uploadResult = new JSONObject();
        try {
            newFile = FileUtil.multipartFileToFile(file);
            ImportParams params = new ImportParams();
            params.setTitleRows(0);
            // 执行导入
            ExcelImportResult<Map> result = ExcelImportUtil.importExcelMore(newFile, Map.class, params);
            int colSize = result.getList().get(0).size();
            String[] res = new String[colSize-1];
            List<List<String>> arr = new ArrayList<>();
            for(int i=0;i<colSize-1;i++){
                List<String> tmp = new ArrayList();
                arr.add(tmp);
            }
            // 转map
            for (int i = 0, length = result.getList().size(); i < length; i++) {
                Map tmpObj = result.getList().get(i);
                for(int j=0;j<colSize-1;j++){
                    String value = (String.valueOf(tmpObj.get("洪号"+(j+1))));
                    if(value!=null&&!("null").equals(value)){
                        arr.get(j).add(value);
                    }
                }
                // 行数+1
                line++;
            }
            for(int i=0;i<arr.size();i++){
                res[i] = StringUtil.join(arr.get(i).toArray(),",");
            }
            input = res;
            uploadResult = complete(input,100.0);
            if(uploadResult!=null){
                uploadResult.put("input",input);
            }else{
                return ApiResp.retFail("设定的退水流量低于所有监测值,当前场次无效");
            }
        }catch (IOException e) {
            return ApiResp.retFail("第【" + line + "】行导入失败");
        }finally {
            newFile.delete();
        }
        return ApiResp.retOK(uploadResult);
    }

    @GetMapping("showright/{flow}")
    public ApiResp changeFlow(@PathVariable String flow){
        JSONObject result = complete(input, Double.parseDouble(flow));
        if(result==null){
            return ApiResp.retFail("设定的退水流量低于所有监测值,当前场次无效");
        }else{
            return ApiResp.retOK(result);
        }
    }


}
