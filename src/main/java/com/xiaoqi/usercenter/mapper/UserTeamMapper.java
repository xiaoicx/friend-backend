package com.xiaoqi.usercenter.mapper;

import com.xiaoqi.usercenter.model.domain.UserTeam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoqi.usercenter.model.request.UserSearch;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * <p>
 * 用户队伍关系表 Mapper 接口
 * </p>
 *
 * @author xiaoqi
 * @since 2023-08-20
 */
public interface UserTeamMapper extends BaseMapper<UserTeam> {
    List<UserSearch> findUser(@Param("id") Long id);

}
