package com.xiaoqi.usercenter.common;


import lombok.Data;

import java.io.Serializable;


/**
 * 通用分页参数
 */

@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = -684979447075466766L;


    public static final Integer DEFAULT_PAGE_SIZE = 20;
    public static final Integer DEFAULT_PAGE_NUM = 1;



    private Integer pageNo = DEFAULT_PAGE_NUM;


    private Integer pageSize = DEFAULT_PAGE_SIZE;
}
