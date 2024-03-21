package com.xiaoqi.usercenter.one;


import com.alibaba.excel.EasyExcel;

import java.util.List;

public class ImportExcel {

    /*public static void main(String[] args) {

        String  file="D:\\userP\\user-center-backend\\src\\main\\resources\\testExcel.xlsx";

        synchronousRead(file);

    }*/


    /**
     * 监听器读
     */
    public static void listenerRead(String file) {
        EasyExcel.read(file, Personal.class, new TableListener()).sheet().doRead();
    }


    /**
     * 同步读
     */

    public static void synchronousRead(String file) {


        List<Personal> list = EasyExcel.read(file, Personal.class, new TableListener()).sheet().doReadSync();
        for (Personal personal : list) {
            System.out.println(personal);
        }

    }


}