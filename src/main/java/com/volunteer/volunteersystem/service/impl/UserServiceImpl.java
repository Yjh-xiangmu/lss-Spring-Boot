package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.volunteersystem.Entity.User;
import com.volunteer.volunteersystem.mapper.UserMapper;
import com.volunteer.volunteersystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(String username, String password, Integer userType) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username)
                .eq(User::getPassword, password)
                .eq(User::getUserType, userType)
                .eq(User::getStatus, 1);

        User user = userMapper.selectOne(queryWrapper);

        return user;
    }

    @Override
    public boolean updateUserInfo(Long userId, String realName, String phone, String email, String address, String skills) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查手机号是否被其他用户使用
        if (phone != null && !phone.equals(user.getPhone())) {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone)
                    .ne(User::getId, userId);
            Long count = userMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new RuntimeException("手机号已被使用");
            }
        }

        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setAddress(address);
        user.setSkills(skills);

        return userMapper.updateById(user) > 0;
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!user.getPassword().equals(oldPassword)) {
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(newPassword);
        return userMapper.updateById(user) > 0;
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
}