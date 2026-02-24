package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.volunteersystem.Entity.Activity;
import com.volunteer.volunteersystem.Entity.ServiceRecord;
import com.volunteer.volunteersystem.Entity.User;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.mapper.ActivityMapper;
import com.volunteer.volunteersystem.mapper.ServiceRecordMapper;
import com.volunteer.volunteersystem.mapper.UserMapper;
import com.volunteer.volunteersystem.utils.DeepSeekUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AiController {

    @Autowired
    private DeepSeekUtil deepSeekUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ServiceRecordMapper serviceRecordMapper;

    /**
     * 智能对话接口 (全知全能·用户画像增强版)
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, Object> params) {
        String message = (String) params.get("message");
        Long volunteerId = params.get("volunteerId") != null ? Long.valueOf(params.get("volunteerId").toString()) : null;

        if (message == null || message.trim().isEmpty()) {
            return Result.error("请输入内容");
        }

        // === 1. 构建“超详细用户画像” (Context) ===
        StringBuilder userProfile = new StringBuilder("未登录访客");

        if (volunteerId != null) {
            User user = userMapper.selectById(volunteerId);
            if (user != null) {
                // A. 基础信息
                userProfile = new StringBuilder(String.format(
                        "【用户信息】：\n- 昵称：%s\n- 居住社区：%s\n- 擅长技能：%s\n- 荣誉星级：%d星\n- 当前积分余额：%d\n- 累计服务时长：%s小时\n",
                        user.getRealName(),
                        user.getCommunity() != null ? user.getCommunity() : "未填写",
                        user.getSkills() != null ? user.getSkills() : "未填写",
                        user.getStarLevel(),
                        user.getPoints(),
                        user.getTotalHours()
                ));

                // B. 历史活动查询 (查询最近5条审核通过的记录)
                List<ServiceRecord> historyRecords = serviceRecordMapper.selectList(new LambdaQueryWrapper<ServiceRecord>()
                        .eq(ServiceRecord::getVolunteerId, volunteerId)
                        .eq(ServiceRecord::getAuditStatus, 1) // 只看审核通过的
                        .orderByDesc(ServiceRecord::getServiceDate)
                        .last("limit 5"));

                if (!historyRecords.isEmpty()) {
                    userProfile.append("- 最近参与的活动：");
                    for (ServiceRecord record : historyRecords) {
                        // 查活动名
                        Activity act = activityMapper.selectById(record.getActivityId());
                        String actTitle = (act != null) ? act.getTitle() : "未知活动";
                        userProfile.append(actTitle).append("、");
                    }
                    userProfile.deleteCharAt(userProfile.length() - 1); // 删掉最后一个顿号
                    userProfile.append("\n");
                } else {
                    // 如果有时长但没记录，说明是历史数据或系统录入
                    if (user.getTotalHours() != null && user.getTotalHours().doubleValue() > 0) {
                        userProfile.append("- 历史记录：系统迁移数据 (有旧时长但无明细)\n");
                    } else {
                        userProfile.append("- 历史记录：暂无参与记录\n");
                    }
                }
            }
        }

        // === 2. 构建“全知活动数据库” (RAG) ===
        // 查询所有正在进行或招募的活动
        List<Activity> activities = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .in(Activity::getStatus, 1, 2)
                .orderByDesc(Activity::getCreateTime));

        StringBuilder activityData = new StringBuilder();
        if (activities.isEmpty()) {
            activityData.append("当前系统暂无正在进行的活动。");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM月dd日");
            for (Activity a : activities) {
                activityData.append(String.format("- 【%s】(类型:%s | 地点:%s | 积分:%d | 时间:%s | 状态:%s)\n",
                        a.getTitle(), a.getActivityType(), a.getArea(), a.getRewardPoints(),
                        a.getActivityStartTime().format(fmt),
                        a.getStatus() == 1 ? "招募中" : "进行中"));
            }
        }

        // === 3. 注入灵魂的 System Prompt ===
        String finalSystemPrompt = String.format("""
            你叫'小爱'，是【爱心互助社区】的智能管家。你拥有上帝视角，了解用户的一切，也知道系统的所有活动。
            
            %s
            
            【系统实时活动池】：
            %s
            
            【你的最高行为准则】：
            1. **基于画像聊天**：
               - 如果用户有擅长技能（如摄影），推荐活动时优先推荐相关的（如宣传拍摄）。
               - 如果用户社区在"阳光社区"，优先推荐同社区的活动。
               - 如果用户"有积分但无记录"，说明是老用户，不要说他没参加过活动，要说"您的历史贡献我们都记得"。
            2. **精准推荐**：用户问"有什么活动"，请根据【实时活动池】推荐，不要瞎编。
            3. **高情商**：
               - 拒绝机械报数！不要一上来就念经一样报时长。
               - 面对负面情绪（累、烦），做一个温柔的倾听者，不要谈工作和积分。
            
            请用温暖、亲切、简练的口语回答。
            """, userProfile.toString(), activityData.toString());

        // 4. 发送请求
        String reply = deepSeekUtil.chat(message, finalSystemPrompt);
        return Result.success(reply);
    }

    // 下面的 generateReport 方法保持不变...
    @PostMapping("/report")
    public Result<String> generateReport(@RequestBody Map<String, Object> params) {
        // ... (保持你之前的代码不动) ...
        // 为了节省篇幅，这里省略，请保留你原文件中 generateReport 的完整代码
        // 如果你原文件里删了，请把上一次我发给你的 generateReport 再贴回来
        Long volunteerId = Long.valueOf(params.get("volunteerId").toString());
        User user = userMapper.selectById(volunteerId);
        if (user == null) return Result.error("用户不存在");

        List<ServiceRecord> records = serviceRecordMapper.selectList(
                new LambdaQueryWrapper<ServiceRecord>()
                        .eq(ServiceRecord::getVolunteerId, volunteerId)
                        .eq(ServiceRecord::getAuditStatus, 1)
        );

        int count = records.size();
        double totalHours = records.stream().mapToDouble(r -> r.getServiceHours().doubleValue()).sum();
        StringBuilder activityNames = new StringBuilder();
        for (int i = 0; i < Math.min(records.size(), 5); i++) {
            Activity act = activityMapper.selectById(records.get(i).getActivityId());
            if(act != null) activityNames.append(act.getTitle()).append("、");
        }
        int currentYear = LocalDate.now().getYear();
        String prompt = String.format("""
            你是一位情感细腻的作家。请根据以下数据，为志愿者【%s】写一份感人至深的【%d年度志愿服务总结】。
            【当前年份】：%d年
            【数据】：累计服务时长 %.1f 小时，参与活动 %d 次，经历关键词：%s
            【要求】：返回纯 HTML 代码片段（无markdown标记），标题含年份，包含 summary, tags, persona 四个div结构。语气温暖有力。
            """, user.getRealName(), currentYear, currentYear, totalHours, count, activityNames.toString());

        String reply = deepSeekUtil.chat("生成报告", prompt);
        reply = reply.replace("```html", "").replace("```", "");
        return Result.success(reply);
    }
}