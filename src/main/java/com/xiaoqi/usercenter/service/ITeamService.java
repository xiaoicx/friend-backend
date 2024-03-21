package com.xiaoqi.usercenter.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoqi.usercenter.common.PageQuery;
import com.xiaoqi.usercenter.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoqi.usercenter.model.domain.User;
import com.xiaoqi.usercenter.model.request.TeamSearchRequest;
import com.xiaoqi.usercenter.model.request.UpdateTeam;
import com.xiaoqi.usercenter.model.vo.TeamSearchVo;

/**
 * <p>
 * 队伍 服务类
 * </p>
 *
 * @author xiaoqi
 * @since 2023-08-20
 */
public interface ITeamService extends IService<Team> {

    /**
     * 增加队伍
     * @param team
     * @param loginUser
     * @return
     */

   long  addTeam(Team team, User loginUser);

    Page<TeamSearchVo> searchTeamMessage(TeamSearchRequest teamSearchRequest, boolean  admin);

    boolean updateTeam(UpdateTeam updateTeam, boolean admin, User loginUser);

    Boolean joinTeam(Long id, User loginUser,String password);

    boolean deleteTeam(long  id,User loginUser);

    boolean backTeam(long id, User loginUser);

    Page<TeamSearchVo> myCreateTeam(PageQuery query, User loginUser);

    Page<TeamSearchVo> myJoinTeam(PageQuery query, User loginUser);

    Page<TeamSearchVo> listTeam(PageQuery pageQuery,User loginUser,Integer status);
}
