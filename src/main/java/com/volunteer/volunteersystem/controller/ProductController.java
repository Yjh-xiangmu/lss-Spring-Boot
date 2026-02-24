package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.Entity.Product;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.mapper.ProductMapper;
import com.volunteer.volunteersystem.service.ProductService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/product")
@CrossOrigin
public class ProductController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductService productService; // 【新增】注入Service用于处理兑换逻辑

    /**
     * 1. 列表查询 (志愿者/管理员通用)
     */
    @GetMapping("/list")
    public Result<Page<Product>> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) wrapper.like(Product::getName, keyword);
        if (status != null) wrapper.eq(Product::getStatus, status); // 志愿者端传1

        wrapper.orderByDesc(Product::getCreateTime);
        return Result.success(productMapper.selectPage(page, wrapper));
    }

    // ... (中间的 add, update, delete, getById 保持不变) ...
    @PostMapping("/add")
    public Result<String> add(@RequestBody Product product) {
        product.setStatus(1);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        return productMapper.insert(product) > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Product product) {
        product.setUpdateTime(LocalDateTime.now());
        return productMapper.updateById(product) > 0 ? Result.success("更新成功") : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        return productMapper.deleteById(id) > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        return Result.success(productMapper.selectById(id));
    }

    /**
     * 【新增】商品兑换接口 (给志愿者用的)
     */
    @PostMapping("/exchange")
    public Result<String> exchange(@RequestBody ExchangeRequest req) {
        try {
            boolean success = productService.exchangeProduct(
                    req.getVolunteerId(),
                    req.getProductId(),
                    req.getQuantity(), // <--- 这里传前端发来的数量
                    req.getReceiverName(),
                    req.getReceiverPhone(),
                    req.getReceiverAddress()
            );
            return success ? Result.success("兑换成功！") : Result.error("兑换失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // DTO
    // DTO
    @Data
    public static class ExchangeRequest {
        private Long volunteerId;
        private Long productId;
        private Integer quantity; // <--- 必须有这个，适配你的前端选择数量功能
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
    }
}