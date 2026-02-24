package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.volunteersystem.Entity.Article;
import com.volunteer.volunteersystem.mapper.ArticleMapper;
import com.volunteer.volunteersystem.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public Page<Article> getArticleList(Integer articleType, Integer pageNum, Integer pageSize) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();

        // 【修复点】这里之前可能写成了 getStatus，现在改成正确的 getArticleType
        if (articleType != null) {
            queryWrapper.eq(Article::getArticleType, articleType);
        }

        // 排序：置顶的排前面，然后按时间倒序
        queryWrapper.orderByDesc(Article::getIsTop);
        queryWrapper.orderByDesc(Article::getCreateTime);

        return articleMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Article getArticleById(Long id) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            // 浏览量 +1
            Integer views = article.getViews() == null ? 0 : article.getViews();
            article.setViews(views + 1);
            articleMapper.updateById(article);
        }
        return article;
    }
}