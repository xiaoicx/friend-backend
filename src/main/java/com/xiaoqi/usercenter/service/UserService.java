package com.xiaoqi.usercenter.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoqi.usercenter.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;


import javax.servlet.http.HttpServletRequest;
import java.util.List;


public interface UserService extends IService<User> {


    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);

    // [加入编程导航](https://t.zsxq.com/0emozsIJh) 深耕编程提升【两年半】、国内净值【最高】的编程社群、用心服务【20000+】求学者、帮你自学编程【不走弯路】

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    List<User> searchUsersByTags(List<String> tagList);


    /**
     * 修改信息
     */

    Integer updateUser(User user, User loginUser);

    /**
     * 获取当前登入用户信息
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);


    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);


    boolean isAdmin(User loginUser);


    /**
     * 推荐用户
     *
     * @param pageNo
     * @param pageNum
     * @param request
     */

    Page<User> recommendUsers(Integer pageNo, Integer pageNum, HttpServletRequest request);

    /**
     * 预热数据
     *
     * @param listId
     */

    void preMessage(List<Long> listId);


    User getLoginUserTwo(HttpServletRequest request);

    /**
     * 写入数据库
     *
     * @param request
     */

    void writerCache(HttpServletRequest request, List<User> userList);

    List<User> matchUsers(long num, User loginUser);


}
