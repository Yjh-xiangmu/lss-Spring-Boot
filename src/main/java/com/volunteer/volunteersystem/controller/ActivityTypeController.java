package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.Entity.ActivityType;
import com.volunteer.volunteersystem.service.ActivityTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity-type")
@CrossOrigin
public class ActivityTypeController {

    @Autowired
    private ActivityTypeService activityTypeService;

    // 1. 获取所有活动类型（包含禁用的，供管理员列表展示）
    @GetMapping("/all")
    public Result<List<ActivityType>> getAllList() {
        LambdaQueryWrapper<ActivityType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ActivityType::getSort);
        return Result.success(activityTypeService.list(wrapper));
    }

    // 2. 获取启用的活动类型（供组织者下拉框使用，你之前测试通的那个）
    @GetMapping("/list")
    public Result<List<ActivityType>> getList() {
        LambdaQueryWrapper<ActivityType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityType::getStatus, 1).orderByAsc(ActivityType::getSort);
        return Result.success(activityTypeService.list(wrapper));
    }

    // 3. 新增类型
    @PostMapping("/add")
    public Result<String> addType(@RequestBody ActivityType type) {
        if (type.getTypeName() == null || type.getTypeName().trim().isEmpty()) {
            return Result.error("类型名称不能为空");
        }
        if (type.getSort() == null) type.setSort(0);
        if (type.getStatus() == null) type.setStatus(1);

        boolean success = activityTypeService.save(type);
        return success ? Result.success("添加成功") : Result.error("添加失败");
    }

    // 4. 修改类型
    @PutMapping("/update")
    public Result<String> updateType(@RequestBody ActivityType type) {
        if (type.getId() == null) return Result.error("ID不能为空");
        boolean success = activityTypeService.updateById(type);
        return success ? Result.success("修改成功") : Result.error("修改失败");
    }

    // 5. 删除类型 (物理删除)
    @DeleteMapping("/{id}")
    public Result<String> deleteType(@PathVariable Integer id) {
        boolean success = activityTypeService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}