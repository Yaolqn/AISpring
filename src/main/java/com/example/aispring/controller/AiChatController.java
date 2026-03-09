package com.example.aispring.controller;

import com.example.aispring.ai.AiChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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
     * AI 对话接口（同步）
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        log.info("收到AI聊天请求: {}", request.getMessage());
        
        String response = aiChatService.chat(
                request.getMessage(),
                request.getLatitude(),
                request.getLongitude()
        );
        
        return new ChatResponse(response);
    }

    /**
     * AI 对话接口（流式）
     * POST /api/ai/chat/stream
     * 返回 text/event-stream 格式，用于前端打字机效果
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        log.info("收到AI流式聊天请求: {}", request.getMessage());
        
        return aiChatService.chatStream(
                request.getMessage(),
                request.getLatitude(),
                request.getLongitude()
        );
    }

    /**
     * 简单对话接口（GET方式，用于测试）
     * GET /api/ai/chat?message=xxx
     */
    @GetMapping("/chat")
    public ChatResponse chatSimple(@RequestParam String message,
                                    @RequestParam(required = false) Double lat,
                                    @RequestParam(required = false) Double lng) {
        log.info("收到AI简单聊天请求: {}, 位置: ({}, {})", message, lat, lng);
        
        String response = aiChatService.chat(message, lat, lng);
        return new ChatResponse(response);
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
