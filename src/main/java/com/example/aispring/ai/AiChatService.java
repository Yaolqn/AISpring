package com.example.aispring.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI 聊天服务
 * 处理与AI的对话，包括Function Calling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient chatClient;
    private final RestaurantTools restaurantTools;
    private final ChatMemoryService chatMemoryService;

    // 系统提示词，定义AI的角色和行为
    private static final String SYSTEM_PROMPT = """
            你是「美食探索助手」，一个专业的餐厅推荐AI助手。
            
            你的职责是：
            1. 帮助用户发现附近的优质餐厅
            2. 根据用户的位置、口味偏好提供个性化推荐
            3. 回答关于餐厅的各种问题
            
            你可以使用的工具：
            1. findNearbyRestaurants - 搜索附近餐厅（需要用户位置）
            2. searchRestaurants - 按名称或类型搜索餐厅
            3. getRecommendedRestaurants - 获取高评分推荐
            
            当用户询问：
            - "附近有什么好吃的"、"附近有什么餐厅" → 使用 findNearbyRestaurants
            - "附近有什么火锅店"、"找日料" → 使用 findNearbyRestaurants，传入type参数
            - "评分最高的川菜" → 使用 findNearbyRestaurants，传入type=川菜, sortBy=rating
            - "搜索海底捞" → 使用 searchRestaurants，传入name参数
            - "推荐几家餐厅" → 使用 getRecommendedRestaurants
            
            注意事项：
            - 如果用户没有提供位置信息，询问用户当前位置
            - 回答要友好、热情，使用emoji增加亲和力
            - 提供的信息要准确、有用
            """;

    /**
     * 提取公共的 FunctionCallbacks 构建方法
     * 以免在同步和流式方法中重复编写
     */
    @SuppressWarnings("deprecation")
    private FunctionCallback[] getRestaurantFunctionCallbacks() {
        FunctionCallback findNearbyCallback = FunctionCallbackWrapper.builder(
                        (RestaurantTools.FindNearbyRequest request) -> restaurantTools.findNearbyRestaurants(request))
                .withName("findNearbyRestaurants")
                .withDescription("根据用户位置搜索附近的餐厅，支持按类型筛选和排序。参数：latitude(纬度), longitude(经度), radiusKm(半径), type(类型), sortBy(排序), limit(数量)")
                .withInputType(RestaurantTools.FindNearbyRequest.class)
                .build();

        FunctionCallback searchCallback = FunctionCallbackWrapper.builder(
                        (RestaurantTools.SearchRequest request) -> restaurantTools.searchRestaurants(request))
                .withName("searchRestaurants")
                .withDescription("根据餐厅名称或类型搜索餐厅。参数：name(名称), type(类型), sortBy(排序), limit(数量)")
                .withInputType(RestaurantTools.SearchRequest.class)
                .build();

        FunctionCallback recommendCallback = FunctionCallbackWrapper.builder(
                        (RestaurantTools.RecommendRequest request) -> restaurantTools.getRecommendedRestaurants(request))
                .withName("getRecommendedRestaurants")
                .withDescription("获取高评分推荐餐厅。参数：type(类型), limit(数量)")
                .withInputType(RestaurantTools.RecommendRequest.class)
                .build();

        return new FunctionCallback[]{findNearbyCallback, searchCallback, recommendCallback};
    }

    /**
     * 构建包含位置上下文的 Prompt（带记忆功能）
     */
    private Prompt buildPrompt(String userMessage, Double latitude, Double longitude, String sessionId) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        // 添加历史消息（如果有会话ID）
        if (sessionId != null && !sessionId.isEmpty()) {
            List<Message> historyMessages = chatMemoryService.toSpringAiMessages(sessionId);
            messages.addAll(historyMessages);
            log.debug("会话 {} 加载历史消息 {} 条", sessionId, historyMessages.size());
        }

        String enhancedMessage = userMessage;
        if (latitude != null && longitude != null) {
            enhancedMessage = String.format("%s\n[用户当前位置：纬度 %.6f, 经度 %.6f]",
                    userMessage, latitude, longitude);
        }
        messages.add(new UserMessage(enhancedMessage));

        return new Prompt(messages);
    }

    /**
     * 发送消息并获取AI回复（同步，带记忆功能）
     */
    public String chat(String userMessage, Double latitude, Double longitude, String sessionId) {
        // 如果没有提供sessionId，生成一个新的
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        log.info("用户消息: {}, 位置: ({}, {}), 会话: {}", userMessage, latitude, longitude, sessionId);

        // 保存用户消息到历史
        chatMemoryService.addUserMessage(sessionId, userMessage);

        final String finalSessionId = sessionId;

        try {
            Prompt prompt = buildPrompt(userMessage, latitude, longitude, finalSessionId);

            // 使用ChatClient进行Function Calling调用
            ChatResponse response = chatClient.prompt(prompt)
                    .functions(getRestaurantFunctionCallbacks()) // 传入提取的公共函数
                    .call()
                    .chatResponse();

            String result = response.getResult().getOutput().getContent();

            // 保存AI回复到历史
            chatMemoryService.addAssistantMessage(finalSessionId, result);

            log.info("AI回复: {}", result);
            return result;

        } catch (Exception e) {
            log.error("AI对话发生错误", e);
            return "抱歉，我暂时无法回答您的问题，请稍后重试。😔";
        }
    }

    /**
     * 发送消息并获取流式AI回复（用于打字机效果，带记忆功能）
     */
    public Flux<String> chatStream(String userMessage, Double latitude, Double longitude, String sessionId) {
        // 如果没有提供sessionId，生成一个新的
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        log.info("用户消息(流式): {}, 位置: ({}, {}), 会话: {}", userMessage, latitude, longitude, sessionId);

        // 保存用户消息到历史
        chatMemoryService.addUserMessage(sessionId, userMessage);

        final String finalSessionId = sessionId;
        StringBuilder responseBuilder = new StringBuilder();

        try {
            Prompt prompt = buildPrompt(userMessage, latitude, longitude, finalSessionId);

            // 在流式模式下直接加入 functions
            return chatClient.prompt(prompt)
                    .functions(getRestaurantFunctionCallbacks()) // 新增：在流式中启用 Function Calling
                    .stream()
                    .content()
                    .doOnNext(chunk -> responseBuilder.append(chunk))
                    .doOnComplete(() -> {
                        // 流式响应完成后，保存完整回复到历史
                        String fullResponse = responseBuilder.toString();
                        chatMemoryService.addAssistantMessage(finalSessionId, fullResponse);
                        log.info("AI流式回复完成，会话: {}", finalSessionId);
                    });

        } catch (Exception e) {
            log.error("AI流式对话发生错误", e);
            return Flux.just("抱歉，我暂时无法回答您的问题，请稍后重试。😔");
        }
    }

    /**
     * 简单的聊天（不带位置信息和会话ID）
     */
    public String chat(String userMessage) {
        return chat(userMessage, null, null, null);
    }

    /**
     * 简单的聊天（带会话ID）
     */
    public String chat(String userMessage, String sessionId) {
        return chat(userMessage, null, null, sessionId);
    }

    /**
     * 清空指定会话的历史
     */
    public void clearSession(String sessionId) {
        chatMemoryService.clearSession(sessionId);
    }

    /**
     * 获取会话消息数量
     */
    public int getSessionMessageCount(String sessionId) {
        return chatMemoryService.getMessageCount(sessionId);
    }
}