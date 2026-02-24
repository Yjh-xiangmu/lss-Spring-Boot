package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.Entity.Article;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.mapper.ArticleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/article")
@CrossOrigin
public class ArticleController {

    // 直接注入 Mapper，省去修改 Service 的麻烦
    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 1. 获取资讯列表 (支持 分页 + 类型筛选 + 关键词搜索)
     */
    @GetMapping("/list")
    public Result<Page<Article>> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer articleType,
            @RequestParam(required = false) String keyword
    ) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        // 筛选类型 (1-公告 2-风采 3-故事)
        if (articleType != null) {
            wrapper.eq(Article::getArticleType, articleType);
        }

        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Article::getTitle, keyword);
        }

        // 排序：置顶的排前面(desc)，然后按创建时间倒序
        wrapper.orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getCreateTime);

        return Result.success(articleMapper.selectPage(page, wrapper));
    }

    /**
     * 2. 获取资讯详情 (并增加浏览量)
     */
    @GetMapping("/{id}")
    public Result<Article> getById(@PathVariable Long id) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            // 浏览量 +1
            article.setViews((article.getViews() == null ? 0 : article.getViews()) + 1);
            articleMapper.updateById(article);
            return Result.success(article);
        } else {
            return Result.error("资讯不存在");
        }
    }

    /**
     * 3. 发布资讯 (管理员用)
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody Article article) {
        // 初始化默认值
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        if (article.getViews() == null) article.setViews(0);
        if (article.getIsTop() == null) article.setIsTop(0); // 默认不置顶

        int rows = articleMapper.insert(article);
        return rows > 0 ? Result.success("发布成功") : Result.error("发布失败");
    }

    /**
     * 4. 修改资讯 (管理员用)
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody Article article) {
        article.setUpdateTime(LocalDateTime.now());
        int rows = articleMapper.updateById(article);
        return rows > 0 ? Result.success("修改成功") : Result.error("修改失败");
    }

    /**
     * 5. 删除资讯 (管理员用)
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        int rows = articleMapper.deleteById(id);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }
}