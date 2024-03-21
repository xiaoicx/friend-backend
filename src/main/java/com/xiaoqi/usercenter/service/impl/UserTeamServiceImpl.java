package com.xiaoqi.usercenter.service.impl;

import com.xiaoqi.usercenter.model.domain.UserTeam;
import com.xiaoqi.usercenter.mapper.UserTeamMapper;
import com.xiaoqi.usercenter.service.IUserTeamService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户队伍关系表 服务实现类
 * </p>
 *
 * @author xiaoqi
 * @since 2023-08-20
 */
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam> implements IUserTeamService {

}
