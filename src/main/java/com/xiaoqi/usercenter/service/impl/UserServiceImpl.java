package com.xiaoqi.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xiaoqi.usercenter.exception.BusinessException;
import com.xiaoqi.usercenter.model.domain.User;
import com.xiaoqi.usercenter.service.UserService;
import com.xiaoqi.usercenter.mapper.UserMapper;
import com.xiaoqi.usercenter.util.AlgorithmUtils;
import com.xiaoqi.usercenter.common.ErrorCode;
import com.xiaoqi.usercenter.contant.Preheat;
import com.xiaoqi.usercenter.contant.UserConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }


    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setProfile(originUser.getProfile());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return 1;
    }


    /**
     * 标签搜索用户
     *
     * @param tagList
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagList) {
        if (CollectionUtils.isEmpty(tagList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = this.lambdaQuery().select().list();
        Gson gson = new Gson();
        List<User> users = userList.stream().filter(user -> {
            if (user == null) {
                return false;
            }
            String tagsName = user.getTags();
            Set<String> tagsNameSet = gson.fromJson(tagsName, new TypeToken<Set<String>>() {
            }.getType());
            tagsNameSet = Optional.ofNullable(tagsNameSet).orElse(new HashSet<>());
            for (String s : tagList) {
                if (!tagsNameSet.contains(s)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());


        return users;

    }

    @Override
    public Integer updateUser(User user, User loginUser) {
        //是管理员,可以更新所有用户
        Long userId = user.getId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);

        if (userObj == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User) userObj;
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        // 仅管理员可查询
        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @Override
    public Page<User> recommendUsers(Integer pageNo, Integer pageNum, HttpServletRequest request) {
        String key = Preheat.PREHEAT_KEY;
        List<User> userList;
        //获取登入用户
        User loginUser = this.getLoginUserTwo(request);
        if (loginUser != null) {
            //用户信息登入的预热数据
            key = key + loginUser.getId();
        }
        userList = (List<User>) redisTemplate.opsForValue().get(key);

        if (userList == null) {

            //如果缓存没有查数据库
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            Page<User> userPage = this.page(new Page<>(pageNo, pageNum), wrapper);
            userList = userPage.getRecords().
                    stream().
                    map(user -> this.getSafetyUser(user)).
                    collect(Collectors.toList());
            //写入缓存
            this.writerCache(request, userList);
        }

        //其实页码
        int startNo = (pageNo - 1) * pageNum;
        int endNo = (pageNum - 1);
        if (endNo > userList.size() - 1) {
            endNo = userList.size() - 1;
        }
        List<User> list = new ArrayList<>();
        for (int i = startNo; i <= endNo; i++) {
            list.add(userList.get(i));
        }

        Page<User> page = new Page<>();
        page.setRecords(list);

        return page;
    }

    @Override
    public void preMessage(List<Long> listId) {
        String noKey = Preheat.PREHEAT_KEY;

        for (Long id : listId) {
            String key = Preheat.PREHEAT_KEY + id;
            //不包括自己
            List<User> list = this.lambdaQuery().notIn(User::getId, id).list();
            //信息脱敏
            List<User> userList = list.stream().map(user -> this.getSafetyUser(user)).collect(Collectors.toList());

            redisTemplate.opsForValue().set(key, userList);
            //设置过期时间
            redisTemplate.expire(key, 1000, TimeUnit.SECONDS);

        }
        //设置没有登入时推荐信息
        List<User> userList1 = this.lambdaQuery().list();
        //信息脱敏
        userList1 = userList1.stream().map(user -> this.getSafetyUser(user)).collect(Collectors.toList());
        redisTemplate.opsForValue().set(noKey, userList1);
        //设置过期时间
        redisTemplate.expire(noKey, 100000, TimeUnit.SECONDS);


    }

    @Override
    public User getLoginUserTwo(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        return (User) userObj;
    }

    @Override
    public void writerCache(HttpServletRequest request, List<User> userList) {
        String key = Preheat.PREHEAT_KEY;
        User loginUser = this.getLoginUserTwo(request);
        if (loginUser != null) {
            key = key + loginUser.getId();
        }

        redisTemplate.opsForValue().set(key, userList);
        //设置过期时间
        redisTemplate.expire(key, 100000, TimeUnit.SECONDS);

    }

    @Override
    public List<User> matchUsers(long num, User loginUser) {
        //1查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list(queryWrapper);
        //获取当前登入用户tags
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        //把["c","b"]String转成list
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 用户列表的下标 => 相似度
        List<Pair<User, Long>> list = new ArrayList<>();
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            if (userTags == null || user.getId().equals(loginUser.getId())) {
                continue;
            }
            //获取标签
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        // 按编辑距离由小到大排序
        List<Pair<User, Long>> pairList = list.stream().sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        //获取到符合的用户id
        List<Long> userIdList = pairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        //通过id查询用户信息
        List<User> users = this.lambdaQuery().in(User::getId, userIdList).list();
        //映射成map<id,List<User>>   List<User>  只有一个User
        Map<Long, List<User>> map
                = users.stream().map(user -> getSafetyUser(user)).collect(Collectors.groupingBy(User::getId));

        List<User> newList = new ArrayList<>();

        for (Long aLong : userIdList) {
            newList.add(map.get(aLong).get(0));
        }
        return newList;
    }


}




