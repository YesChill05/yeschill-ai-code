package com.yeschillaicode.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yeschillaicode.model.dto.User.UserQueryRequest;
import com.yeschillaicode.model.enums.UserRoleEnum;
import com.yeschillaicode.exception.BusinessException;
import com.yeschillaicode.exception.ErrorCode;
import com.yeschillaicode.model.entity.User;
import com.yeschillaicode.mapper.UserMapper;
import com.yeschillaicode.model.vo.User.LoginUserVO;
import com.yeschillaicode.model.vo.User.UserVO;
import com.yeschillaicode.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.stream.Collectors;

import static com.yeschillaicode.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String userName) {
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");}
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");}
        if (userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");}
        if (!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");}
        //2.查询用户名是否已存在
        QueryWrapper queryWrapper= new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count  = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        //3.加密密码
        String encryptPassword = getEncryptPassword(userPassword);

        //4.创建用户
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        String finalUserName = StrUtil.isNotBlank(userName) ? userName : userAccount;
        if (finalUserName.length() > 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称长度不能超过8个字符");
        }
        user.setUserName(finalUserName);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败,数据库异常  ");
        }
        return user.getId();
    }


    @Override
    public String getEncryptPassword(String userPassword){
        //盐值 混淆密码
        final String SALT = "yeschill";

        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }
    public LoginUserVO getLoginUserVO(User user){
        if(user == null){
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if(user == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;


    }

    @Override
    public List<UserVO> getUserVOList(List<User> userlist) {
        if(userlist == null)
            return null;
        return userlist.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        //先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"用户未登录");
        }
        //用户已经登录，移除用户的登录状态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }


    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        //2.加密
        String encryptPassword = getEncryptPassword(userPassword);
        //3.查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        //4记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return getLoginUserVO(user);

    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        //先判断用户是否登录
         Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
         User currentUser= (User) userObj;
         if (currentUser == null){
             throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
         }
         //用户已经登录，从数据库查当前用户信息
         Long userId = currentUser.getId();
         currentUser = this.getById(userId);
         if (currentUser == null){
             throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
         }
         return currentUser;
    }

}
