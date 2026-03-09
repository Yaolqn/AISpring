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
import java.util.function.Function;

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
     * 发送消息并获取AI回复（同步）
     *
     * @param userMessage 用户消息
     * @param latitude    用户纬度（可选）
     * @param longitude   用户经度（可选）
     * @return AI回复
     */
    public String chat(String userMessage, Double latitude, Double longitude) {
        log.info("用户消息: {}, 位置: ({}, {})", userMessage, latitude, longitude);

        try {
            // 构建消息列表
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(SYSTEM_PROMPT));
            
            // 如果有位置信息，添加到上下文
            String enhancedMessage = userMessage;
            if (latitude != null && longitude != null) {
                enhancedMessage = String.format("%s\n[用户当前位置：纬度 %.6f, 经度 %.6f]", 
                        userMessage, latitude, longitude);
            }
            messages.add(new UserMessage(enhancedMessage));

            // 构建Prompt并调用AI
            Prompt prompt = new Prompt(messages);

            // 创建Function Callbacks
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
            
            // 使用ChatClient进行Function Calling调用
            ChatResponse response = chatClient.prompt(prompt)
                    .functions(findNearbyCallback, searchCallback, recommendCallback)
                    .call()
                    .chatResponse();

            String result = response.getResult().getOutput().getContent();
            log.info("AI回复: {}", result);
            return result;

        } catch (Exception e) {
            log.error("AI对话发生错误", e);
            return "抱歉，我暂时无法回答您的问题，请稍后重试。😔";
        }
    }

    /**
     * 发送消息并获取流式AI回复（用于打字机效果）
     *
     * @param userMessage 用户消息
     * @param latitude    用户纬度（可选）
     * @param longitude   用户经度（可选）
     * @return 流式AI回复
     */
    public Flux<String> chatStream(String userMessage, Double latitude, Double longitude) {
        log.info("用户消息(流式): {}, 位置: ({}, {})", userMessage, latitude, longitude);

        try {
            // 构建消息列表
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(SYSTEM_PROMPT));
            
            // 如果有位置信息，添加到上下文
            String enhancedMessage = userMessage;
            if (latitude != null && longitude != null) {
                enhancedMessage = String.format("%s\n[用户当前位置：纬度 %.6f, 经度 %.6f]", 
                        userMessage, latitude, longitude);
            }
            messages.add(new UserMessage(enhancedMessage));

            // 构建Prompt
            Prompt prompt = new Prompt(messages);
            
            // 创建Function Callbacks
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

            // 使用ChatClient进行流式调用
            return chatClient.prompt(prompt)
                    .functions(findNearbyCallback, searchCallback, recommendCallback)
                    .stream()
                    .content();

        } catch (Exception e) {
            log.error("AI流式对话发生错误", e);
            return Flux.just("抱歉，我暂时无法回答您的问题，请稍后重试。😔");
        }
    }

    /**
     * 简单的聊天（不带位置信息）
     *
     * @param userMessage 用户消息
     * @return AI回复
     */
    public String chat(String userMessage) {
        return chat(userMessage, null, null);
    }
}
