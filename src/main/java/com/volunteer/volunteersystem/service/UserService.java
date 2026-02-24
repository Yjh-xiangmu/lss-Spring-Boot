package com.volunteer.volunteersystem.service;

import com.volunteer.volunteersystem.Entity.User;

public interface UserService {
    /**
     * 用户登录
     */
    User login(String username, String password, Integer userType);

    /**
     * 更新用户信息
     */
    boolean updateUserInfo(Long userId, String realName, String phone, String email, String address, String skills);

    /**
     * 修改密码
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 获取用户信息
     */
    User getUserById(Long userId);
}