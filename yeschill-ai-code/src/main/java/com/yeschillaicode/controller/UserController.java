package com.yeschillaicode.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.yeschillaicode.annotation.AuthCheck;
import com.yeschillaicode.common.BaseResponse;
import com.yeschillaicode.common.DeleteRequest;
import com.yeschillaicode.common.ResultUtils;
import com.yeschillaicode.constant.UserConstant;
import com.yeschillaicode.exception.BusinessException;
import com.yeschillaicode.exception.ErrorCode;
import com.yeschillaicode.exception.ThrowUtils;
import com.yeschillaicode.manager.CosManager;
import com.yeschillaicode.model.dto.User.*;
import com.yeschillaicode.model.vo.User.LoginUserVO;
import com.yeschillaicode.model.vo.User.UserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yeschillaicode.model.entity.User;
import com.yeschillaicode.service.UserService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 用户 控制层。
 *
 */
@RestController
@RequestMapping("/user")
public class UserController {

   @Resource
    private UserService userService;
   @Resource
    private CosManager cosManager;
    /**
     * 用户登出
     * @param request 请求对象
     * @return 登出结果
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){
         ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

     /**
     * 用户登录
     * @param userLoginRequest 用户登录请求
      * @param request 请求对象
     * @return 脱敏后的用户信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
            ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
            String userAccount = userLoginRequest.getUserAccount();
            String userPassword = userLoginRequest.getUserPassword();
            LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
            return ResultUtils.success(loginUserVO);
    }
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }


    /**
     *   用户注册
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){

        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String userName = userRegisterRequest.getUserName();
        long result = userService.userRegister(userAccount, userPassword, checkPassword, userName);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 用户更新自己的信息
     *
     * @param userUpdateMyRequest 用户自更新请求
     * @param request             请求
     * @return 更新结果
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                              HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        user.setId(loginUser.getId());
        user.setUserName(userUpdateMyRequest.getUserName());
        user.setUserAvatar(userUpdateMyRequest.getUserAvatar());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 用户上传头像
     *
     * @param file    头像文件
     * @param request 请求
     * @return 头像URL
     */
    @PostMapping("/upload-avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(file.getSize() > 2 * 1024 * 1024, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase() : "";
        Set<String> allowedSuffixes = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp");
        ThrowUtils.throwIf(!allowedSuffixes.contains(suffix), ErrorCode.PARAMS_ERROR, "仅支持 png、jpg、jpeg、gif、webp 格式");
        try {
            User loginUser = userService.getLoginUser(request);
            // 构建COS对象键
            String cosKey = "/avatar/" + loginUser.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + suffix;
            // 临时文件
            File tempFile = FileUtil.createTempFile(suffix, true);
            file.transferTo(tempFile);
            // 上传到COS
            String avatarUrl = cosManager.uploadFile(cosKey, tempFile);
            ThrowUtils.throwIf(avatarUrl == null, ErrorCode.SYSTEM_ERROR, "头像上传失败");
            // 删除临时文件
            tempFile.delete();
            // 更新用户头像
            User updateUser = new User();
            updateUser.setId(loginUser.getId());
            updateUser.setUserAvatar(avatarUrl);
            userService.updateById(updateUser);
            return ResultUtils.success(avatarUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        }
    }

    /**
     * 用户修改密码
     *
     * @param userChangePasswordRequest 修改密码请求
     * @param request                   请求
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public BaseResponse<Boolean> changePassword(@RequestBody UserChangePasswordRequest userChangePasswordRequest,
                                                HttpServletRequest request) {
        if (userChangePasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String oldPassword = userChangePasswordRequest.getOldPassword();
        String newPassword = userChangePasswordRequest.getNewPassword();
        String checkPassword = userChangePasswordRequest.getCheckPassword();
        ThrowUtils.throwIf(StrUtil.isBlank(oldPassword), ErrorCode.PARAMS_ERROR, "旧密码不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(newPassword), ErrorCode.PARAMS_ERROR, "新密码不能为空");
        ThrowUtils.throwIf(newPassword.length() < 8, ErrorCode.PARAMS_ERROR, "新密码长度不能小于8位");
        ThrowUtils.throwIf(!newPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        User loginUser = userService.getLoginUser(request);
        // 验证旧密码
        String encryptOldPassword = userService.getEncryptPassword(oldPassword);
        if (!encryptOldPassword.equals(loginUser.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
        }
        // 新密码不能和旧密码一样
        String encryptNewPassword = userService.getEncryptPassword(newPassword);
        if (encryptNewPassword.equals(loginUser.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能与旧密码相同");
        }
        User updateUser = new User();
        updateUser.setId(loginUser.getId());
        updateUser.setUserPassword(encryptNewPassword);
        boolean result = userService.updateById(updateUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

}
