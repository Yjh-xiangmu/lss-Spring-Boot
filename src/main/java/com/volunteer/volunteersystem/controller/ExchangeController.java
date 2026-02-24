package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.Entity.ExchangeRecord;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.mapper.ExchangeRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/exchange")
@CrossOrigin
public class ExchangeController {

    @Autowired
    private ExchangeRecordMapper exchangeRecordMapper;

    /**
     * 1. 志愿者查询自己的兑换记录
     */
    @GetMapping("/my")
    public Result<Page<ExchangeRecord>> getMyExchanges(
            @RequestParam Long volunteerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<ExchangeRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ExchangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExchangeRecord::getVolunteerId, volunteerId);
        wrapper.orderByDesc(ExchangeRecord::getCreateTime);

        return Result.success(exchangeRecordMapper.selectPage(page, wrapper));
    }

    /**
     * 2. 管理员查询所有订单
     */
    @GetMapping("/list")
    public Result<Page<ExchangeRecord>> getAllExchanges(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status // 0-待发货 1-已发货
    ) {
        Page<ExchangeRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ExchangeRecord> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(ExchangeRecord::getReceiveStatus, status);
        }
        wrapper.orderByDesc(ExchangeRecord::getCreateTime);

        return Result.success(exchangeRecordMapper.selectPage(page, wrapper));
    }

    /**
     * 3. 管理员发货
     */
    @PutMapping("/ship/{id}")
    public Result<String> shipOrder(@PathVariable Long id) {
        ExchangeRecord record = exchangeRecordMapper.selectById(id);
        if (record == null) return Result.error("订单不存在");

        record.setReceiveStatus(1); // 1 代表已发货/已领取
        record.setUpdateTime(LocalDateTime.now());
        exchangeRecordMapper.updateById(record);

        return Result.success("发货成功");
    }
}