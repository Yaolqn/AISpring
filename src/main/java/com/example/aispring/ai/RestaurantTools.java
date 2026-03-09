package com.example.aispring.ai;

import com.example.aispring.entity.Restaurant;
import com.example.aispring.service.RestaurantService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI Function Calling 工具类
 * 定义餐厅搜索相关的工具函数，供AI调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantTools {

    private final RestaurantService restaurantService;

    /**
     * 搜索附近餐厅的工具函数
     * AI识别到用户想找附近餐厅时会调用此函数
     *
     * @param request 搜索请求参数
     * @return 餐厅搜索结果
     */
    public String findNearbyRestaurants(FindNearbyRequest request) {
        
        log.info("AI调用 findNearbyRestaurants: latitude={}, longitude={}, radiusKm={}, type={}, sortBy={}, limit={}",
                request.latitude(), request.longitude(), request.radiusKm(), 
                request.type(), request.sortBy(), request.limit());

        try {
            // 参数校验和默认值
            double radius = request.radiusKm() != null ? request.radiusKm() : 5.0;
            int limit = request.limit() != null ? request.limit() : 10;
            String sortBy = request.sortBy() != null ? request.sortBy() : "distance";
            String type = request.type();

            // 调用服务层查询
            List<RestaurantService.RestaurantWithDistance> results = 
                    restaurantService.findNearbyRestaurantsWithDistance(
                            request.latitude(), request.longitude(),
                            radius, type, sortBy, limit);

            if (results.isEmpty()) {
                return String.format("在距离您%.1f公里范围内未找到%s餐厅。",
                        radius, type != null ? "「" + type + "」类" : "");
            }

            // 格式化结果
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("为您找到%d家%s餐厅（搜索半径：%.1f公里，按%s排序）：\n\n",
                    results.size(), 
                    type != null ? "「" + type + "」类" : "",
                    radius,
                    "rating".equalsIgnoreCase(sortBy) ? "评分" : "距离"));

            for (int i = 0; i < results.size(); i++) {
                RestaurantService.RestaurantWithDistance rwd = results.get(i);
                Restaurant r = rwd.restaurant();
                sb.append(String.format("%d. %s (%s)\n", i + 1, r.getName(), r.getType()));
                sb.append(String.format("   评分：%.1f/5.0 ⭐\n", r.getRating()));
                sb.append(String.format("   距离：%.2f公里\n", rwd.distanceKm()));
                sb.append(String.format("   地址：%s\n", r.getAddress()));
                if (r.getDescription() != null && !r.getDescription().isEmpty()) {
                    sb.append(String.format("   简介：%s\n", r.getDescription()));
                }
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("搜索餐厅时发生错误", e);
            return "搜索餐厅时发生错误，请稍后重试。";
        }
    }

    /**
     * 搜索餐厅（不限制距离）
     * 用于按名称或类型搜索
     */
    public String searchRestaurants(SearchRequest request) {
        
        log.info("AI调用 searchRestaurants: name={}, type={}, sortBy={}, limit={}",
                request.name(), request.type(), request.sortBy(), request.limit());

        try {
            int limit = request.limit() != null ? request.limit() : 10;
            String sortBy = request.sortBy() != null ? request.sortBy() : "rating";

            List<Restaurant> results;

            // 根据参数决定查询方式
            if (request.name() != null && !request.name().isEmpty() && 
                request.type() != null && !request.type().isEmpty()) {
                results = restaurantService.searchByNameAndType(request.name(), request.type());
            } else if (request.name() != null && !request.name().isEmpty()) {
                results = restaurantService.searchByName(request.name());
            } else if (request.type() != null && !request.type().isEmpty()) {
                results = restaurantService.searchByType(request.type());
            } else {
                results = restaurantService.getAllRestaurants();
            }

            // 排序
            if ("rating".equalsIgnoreCase(sortBy)) {
                results = results.stream()
                        .sorted((r1, r2) -> Double.compare(r2.getRating(), r1.getRating()))
                        .collect(Collectors.toList());
            }

            // 限制数量
            if (limit > 0 && limit < results.size()) {
                results = results.subList(0, limit);
            }

            if (results.isEmpty()) {
                String searchDesc = "";
                if (request.name() != null && !request.name().isEmpty()) {
                    searchDesc += "名称包含「" + request.name() + "」";
                }
                if (request.type() != null && !request.type().isEmpty()) {
                    searchDesc += (searchDesc.isEmpty() ? "" : "且") + "类型为「" + request.type() + "」";
                }
                return "未找到" + (searchDesc.isEmpty() ? "" : searchDesc) + "的餐厅。";
            }

            // 格式化结果
            StringBuilder sb = new StringBuilder();
            String searchDesc = "";
            if (request.name() != null && !request.name().isEmpty()) {
                searchDesc += "名称包含「" + request.name() + "」";
            }
            if (request.type() != null && !request.type().isEmpty()) {
                searchDesc += (searchDesc.isEmpty() ? "" : "且") + "类型为「" + request.type() + "」";
            }
            
            sb.append(String.format("为您找到%d家%s餐厅（按%s排序）：\n\n",
                    results.size(),
                    searchDesc.isEmpty() ? "" : searchDesc,
                    "rating".equalsIgnoreCase(sortBy) ? "评分" : "默认"));

            for (int i = 0; i < results.size(); i++) {
                Restaurant r = results.get(i);
                sb.append(String.format("%d. %s (%s)\n", i + 1, r.getName(), r.getType()));
                sb.append(String.format("   评分：%.1f/5.0 ⭐\n", r.getRating()));
                sb.append(String.format("   地址：%s\n", r.getAddress()));
                if (r.getDescription() != null && !r.getDescription().isEmpty()) {
                    sb.append(String.format("   简介：%s\n", r.getDescription()));
                }
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("搜索餐厅时发生错误", e);
            return "搜索餐厅时发生错误，请稍后重试。";
        }
    }

    /**
     * 获取推荐餐厅
     */
    public String getRecommendedRestaurants(RecommendRequest request) {
        
        log.info("AI调用 getRecommendedRestaurants: type={}, limit={}", request.type(), request.limit());

        try {
            int limit = request.limit() != null ? request.limit() : 5;

            List<Restaurant> results;
            if (request.type() != null && !request.type().isEmpty()) {
                results = restaurantService.getRecommendedRestaurantsByType(request.type(), limit);
            } else {
                results = restaurantService.getRecommendedRestaurants(limit);
            }

            if (results.isEmpty()) {
                return request.type() != null ? 
                        "暂无「" + request.type() + "」类餐厅的推荐。" : 
                        "暂无餐厅推荐。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("为您推荐%d家%s高评分餐厅：\n\n",
                    results.size(),
                    request.type() != null ? "「" + request.type() + "」类" : ""));

            for (int i = 0; i < results.size(); i++) {
                Restaurant r = results.get(i);
                sb.append(String.format("%d. %s (%s)\n", i + 1, r.getName(), r.getType()));
                sb.append(String.format("   评分：%.1f/5.0 ⭐⭐⭐⭐⭐\n", r.getRating()));
                sb.append(String.format("   地址：%s\n", r.getAddress()));
                if (r.getDescription() != null && !r.getDescription().isEmpty()) {
                    sb.append(String.format("   简介：%s\n", r.getDescription()));
                }
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("获取推荐时发生错误", e);
            return "获取推荐时发生错误，请稍后重试。";
        }
    }

    // ==================== 请求参数类 ====================

    /**
     * 附近餐厅搜索请求
     */
    @JsonClassDescription("搜索附近餐厅的请求参数")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FindNearbyRequest(
            @JsonProperty(required = true)
            @JsonPropertyDescription("用户当前位置的纬度，例如：39.9042（北京天安门）")
            Double latitude,

            @JsonProperty(required = true)
            @JsonPropertyDescription("用户当前位置的经度，例如：116.4074（北京天安门）")
            Double longitude,

            @JsonPropertyDescription("搜索半径（公里），默认5公里")
            Double radiusKm,

            @JsonPropertyDescription("餐厅类型筛选，如：火锅、日料、川菜、粤菜、快餐等")
            String type,

            @JsonPropertyDescription("排序方式：distance(距离最近)、rating(评分最高)，默认distance")
            String sortBy,

            @JsonPropertyDescription("返回结果数量限制，默认10条")
            Integer limit
    ) {}

    /**
     * 餐厅搜索请求
     */
    @JsonClassDescription("搜索餐厅的请求参数")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SearchRequest(
            @JsonPropertyDescription("餐厅名称关键字，支持模糊搜索")
            String name,

            @JsonPropertyDescription("餐厅类型，如：火锅、日料、川菜、粤菜、快餐等")
            String type,

            @JsonPropertyDescription("排序方式：rating(评分最高)，默认rating")
            String sortBy,

            @JsonPropertyDescription("返回结果数量限制，默认10条")
            Integer limit
    ) {}

    /**
     * 推荐请求
     */
    @JsonClassDescription("获取推荐餐厅的请求参数")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RecommendRequest(
            @JsonPropertyDescription("餐厅类型筛选，可选")
            String type,

            @JsonPropertyDescription("返回推荐数量，默认5条")
            Integer limit
    ) {}
}
