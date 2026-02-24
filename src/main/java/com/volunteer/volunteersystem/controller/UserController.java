package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.Entity.User;
import com.volunteer.volunteersystem.mapper.UserMapper;
import com.volunteer.volunteersystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (request.getUserType() == null) {
            return Result.error("请选择用户类型");
        }

        User user = userService.login(request.getUsername(), request.getPassword(), request.getUserType());

        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 志愿者注册
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest request) {
        try {
            // ================== 【新增：后端格式校验】 ==================
            // 1. 校验手机号格式
            String phoneRegex = "^1[3-9]\\d{9}$";
            if (request.getPhone() == null || !request.getPhone().matches(phoneRegex)) {
                return Result.error("手机号格式不正确");
            }

            // 2. 校验身份证号格式
            String idCardRegex = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dX]$";
            if (request.getIdCard() == null || !request.getIdCard().matches(idCardRegex)) {
                return Result.error("身份证号格式不正确");
            }
            // 验证用户名是否存在
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, request.getUsername());
            Long count = userMapper.selectCount(queryWrapper);
            if (count > 0) {
                return Result.error("用户名已存在");
            }

            // 验证手机号是否存在
            queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, request.getPhone());
            count = userMapper.selectCount(queryWrapper);
            if (count > 0) {
                return Result.error("手机号已被注册");
            }

            // 验证身份证号是否存在
            queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getIdCard, request.getIdCard());
            count = userMapper.selectCount(queryWrapper);
            if (count > 0) {
                return Result.error("身份证号已被注册");
            }

            // 创建用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setRealName(request.getRealName());
            user.setPhone(request.getPhone());
            user.setIdCard(request.getIdCard());
            user.setCommunity(request.getCommunity());
            user.setSkills(request.getSkills());
            user.setUserType(request.getUserType() != null ? request.getUserType() : 1); // 志愿者
            user.setStatus(1);
            user.setStarLevel(1);
            user.setTotalHours(new java.math.BigDecimal(0));
            user.setTotalPoints(0);

            int result = userMapper.insert(user);

            if (result > 0) {
                return Result.success("注册成功");
            } else {
                return Result.error("注册失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("注册失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public Result<String> updateUserInfo(@RequestBody UpdateUserRequest request) {
        try {
            boolean success = userService.updateUserInfo(
                    request.getUserId(),
                    request.getRealName(),
                    request.getPhone(),
                    request.getEmail(),
                    request.getAddress(),
                    request.getSkills()
            );

            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            boolean success = userService.changePassword(
                    request.getUserId(),
                    request.getOldPassword(),
                    request.getNewPassword()
            );

            if (success) {
                return Result.success("密码修改成功");
            } else {
                return Result.error("密码修改失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 登录请求参数
     */
    @lombok.Data
    public static class LoginRequest {
        private String username;
        private String password;
        private Integer userType;
    }

    /**
     * 注册请求参数
     */
    @lombok.Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String realName;
        private String phone;
        private String idCard;
        private String community;
        private String skills;
        private Integer userType;
    }

    /**
     * 更新用户信息请求参数
     */
    @lombok.Data
    public static class UpdateUserRequest {
        private Long userId;
        private String realName;
        private String phone;
        private String email;
        private String address;
        private String skills;
    }

    /**
     * 修改密码请求参数
     */
    @lombok.Data
    public static class ChangePasswordRequest {
        private Long userId;
        private String oldPassword;
        private String newPassword;
    }
}