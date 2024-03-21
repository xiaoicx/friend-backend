package com.xiaoqi.usercenter.mapper;

import com.xiaoqi.usercenter.model.domain.Team;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoqi.usercenter.model.request.TeamSearchRequest;
import com.xiaoqi.usercenter.model.vo.TeamSearchVo;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 队伍 Mapper 接口
 * </p>
 *
 * @author xiaoqi
 * @since 2023-08-20
 */
public interface TeamMapper extends BaseMapper<Team> {


    List<TeamSearchVo> search(@Param("team") TeamSearchRequest team, @Param("nowTime") LocalDateTime nowTime);

    List<TeamSearchVo> notAdminSearch(@Param("team") TeamSearchRequest team, @Param("nowTime") LocalDateTime nowTime);

}
