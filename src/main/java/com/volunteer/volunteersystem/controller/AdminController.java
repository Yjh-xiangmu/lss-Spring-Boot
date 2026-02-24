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
            @RequestParam(required = false) Integer type
    ) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (type != null) {
            wrapper.eq(User::getUserType, type);
        } else {
            wrapper.ne(User::getUserType, 3);
        }

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

    // ================== 新增：用户增删改查与审核模块 ==================

    /**
     * 4. 管理员新增用户
     */
    @PostMapping("/user/add")
    public Result<String> addUser(@RequestBody User user) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return Result.error("用户名和密码不能为空");
        }

        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            return Result.error("用户名已存在");
        }

        if (user.getStatus() == null) user.setStatus(1);
        if (user.getUserType() == null) user.setUserType(1);

        int result = userMapper.insert(user);
        return result > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }

    /**
     * 5. 管理员修改用户
     */
    @PutMapping("/user/update")
    public Result<String> updateUser(@RequestBody User user) {
        if (user.getId() == null) return Result.error("用户ID不能为空");

        // 防呆设计：如果密码为空，说明前端没填新密码，保留老密码不修改
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            User oldUser = userMapper.selectById(user.getId());
            if (oldUser != null) {
                user.setPassword(oldUser.getPassword());
            }
        }

        int result = userMapper.updateById(user);
        return result > 0 ? Result.success("修改成功") : Result.error("修改失败");
    }

    /**
     * 6. 管理员删除用户
     */
    @DeleteMapping("/user/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        int result = userMapper.deleteById(id);
        return result > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 7. 组织者注册审核 (通过/驳回)
     */
    @PutMapping("/user/audit")
    public Result<String> auditUser(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            Integer status = Integer.valueOf(params.get("status").toString()); // 1为通过，3为驳回

            User user = userMapper.selectById(id);
            if (user != null) {
                user.setStatus(status);
                userMapper.updateById(user);
                return Result.success("审核操作成功");
            }
            return Result.error("用户不存在");
        } catch (Exception e) {
            return Result.error("参数错误");
        }
    }

    @Data
    public static class StatusRequest {
        private Long id;
        private Integer status;
    }
}