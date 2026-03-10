package com.example.aispring.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 * 存储单条对话消息的信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息角色：user-用户，assistant-AI助手
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 创建用户消息
     */
    public static ChatMessage user(String content) {
        return ChatMessage.builder()
                .role("user")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建AI助手消息
     */
    public static ChatMessage assistant(String content) {
        return ChatMessage.builder()
                .role("assistant")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
