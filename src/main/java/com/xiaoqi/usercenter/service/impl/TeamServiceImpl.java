package com.xiaoqi.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoqi.usercenter.common.PageQuery;
import com.xiaoqi.usercenter.enums.Status;
import com.xiaoqi.usercenter.exception.BusinessException;
import com.xiaoqi.usercenter.model.domain.Team;
import com.xiaoqi.usercenter.mapper.TeamMapper;
import com.xiaoqi.usercenter.model.domain.User;
import com.xiaoqi.usercenter.model.domain.UserTeam;
import com.xiaoqi.usercenter.model.request.TeamSearchRequest;
import com.xiaoqi.usercenter.model.request.UpdateTeam;
import com.xiaoqi.usercenter.model.request.UserSearch;
import com.xiaoqi.usercenter.model.vo.TeamSearchVo;
import com.xiaoqi.usercenter.service.ITeamService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoqi.usercenter.service.IUserTeamService;
import com.xiaoqi.usercenter.service.UserService;
import com.xiaoqi.usercenter.common.ErrorCode;
import com.xiaoqi.usercenter.contant.Preheat;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 队伍 服务实现类
 * </p>
 *
 * @author xiaoqi
 * @since 2023-08-20
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements ITeamService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IUserTeamService iUserTeamService;

    @Resource
    private TeamMapper teamMapper;
    @Resource
    private UserService userService;


    @Override
    @Transactional
    public long addTeam(Team team, User loginUser) {
        //1获取用户id
        Long userId = loginUser.getId();
        //2队伍人数>1且<=20
        Integer maxNum = team.getMaxNum();
        if (maxNum < 1 && maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不符合队伍人数限定");
        }
        //3队伍标题<=20
        String title = team.getName();
        if (StringUtils.isNotBlank(title) && title.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题长度不服合要求");
        }
        //4描述长度<=512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "描述长度不服合要求");
        }
        //5是否有状态码
        Integer status = Optional.ofNullable(team.getStatus()).orElse(0);
        if (status < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态码不服合要求");
        }
        //6校验密码
        Status statuesName = Status.getStatus(status);
        String password = team.getPassword();
        if (statuesName == null || (statuesName.getValue() == 2 && (StringUtils.isEmpty(password) || password.length() > 32))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不服合要求");
        }
        //7过期时间
        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime expireTime = team.getExpireTime();
        if (currentDate.isAfter(team.getExpireTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过期时间不合要求");
        }
        //8队伍数量,加锁
        //8.1获取锁
        RLock lock = redissonClient.getLock(Preheat.TROOPS_LOCK);

        try {
            if (lock.tryLock(30, -1, TimeUnit.MILLISECONDS)) {

                Long count = this.lambdaQuery().eq(Team::getUserId, userId).count();
                if (count >= 5) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前用户创建队伍超过5个");
                }
                //添加队伍
                boolean save = this.save(team);
                if (!save) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加队伍失败");
                }
                //添加队伍关系表
                UserTeam userTeam = new UserTeam();
                userTeam.setUserId(userId);
                userTeam.setTeamId(team.getId());
                userTeam.setJoinTime(LocalDateTime.now());
                iUserTeamService.save(userTeam);

                return team.getId();

            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return 0;
    }

    @Override
    public Page<TeamSearchVo> searchTeamMessage(TeamSearchRequest teamSearchRequest, boolean admin) {
        //管理可以查询所有未过期的队伍
        LocalDateTime nowTime = LocalDateTime.now();
        String descriptions = teamSearchRequest.getDescriptions();
        Integer pageNo = teamSearchRequest.getPageNo();
        Integer pageSize = teamSearchRequest.getPageSize();
        boolean flag = true;
        LambdaQueryWrapper<Team> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (descriptions == null || descriptions.equals("")) {
            flag = false;
        }
        lambdaQueryWrapper.gt(Team::getExpireTime, nowTime).eq(!admin, Team::getStatus, 0);
        if (flag) {
            lambdaQueryWrapper.and(i -> i.like(Team::getName, descriptions).or().like(Team::getDescription, descriptions));
        }


        Page<TeamSearchVo> page = commonTeamMessage(pageNo, pageSize, lambdaQueryWrapper);


        return page;
    }


    @Override
    public boolean updateTeam(UpdateTeam updateTeam, boolean admin, User loginUser) {
        Integer status = updateTeam.getStatus();
        boolean result = this.lambdaUpdate()
                .eq(updateTeam.getId() != null, Team::getId, updateTeam.getId())
                .eq(!admin, Team::getUserId, loginUser.getId())
                .set(Team::getExpireTime, LocalDateTime.now())
                .set(updateTeam.getPassword() != null && updateTeam.getPassword().length() <= 32 && updateTeam.getStatus() == 2, Team::getPassword, updateTeam.getPassword())
                .set(updateTeam.getDescription() != null && !updateTeam.getDescription().equals("") && updateTeam.getDescription().length() <= 512, Team::getDescription, updateTeam.getDescription())
                .set(updateTeam.getName() != null && !updateTeam.getName().equals("") && updateTeam.getName().length() <= 20, Team::getName, updateTeam.getName())
                .set(status != null && (status == 0 || status == 1 || status == 2), Team::getStatus, status)
                .update();
        return result;
    }

    @Override
    public Boolean joinTeam(Long id, User loginUser, String password) {
        Team team = this.lambdaQuery().eq(Team::getId, id).one();

        if (team == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍不存在");
        }

        RLock lock = redissonClient.getLock(Preheat.JOIN_LOCK);

        try {
            if (lock.tryLock(20, -1, TimeUnit.MILLISECONDS)) {
                Long count = iUserTeamService.
                        lambdaQuery().eq(UserTeam::getUserId, loginUser.getId()).count();

                if (count >= 5) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍数量超过5个");
                }

                UserTeam joinUserTeam = iUserTeamService.
                        lambdaQuery().eq(UserTeam::getTeamId, id).eq(UserTeam::getUserId, loginUser.getId()).one();
                if (joinUserTeam != null) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "已经加入这个队伍不能重复加");
                }
                if (team.getStatus() == 2 && !team.getPassword().equals(password)) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不符合条件");
                }
                UserTeam userTeam = new UserTeam();
                LocalDateTime nowTime = LocalDateTime.now();
                userTeam.setUserId(loginUser.getId());
                userTeam.setTeamId(id);
                userTeam.setJoinTime(nowTime);

                boolean result = iUserTeamService.save(userTeam);
                return result;

            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return false;
    }


    /**
     * 队长移除队伍表
     *
     * @param id
     * @param loginUser
     * @return
     */


    @Override
    @Transactional
    public boolean deleteTeam(long id, User loginUser) {
        Team team =
                this.lambdaQuery().
                        eq(Team::getId, id).
                        eq(Team::getUserId, loginUser.getId()).one();
        if (team == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不存在该队伍");
        }
        //移除队伍关系表
        LambdaQueryWrapper<UserTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTeam::getTeamId, id);
        boolean remove = iUserTeamService.remove(wrapper);
        //移除队伍表
        boolean result = this.removeById(id);
        return (remove && result);
    }


    /**
     * 退队
     *
     * @param id
     * @param loginUser
     * @return
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean backTeam(long id, User loginUser) {
        long count = iUserTeamService.lambdaQuery().eq(UserTeam::getTeamId, id).count();

        //该队伍不存在
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该队伍不存在");
        }
        //该队伍只剩一人
        if (count == 1) {
            //删除队伍关系表
            boolean remove = iUserTeamService.
                    lambdaUpdate().
                    eq(UserTeam::getUserId, loginUser.getId()).
                    remove();
            //删除队伍表
            boolean result = this.lambdaUpdate().eq(Team::getId, id).
                    eq(Team::getUserId, loginUser.getId()).remove();

            return (remove && result);

        }
        //队伍人数超过一人
        boolean remove = iUserTeamService.lambdaUpdate().
                eq(UserTeam::getTeamId, id).eq(UserTeam::getUserId, loginUser.getId()).remove();


        List<UserTeam> userTeams
                = iUserTeamService.lambdaQuery().
                eq(UserTeam::getTeamId, id).
                orderByAsc(UserTeam::getJoinTime).list();

        UserTeam userTeam = userTeams.get(0);
        //队长位置顺位
        boolean result = this.lambdaUpdate().eq(Team::getId, id).set(Team::getUserId, userTeam.getUserId()).update();
        return (remove && result);
    }


    /**
     * 查询自己创建的队伍
     *
     * @param query
     * @param loginUser
     * @return
     */

    @Override
    public Page<TeamSearchVo> myCreateTeam(PageQuery query, User loginUser) {
        Integer pageNo = query.getPageNo();
        Integer pageSize = query.getPageSize();
        LocalDateTime dateTime = LocalDateTime.now();
        LambdaQueryWrapper<Team> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Team::getUserId, loginUser.getId());
        //过期时间大于现在时间
        lambdaQueryWrapper.gt(Team::getExpireTime, dateTime);
        Page<TeamSearchVo> page = commonTeamMessage(pageNo, pageSize, lambdaQueryWrapper);
        return page;
    }


    /**
     * 查询自己加入的队伍
     *
     * @param query
     * @param loginUser
     * @return
     */

    @Override
    public Page<TeamSearchVo> myJoinTeam(PageQuery query, User loginUser) {
        Integer pageNo = query.getPageNo();
        Integer pageSize = query.getPageSize();
        List<UserTeam> userTeamList =
                iUserTeamService.lambdaQuery().eq(UserTeam::getUserId, loginUser.getId()).list();
        List<Long> teamIdList = userTeamList.stream().map(userTeam -> userTeam.getTeamId()).collect(Collectors.toList());
        LambdaQueryWrapper<Team> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        LocalDateTime dateTime = LocalDateTime.now();
        lambdaQueryWrapper.in(Team::getId, teamIdList);
        //过期时间大于现在时间
        lambdaQueryWrapper.gt(Team::getExpireTime, dateTime);
        //查询出所有与自己有关的队伍
        Page<TeamSearchVo> page = commonTeamMessage(pageNo, pageSize, lambdaQueryWrapper);
        //去除自己创建的队伍
        List<TeamSearchVo> list = new ArrayList<>();
        List<TeamSearchVo> records = page.getRecords();
        for (TeamSearchVo record : records) {
            if (record.getUserId().equals(loginUser.getId())) {
                break;
            }
            list.add(record);
        }
        page.setRecords(list);

        return page;
    }


    /**
     * 分页队伍信息
     *
     * @param pageQuery
     * @param loginUser
     * @return
     */
    @Override
    public Page<TeamSearchVo> listTeam(PageQuery pageQuery, User loginUser, Integer status) {
        Integer pageNo = pageQuery.getPageNo();
        Integer pageSize = pageQuery.getPageSize();
        LambdaQueryWrapper<Team> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        LocalDateTime dateTime = LocalDateTime.now();
        //过期时间大于现在时间
        lambdaQueryWrapper.gt(Team::getExpireTime, dateTime);
        lambdaQueryWrapper.eq(status != null, Team::getStatus, status);
        Page<TeamSearchVo> page = commonTeamMessage(pageNo, pageSize, lambdaQueryWrapper);
        List<TeamSearchVo> records = page.getRecords();
        if (loginUser != null) {
            for (TeamSearchVo record : records) {
                //是创建队伍的人
                if (record.getUserId().equals(loginUser.getId())) {
                    record.setJoin(true);
                } else {
                    //不是创建队伍的人
                    Map<Long, UserSearch> map =
                            record.getUser().stream().
                                    collect(Collectors.toMap(UserSearch::getId, c -> c));
                    UserSearch userSearch = map.get(loginUser.getId());
                    if (userSearch != null) {
                        //但是已经加入了
                        record.setJoin(true);
                    }
                }

            }


        }


        return page;
    }


    public List<UserSearch> copyUser(List<User> userList) {

        List<UserSearch> list = new ArrayList<>();
        for (User user : userList) {
            UserSearch userSearch = new UserSearch();
            userSearch.setUserAccount(user.getUserAccount());
            userSearch.setProfile(user.getProfile());
            userSearch.setUsername(user.getUsername());
            userSearch.setId(user.getId());
            userSearch.setPhone(user.getPhone());
            userSearch.setAvatarUrl(user.getAvatarUrl());
            list.add(userSearch);
        }

        return list;
    }


    public Page<TeamSearchVo> commonTeamMessage(Integer pageNo, Integer pageSize, LambdaQueryWrapper<Team> lambdaQueryWrapper) {
        Page page = null;

        page = this.page(new Page<>(pageNo, pageSize), lambdaQueryWrapper);
        List<Team> records = page.getRecords();
        List<TeamSearchVo> listVo = new ArrayList<>();
        for (Team team : records) {
            TeamSearchVo vo = new TeamSearchVo();
            vo.setId(team.getId());
            vo.setName(team.getName());
            vo.setDescription(team.getDescription());
            vo.setMaxNum(team.getMaxNum());
            vo.setStatus(team.getStatus());
            vo.setExpireTime(team.getExpireTime());
            vo.setCreateTime(team.getCreateTime());
            vo.setUserId(team.getUserId());
            listVo.add(vo);
        }
        //存放用户id
        Map<Long, List<Long>> map = new HashMap<>();
        //存放已经加加入队伍人数
        Map<Long, Integer> map1 = new HashMap<>();

        //查队伍关系表
        for (Team team : records) {
            List<UserTeam> userTeams = iUserTeamService.lambdaQuery().eq(UserTeam::getTeamId, team.getId()).list();
            List<Long> collect = userTeams.stream().map(userTeam -> userTeam.getUserId()).collect(Collectors.toList());
            map.put(team.getId(), collect);
            map1.put(team.getId(), userTeams.size());
        }


        for (TeamSearchVo vo : listVo) {
            Long teamId = vo.getId();
            List<Long> userId = map.get(teamId);
            if (userId != null) {
                List<User> userList = userService.listByIds(userId);
                List<UserSearch> userSearchList = copyUser(userList);
                Map<Long, UserSearch> userMap = userSearchList.stream().collect(Collectors.toMap(UserSearch::getId, c -> c));
                UserSearch user = userMap.get(vo.getUserId());
                vo.setHostUser(user);
                vo.setUser(userSearchList);
                vo.setJoinNum(map1.get(vo.getId()));
            }
        }
        page.setRecords(listVo);


        return page;
    }


}