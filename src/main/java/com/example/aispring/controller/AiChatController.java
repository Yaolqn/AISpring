package com.example.aispring.controller;

import com.example.aispring.ai.AiChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * AI 聊天控制器
 * 提供AI对话接口，支持Function Calling
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * AI 对话接口（同步，带记忆功能）
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    public ChatResponseWithSession chat(@RequestBody ChatRequest request) {
        log.info("收到AI聊天请求: {}, 会话: {}", request.getMessage(), request.getSessionId());

        // 如果没有sessionId，生成一个新的
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            log.info("生成新会话ID: {}", sessionId);
        }

        String response = aiChatService.chat(
                request.getMessage(),
                request.getLatitude(),
                request.getLongitude(),
                sessionId
        );

        return new ChatResponseWithSession(response, sessionId);
    }

    /**
     * AI 对话接口（流式，带记忆功能）
     * POST /api/ai/chat/stream
     * 返回 text/event-stream 格式，用于前端打字机效果
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        log.info("收到AI流式聊天请求: {}, 会话: {}", request.getMessage(), request.getSessionId());

        // 如果没有sessionId，生成一个新的
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            log.info("生成新流式会话ID: {}", sessionId);
        }

        final String finalSessionId = sessionId;

        return aiChatService.chatStream(
                request.getMessage(),
                request.getLatitude(),
                request.getLongitude(),
                finalSessionId
        );
    }

    /**
     * 简单对话接口（GET方式，用于测试）
     * GET /api/ai/chat?message=xxx&sessionId=xxx
     */
    @GetMapping("/chat")
    public ChatResponseWithSession chatSimple(@RequestParam String message,
                                               @RequestParam(required = false) Double lat,
                                               @RequestParam(required = false) Double lng,
                                               @RequestParam(required = false) String sessionId) {
        log.info("收到AI简单聊天请求: {}, 位置: ({}, {}), 会话: {}", message, lat, lng, sessionId);

        // 如果没有sessionId，生成一个新的
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            log.info("生成新简单会话ID: {}", sessionId);
        }

        String response = aiChatService.chat(message, lat, lng, sessionId);
        return new ChatResponseWithSession(response, sessionId);
    }

    /**
     * 清空会话历史
     * POST /api/ai/chat/clear
     */
    @PostMapping("/chat/clear")
    public ClearResponse clearSession(@RequestBody ClearRequest request) {
        log.info("清空会话请求: {}", request.getSessionId());

        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            aiChatService.clearSession(request.getSessionId());
            return new ClearResponse("success", "会话历史已清空");
        } else {
            return new ClearResponse("error", "会话ID不能为空");
        }
    }

    /**
     * 清空会话请求
     */
    @Data
    public static class ClearRequest {
        private String sessionId;
    }

    /**
     * 清空会话响应
     */
    @Data
    @RequiredArgsConstructor
    public static class ClearResponse {
        private final String status;
        private final String message;
    }

    // ==================== 请求/响应类 ====================

    /**
     * 聊天请求
     */
    @Data
    public static class ChatRequest {
        /**
         * 用户消息
         */
        private String message;

        /**
         * 用户纬度（可选）
         */
        private Double latitude;

        /**
         * 用户经度（可选）
         */
        private Double longitude;

        /**
         * 会话ID（可选，用于保持对话上下文）
         * 如果不提供，服务端会生成新的会话ID
         */
        private String sessionId;
    }

    /**
     * 带会话ID的聊天响应
     */
    @Data
    @RequiredArgsConstructor
    public static class ChatResponseWithSession {
        /**
         * AI回复内容
         */
        private final String response;

        /**
         * 会话ID，用于后续对话保持上下文
         */
        private final String sessionId;

        /**
         * 响应状态
         */
        private final String status = "success";
    }

    /**
     * 聊天响应
     */
    @Data
    @RequiredArgsConstructor
    public static class ChatResponse {
        /**
         * AI回复内容
         */
        private final String response;
        
        /**
         * 响应状态
         */
        private final String status = "success";
    }
}
