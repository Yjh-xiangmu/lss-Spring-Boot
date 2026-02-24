package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.Entity.Activity;
import com.volunteer.volunteersystem.Entity.User;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.mapper.ActivityMapper;
import com.volunteer.volunteersystem.mapper.ServiceRecordMapper;
import com.volunteer.volunteersystem.mapper.UserMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ServiceRecordMapper serviceRecordMapper;

    /**
     * 1. 首页数据统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getGlobalStats() {
        Map<String, Object> map = new HashMap<>();
        try {
            Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>().ne(User::getUserType, 3));
            Long activityCount = activityMapper.selectCount(null);
            Long recordCount = serviceRecordMapper.selectCount(null);

            map.put("userCount", userCount);
            map.put("activityCount", activityCount);
            map.put("totalRecords", recordCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.success(map);
    }

    /**
     * 2. 用户管理列表 (支持按类型筛选)
     * @param type 1-志愿者 2-组织者 (不传则查所有非管理员)
     */
    @GetMapping("/users")
    public Result<Page<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type // 【新增】筛选用户类型
    ) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 如果传了type，就精准查询；没传就查所有非管理员
        if (type != null) {
            wrapper.eq(User::getUserType, type);
        } else {
            wrapper.ne(User::getUserType, 3);
        }

        // 搜索逻辑
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getPhone, keyword));
        }

        wrapper.orderByDesc(User::getCreateTime);
        return Result.success(userMapper.selectPage(page, wrapper));
    }

    /**
     * 3. 封禁/解封用户
     */
    @PutMapping("/user/status")
    public Result<String> updateUserStatus(@RequestBody StatusRequest req) {
        User user = userMapper.selectById(req.getId());
        if (user == null) return Result.error("用户不存在");

        user.setStatus(req.getStatus()); // 1-正常 0-禁用
        userMapper.updateById(user);

        return Result.success(req.getStatus() == 1 ? "用户已解封" : "用户已封禁");
    }

    @Data
    public static class StatusRequest {
        private Long id;
        private Integer status;
    }
}