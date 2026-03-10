package com.example.aispring.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 聊天记忆服务
 * 管理多会话的聊天历史记录（内存存储）
 */
@Slf4j
@Service
public class ChatMemoryService {

    /**
     * 存储会话历史：sessionId -> 消息列表
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, List<ChatMessage>> sessionMemory = new ConcurrentHashMap<>();

    /**
     * 默认最大保留消息数量（防止内存溢出）
     */
    private static final int DEFAULT_MAX_MESSAGES = 20;

    /**
     * 获取或创建会话历史
     */
    public List<ChatMessage> getSessionHistory(String sessionId) {
        return sessionMemory.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    /**
     * 添加用户消息到会话历史
     */
    public void addUserMessage(String sessionId, String content) {
        List<ChatMessage> history = getSessionHistory(sessionId);
        history.add(ChatMessage.user(content));
        trimHistory(history);
        log.debug("会话 {} 添加用户消息，当前历史数: {}", sessionId, history.size());
    }

    /**
     * 添加AI回复到会话历史
     */
    public void addAssistantMessage(String sessionId, String content) {
        List<ChatMessage> history = getSessionHistory(sessionId);
        history.add(ChatMessage.assistant(content));
        trimHistory(history);
        log.debug("会话 {} 添加AI消息，当前历史数: {}", sessionId, history.size());
    }

    /**
     * 将历史记录转换为 Spring AI Message 列表
     */
    public List<Message> toSpringAiMessages(String sessionId) {
        List<ChatMessage> history = getSessionHistory(sessionId);
        return history.stream()
                .map(msg -> {
                    if ("user".equals(msg.getRole())) {
                        return new UserMessage(msg.getContent());
                    } else {
                        return new AssistantMessage(msg.getContent());
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 清空指定会话的历史
     */
    public void clearSession(String sessionId) {
        sessionMemory.remove(sessionId);
        log.info("清空会话 {} 的历史记录", sessionId);
    }

    /**
     * 获取所有会话ID
     */
    public Set<String> getAllSessionIds() {
        return new HashSet<>(sessionMemory.keySet());
    }

    /**
     * 检查会话是否存在
     */
    public boolean hasSession(String sessionId) {
        return sessionMemory.containsKey(sessionId);
    }

    /**
     * 获取会话消息数量
     */
    public int getMessageCount(String sessionId) {
        List<ChatMessage> history = sessionMemory.get(sessionId);
        return history != null ? history.size() : 0;
    }

    /**
     * 修剪历史记录，防止内存溢出
     * 保留最近的消息
     */
    private void trimHistory(List<ChatMessage> history) {
        if (history.size() > DEFAULT_MAX_MESSAGES) {
            // 保留最近的消息，移除最旧的消息
            while (history.size() > DEFAULT_MAX_MESSAGES) {
                history.remove(0);
            }
        }
    }
}
