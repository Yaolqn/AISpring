package com.example.aispring.controller;

import com.example.aispring.entity.Restaurant;
import com.example.aispring.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 餐厅 RESTful API 控制器
 * 提供餐厅的CRUD操作和搜索功能
 */
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 允许前端跨域访问
public class RestaurantController {

    private final RestaurantService restaurantService;

    // ==================== CRUD 接口 ====================

    /**
     * 创建餐厅
     * POST /api/restaurants
     */
    @PostMapping
    public ResponseEntity<Restaurant> createRestaurant(@RequestBody Restaurant restaurant) {
        Restaurant saved = restaurantService.createRestaurant(restaurant);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * 获取所有餐厅
     * GET /api/restaurants
     */
    @GetMapping
    public ResponseEntity<List<Restaurant>> getAllRestaurants() {
        return ResponseEntity.ok(restaurantService.getAllRestaurants());
    }

    /**
     * 根据ID获取餐厅
     * GET /api/restaurants/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> getRestaurantById(@PathVariable Long id) {
        return restaurantService.getRestaurantById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新餐厅
     * PUT /api/restaurants/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Restaurant> updateRestaurant(
            @PathVariable Long id,
            @RequestBody Restaurant restaurant) {
        return ResponseEntity.ok(restaurantService.updateRestaurant(id, restaurant));
    }

    /**
     * 删除餐厅
     * DELETE /api/restaurants/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== 搜索接口 ====================

    /**
     * 按名称模糊搜索
     * GET /api/restaurants/search?name=xxx
     */
    @GetMapping("/search")
    public ResponseEntity<List<Restaurant>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(restaurantService.searchByName(name));
    }

    /**
     * 按类型筛选
     * GET /api/restaurants/filter?type=xxx
     */
    @GetMapping("/filter")
    public ResponseEntity<List<Restaurant>> filterByType(@RequestParam String type) {
        return ResponseEntity.ok(restaurantService.searchByType(type));
    }

    /**
     * 组合搜索（名称+类型）
     * GET /api/restaurants/search/combined?name=xxx&type=xxx
     */
    @GetMapping("/search/combined")
    public ResponseEntity<List<Restaurant>> searchCombined(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type) {
        
        if (name != null && type != null) {
            return ResponseEntity.ok(restaurantService.searchByNameAndType(name, type));
        } else if (name != null) {
            return ResponseEntity.ok(restaurantService.searchByName(name));
        } else if (type != null) {
            return ResponseEntity.ok(restaurantService.searchByType(type));
        } else {
            return ResponseEntity.ok(restaurantService.getAllRestaurants());
        }
    }

    // ==================== 推荐接口 ====================

    /**
     * 获取推荐餐厅列表（按评分降序）
     * GET /api/restaurants/recommended?limit=10
     */
    @GetMapping("/recommended")
    public ResponseEntity<List<Restaurant>> getRecommendedRestaurants(
            @RequestParam(required = false, defaultValue = "0") int limit) {
        return ResponseEntity.ok(restaurantService.getRecommendedRestaurants(limit));
    }

    /**
     * 获取指定类型的推荐餐厅
     * GET /api/restaurants/recommended/{type}?limit=10
     */
    @GetMapping("/recommended/{type}")
    public ResponseEntity<List<Restaurant>> getRecommendedByType(
            @PathVariable String type,
            @RequestParam(required = false, defaultValue = "0") int limit) {
        return ResponseEntity.ok(restaurantService.getRecommendedRestaurantsByType(type, limit));
    }

    // ==================== 地理位置接口 ====================

    /**
     * 查找附近餐厅
     * GET /api/restaurants/nearby?lat=xx&lng=xx&radius=5
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<Restaurant>> findNearbyRestaurants(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false, defaultValue = "5.0") double radius) {
        return ResponseEntity.ok(restaurantService.findNearbyRestaurants(lat, lng, radius));
    }

    /**
     * 查找附近指定类型的餐厅
     * GET /api/restaurants/nearby/{type}?lat=xx&lng=xx&radius=5
     */
    @GetMapping("/nearby/{type}")
    public ResponseEntity<List<Restaurant>> findNearbyByType(
            @PathVariable String type,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false, defaultValue = "5.0") double radius) {
        return ResponseEntity.ok(restaurantService.findNearbyRestaurantsByType(lat, lng, radius, type));
    }
}
