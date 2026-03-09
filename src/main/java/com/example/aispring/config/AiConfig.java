package com.example.aispring.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置类
 * 配置 ChatClient 用于 AI 对话
 */
@Configuration
public class AiConfig {

    /**
     * 配置 ChatClient
     * 使用 Spring AI 自动配置的 OpenAI ChatModel
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
