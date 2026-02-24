package com.volunteer.volunteersystem.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.Duration;
@Component
public class DeepSeekUtil {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String apiUrl;

    // 保持连接配置，防止超时
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30)) // 连接超时 30s
            .readTimeout(Duration.ofSeconds(60))    // 读取超时 60s
            .writeTimeout(Duration.ofSeconds(60))   // 写入超时 60s
            .build();

    /**
     * 进阶版聊天方法
     * @param userMessage 用户的问题
     * @param dynamicSystemPrompt 动态生成的系统人设（包含数据库里的实时数据）
     */
    public String chat(String userMessage, String dynamicSystemPrompt) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "deepseek-chat");
            payload.put("temperature", 0.7);

            List<Map<String, String>> messages = new ArrayList<>();

            // 1. 注入动态系统提示词 (最关键的一步！)
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", dynamicSystemPrompt);
            messages.add(systemMsg);

            // 2. 用户问题
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            payload.put("messages", messages);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    JSON.toJSONString(payload)
            );

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) return "AI 响应异常: " + response.code();
                String resStr = response.body().string();
                JSONObject json = JSON.parseObject(resStr);
                return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "小爱正在思考人生，请稍后再试...";
        }
    }
}