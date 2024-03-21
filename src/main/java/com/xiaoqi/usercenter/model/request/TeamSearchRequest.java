package com.xiaoqi.usercenter.model.request;

import com.xiaoqi.usercenter.common.PageQuery;
import lombok.Data;

@Data
public class TeamSearchRequest extends PageQuery {

    private String descriptions;

}
