package com.xiaoqi.usercenter.one;


import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Personal {

    @ExcelProperty("成员编号")
    private Integer id;
    @ExcelProperty("成员昵称")
    private String name;


}
