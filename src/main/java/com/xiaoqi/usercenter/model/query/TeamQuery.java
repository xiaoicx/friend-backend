package com.xiaoqi.usercenter.model.query;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import com.xiaoqi.usercenter.common.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 请求数据类
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQuery extends PageQuery {

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "队伍名称")
    private String name;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "最大人数")
    private Integer maxNum;

    @ApiModelProperty(value = "0 - 公开, 1-私有, 2-加密")
    private Integer status;


    @ApiModelProperty(value = "用户id")
    private Long userId;
}
