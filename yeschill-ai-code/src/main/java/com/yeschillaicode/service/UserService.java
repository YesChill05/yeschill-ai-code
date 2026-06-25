package com.yeschillaicode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yeschillaicode.model.dto.User.UserQueryRequest;
import com.yeschillaicode.model.entity.User;
import com.yeschillaicode.model.vo.User.LoginUserVO;
import com.yeschillaicode.model.vo.User.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String userName);


    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);
    /**
     * 用户登录
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param request     保存用户登录态
     * @return 用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

/**
 * 根据HTTP请求获取登录用户信息
 * @param request HttpServletRequest对象，包含当前HTTP请求的所有信息
 * @return 返回当前登录用户的User对象，如果用户未登录则可能返回null
 */
    User getLoginUser(HttpServletRequest request);  // 根据请求获取并返回当前登录用户信息
/**
 * 根据用户信息获取登录用户视图对象
 *
 * @param user 用户信息实体对象
 * @return LoginUserVO 登录用户视图对象，包含用户登录所需展示的信息
 */
    LoginUserVO getLoginUserVO(User user);
/**
 * 根据User对象获取对应的UserVO(Value Object)对象
 * UserVO通常用于数据传输和展示，可能包含与User实体不同的数据结构或过滤后的敏感信息
 *
 * @param user 用户实体对象，包含用户的基本信息
 * @return UserVO 用户视图对象，用于前端展示或数据传输
 */
    UserVO getUserVO(User user);
    /**
     * 根据User对象获取对应的UserVO(Value Object)对象   分页
     * UserVO通常用于数据传输和展示，可能包含与User实体不同的数据结构或过滤后的敏感信息
     *
     * @param userlist 用户列表实体对象，包含用户的基本信息
     * @return UserVO 用户视图对象，用于前端展示或数据传输
     */
    List<UserVO> getUserVOList(List<User> userlist);
/**
 * 用户登出方法
 * @param request HttpServletRequest对象，用于获取客户端请求信息
 * @return 返回一个布尔值，表示用户登出操作是否成功
 */
    boolean userLogout(HttpServletRequest request);
/**
 * 根据用户查询请求参数构建查询条件包装器
 *
 * @param userQueryRequest 用户查询请求对象，包含查询条件参数
 * @return QueryWrapper 返回一个包含查询条件的QueryWrapper对象，
 *         用于构建数据库查询条件
 */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
}
