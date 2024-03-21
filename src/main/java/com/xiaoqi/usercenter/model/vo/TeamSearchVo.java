package com.xiaoqi.usercenter.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xiaoqi.usercenter.model.request.UserSearch;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;


@Data
public class TeamSearchVo implements Serializable {

    private static final long serialVersionUID = 156L;

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


    @ApiModelProperty(value = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;


    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;


    private Long userId;


    private Integer joinNum;

    /**
     * 创建人信息
     */

    private UserSearch hostUser;

    /**
     * 队伍关联人信息
     */

    private List<UserSearch> user;


    private boolean join = false;


}
