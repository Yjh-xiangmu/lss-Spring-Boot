package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.Entity.ExchangeRecord;
import com.volunteer.volunteersystem.Entity.Product;
import com.volunteer.volunteersystem.Entity.User;
import com.volunteer.volunteersystem.mapper.ExchangeRecordMapper;
import com.volunteer.volunteersystem.mapper.ProductMapper;
import com.volunteer.volunteersystem.mapper.UserMapper;
import com.volunteer.volunteersystem.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExchangeRecordMapper exchangeRecordMapper;

    @Override
    public Page<Product> getProductList(Integer pageNum, Integer pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, 1);  // 只显示上架的商品
        queryWrapper.orderByDesc(Product::getCreateTime);

        return productMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Product getProductById(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务，任何一步报错都会回滚
    public boolean exchangeProduct(Long volunteerId, Long productId, Integer quantity,
                                   String receiverName, String receiverPhone, String receiverAddress) {
        // 1. 检查商品
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        if (product.getStatus() != 1) {
            throw new RuntimeException("商品已下架");
        }

        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足");
        }

        // 2. 检查用户积分
        User user = userMapper.selectById(volunteerId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 【修复】获取单价 (Entity里叫 price)
        int costPoints = product.getPrice() * quantity;

        // 【修复】检查的是“当前余额 points”，而不是“历史总分 TotalPoints”
        Integer currentBalance = user.getPoints() == null ? 0 : user.getPoints();

        if (currentBalance < costPoints) {
            throw new RuntimeException("积分不足");
        }

        // 3. 扣减积分 (扣余额)
        user.setPoints(currentBalance - costPoints);
        userMapper.updateById(user);

        // 4. 扣减库存
        product.setStock(product.getStock() - quantity);
        // 【已删除】product.setExchangedCount(...)
        // 因为简版实体类没有这个字段，直接删掉这行，不影响核心功能
        productMapper.updateById(product);

        // 5. 创建兑换记录
        ExchangeRecord record = new ExchangeRecord();
        record.setOrderNo(generateOrderNo());
        record.setVolunteerId(volunteerId);
        record.setProductId(productId);
        record.setProductName(product.getName());
        record.setQuantity(quantity);
        record.setTotalPoints(costPoints);
        record.setReceiveStatus(0); // 0-未领取
        record.setReceiverName(receiverName);
        record.setReceiverPhone(receiverPhone);
        record.setReceiverAddress(receiverAddress);
        record.setCreateTime(LocalDateTime.now()); // 补上创建时间

        return exchangeRecordMapper.insert(record) > 0;
    }

    // 生成订单号
    private String generateOrderNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "EX" + LocalDateTime.now().format(formatter) + (int)(Math.random() * 1000);
    }
}