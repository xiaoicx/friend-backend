package com.xiaoqi.usercenter.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoqi.usercenter.common.BaseResponse;
import com.xiaoqi.usercenter.common.PageQuery;
import com.xiaoqi.usercenter.common.ResultUtils;
import com.xiaoqi.usercenter.exception.BusinessException;
import com.xiaoqi.usercenter.model.domain.Team;
import com.xiaoqi.usercenter.model.domain.User;
import com.xiaoqi.usercenter.model.query.AddTeamQuery;
import com.xiaoqi.usercenter.model.query.TeamQuery;
import com.xiaoqi.usercenter.model.request.TeamSearchRequest;
import com.xiaoqi.usercenter.model.request.UpdateTeam;
import com.xiaoqi.usercenter.model.vo.TeamSearchVo;
import com.xiaoqi.usercenter.service.ITeamService;
import com.xiaoqi.usercenter.service.UserService;
import com.xiaoqi.usercenter.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 队伍 前端控制器
 * </p>
 *
 * @author xiaoqi
 * @since 2023-08-20
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = "http://localhost:3000/", allowCredentials = "true")
@Slf4j
public class TeamController {

    @Autowired
    private ITeamService iTeamService;
    @Autowired
    private UserService userService;

    @PostMapping("/add")
    //@JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    public BaseResponse<Long> addTeam(@RequestBody AddTeamQuery addTeamQuery, HttpServletRequest request) {
        if (addTeamQuery == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(addTeamQuery, team);
        team.setUserId(loginUser.getId());
        long result = iTeamService.addTeam(team, loginUser);
        return ResultUtils.success(result);
    }


    @PutMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody UpdateTeam updateTeam, HttpServletRequest request) {
        if (updateTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);

        boolean admin = userService.isAdmin(loginUser);
        boolean result = iTeamService.updateTeam(updateTeam, admin, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改失败");
        }
        return ResultUtils.success(true);

    }

    //通过id获取
    @GetMapping("/get/{id}")
    public BaseResponse<UpdateTeam> getTeamById(@PathVariable("id") long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = iTeamService.getById(id);
        UpdateTeam updateTeam = new UpdateTeam();
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        updateTeam.setId(team.getId());
        updateTeam.setName(team.getName());
        updateTeam.setPassword(team.getPassword());
        updateTeam.setDescription(team.getDescription());
        updateTeam.setExpireTime(team.getExpireTime());
        updateTeam.setStatus(team.getStatus());
        updateTeam.setMaxNum(team.getMaxNum());
        return ResultUtils.success(updateTeam);

    }


    @GetMapping("/list")
    public BaseResponse<Page<TeamSearchVo>> listTeam(PageQuery pageQuery, Integer status, HttpServletRequest request) {

        User loginUser = userService.getLoginUserTwo(request);
        Page<TeamSearchVo> page = iTeamService.listTeam(pageQuery, loginUser, status);

        return ResultUtils.success(page);


    }


    @GetMapping("/list/page")

    public BaseResponse<Page<Team>> listTeamPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        QueryWrapper<Team> wrapper = new QueryWrapper<>(team);
        Page<Team> teamPage = iTeamService.page(new Page<>(teamQuery.getPageNo(), teamQuery.getPageSize()), wrapper);
        return ResultUtils.success(teamPage);


    }


    @GetMapping("/search")
    public BaseResponse<Page<TeamSearchVo>> searchTeamMessage(TeamSearchRequest teamSearchRequest,
                                                              HttpServletRequest request) {
        if (teamSearchRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUserTwo(request);
        boolean admin = userService.isAdmin(loginUser);

        Page<TeamSearchVo> page = iTeamService.searchTeamMessage(teamSearchRequest, admin);

        return ResultUtils.success(page);


    }

    @GetMapping("/join")
    public BaseResponse<Boolean> joinTeam(Long id, String password, HttpServletRequest request) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = iTeamService.joinTeam(id, loginUser, password);

        return ResultUtils.success(result);

    }


    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteTeam(@PathVariable("id") long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        User loginUser = userService.getLoginUser(request);

        boolean result = iTeamService.deleteTeam(id, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/back/{id}")
    public BaseResponse<Boolean> backTeam(@PathVariable("id") long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        boolean result = iTeamService.backTeam(id, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退队失败");
        }
        return ResultUtils.success(true);


    }

    /**
     * 获取自己创建的队伍
     *
     * @param request
     * @return
     */
    @GetMapping("/create")
    public BaseResponse<Page<TeamSearchVo>> myCreateTeam(PageQuery query, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<TeamSearchVo> page = iTeamService.myCreateTeam(query, loginUser);
        return ResultUtils.success(page);

    }


    /**
     * 我加入的队伍
     *
     * @param query
     * @param request
     * @return
     */
    @GetMapping("/my/join")
    public BaseResponse<Page<TeamSearchVo>> myJoinTeam(PageQuery query, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        Page<TeamSearchVo> page = iTeamService.myJoinTeam(query, loginUser);
        return ResultUtils.success(page);
    }


}
