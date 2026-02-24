package com.volunteer.volunteersystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.Entity.Article;

public interface ArticleService {
    /**
     * 获取资讯列表
     */
    Page<Article> getArticleList(Integer articleType, Integer pageNum, Integer pageSize);

    /**
     * 获取资讯详情
     */
    Article getArticleById(Long id);
}