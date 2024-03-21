package com.xiaoqi.usercenter.one;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;

public class TableListener implements ReadListener<Personal> {


    @Override
    public void invoke(Personal personal, AnalysisContext analysisContext) {
        // 当前sheet的名称 编码获取类似
        // System.out.println(personal);

    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        System.out.println("已经解析完成");

    }


}
