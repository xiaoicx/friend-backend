package com.xiaoqi.usercenter.enums;



public enum Status {
    PUBLIC(0,"公共"),
    PRIVATE(1,"私有"),
    SECRET(2,"加密");


  public static  Status  getStatus(Integer value){
      if (value==null){
          return null;
      }
      Status[] values = Status.values();
      for (Status status : values) {
          if (status.getValue()==value){
              return status;
          }
      }

     return null;
  }




    private  int   value;
  private  String  text;

    Status(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
