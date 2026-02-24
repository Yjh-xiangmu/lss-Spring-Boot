package com.volunteer.volunteersystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.Entity.Product;

public interface ProductService {
    /**
     * 获取商品列表
     */
    Page<Product> getProductList(Integer pageNum, Integer pageSize);

    /**
     * 获取商品详情
     */
    Product getProductById(Long id);

    /**
     * 兑换商品
     */
    boolean exchangeProduct(Long volunteerId, Long productId, Integer quantity,
                            String receiverName, String receiverPhone, String receiverAddress);
}