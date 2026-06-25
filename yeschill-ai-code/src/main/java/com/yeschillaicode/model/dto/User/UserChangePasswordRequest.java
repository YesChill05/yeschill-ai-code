package com.yeschillaicode.model.dto.User;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserChangePasswordRequest implements Serializable {

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String checkPassword;

    private static final long serialVersionUID = 1L;
}
