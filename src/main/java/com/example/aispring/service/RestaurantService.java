package com.example.aispring.service;

import com.example.aispring.entity.Restaurant;
import com.example.aispring.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 餐厅服务层
 * 处理餐厅相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    // ==================== CRUD 操作 ====================

    /**
     * 创建新餐厅
     *
     * @param restaurant 餐厅信息
     * @return 保存后的餐厅
     */
    @Transactional
    public Restaurant createRestaurant(Restaurant restaurant) {
        return restaurantRepository.save(restaurant);
    }

    /**
     * 根据ID查询餐厅
     *
     * @param id 餐厅ID
     * @return 餐厅信息
     */
    @Transactional(readOnly = true)
    public Optional<Restaurant> getRestaurantById(Long id) {
        return restaurantRepository.findById(id);
    }

    /**
     * 获取所有餐厅
     *
     * @return 餐厅列表
     */
    @Transactional(readOnly = true)
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    /**
     * 更新餐厅信息
     *
     * @param id         餐厅ID
     * @param restaurant 更新的餐厅信息
     * @return 更新后的餐厅
     */
    @Transactional
    public Restaurant updateRestaurant(Long id, Restaurant restaurant) {
        Restaurant existingRestaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("餐厅不存在: " + id));

        existingRestaurant.setName(restaurant.getName());
        existingRestaurant.setAddress(restaurant.getAddress());
        existingRestaurant.setLatitude(restaurant.getLatitude());
        existingRestaurant.setLongitude(restaurant.getLongitude());
        existingRestaurant.setType(restaurant.getType());
        existingRestaurant.setRating(restaurant.getRating());
        existingRestaurant.setDescription(restaurant.getDescription());

        return restaurantRepository.save(existingRestaurant);
    }

    /**
     * 删除餐厅
     *
     * @param id 餐厅ID
     */
    @Transactional
    public void deleteRestaurant(Long id) {
        restaurantRepository.deleteById(id);
    }

    // ==================== 搜索和筛选 ====================

    /**
     * 按名称模糊搜索
     *
     * @param name 餐厅名称关键字
     * @return 匹配的餐厅列表
     */
    @Transactional(readOnly = true)
    public List<Restaurant> searchByName(String name) {
        return restaurantRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * 按类型筛选
     *
     * @param type 餐厅类型
     * @return 该类型的餐厅列表
     */
    @Transactional(readOnly = true)
    public List<Restaurant> searchByType(String type) {
        return restaurantRepository.findByTypeIgnoreCase(type);
    }

    /**
     * 按名称和类型同时筛选
     *
     * @param name 餐厅名称关键字
     * @param type 餐厅类型
     * @return 匹配的餐厅列表
     */
    @Transactional(readOnly = true)
    public List<Restaurant> searchByNameAndType(String name, String type) {
        return restaurantRepository.findByNameContainingIgnoreCaseAndTypeIgnoreCase(name, type);
    }

    // ==================== 推荐功能 ====================

    /**
     * 获取推荐餐厅列表（按评分降序）
     *
     * @param limit 返回数量限制（0表示不限制）
     * @return 推荐餐厅列表
     */
    @Transactional(readOnly = true)
    public List<Restaurant> getRecommendedRestaurants(int limit) {
        List<Restaurant> restaurants = restaurantRepository.findAllByOrderByRatingDesc();
        if (limit > 0 && limit < restaurants.size()) {
            return restaurants.subList(0, limit);
        }
        return restaurants;
    }

    /**
     * 获取指定类型的推荐餐厅（按评分降序）
     *
     * @param type  餐厅类型
     * @param limit 返回数量限制
     * @return 推荐餐厅列表
     */
    @Transactional(readOnly = true)
    public List<Restaurant> getRecommendedRestaurantsByType(String type, int limit) {
        List<Restaurant> restaurants = restaurantRepository.findByTypeIgnoreCaseOrderByRatingDesc(type);
        if (limit > 0 && limit < restaurants.size()) {
            return restaurants.subList(0, limit);
        }
        return restaurants;
    }

    // ==================== 地理位置相关 ====================

    /**
     * 查找附近的餐厅
     *
     * @param latitude  用户纬度
     * @param longitude 用户经度
     * @param radiusKm  搜索半径（公里）
     * @return 附近餐厅列表
     */
    @Transactional(readOnly = true)
    public List<Restaurant> findNearbyRestaurants(double latitude, double longitude, double radiusKm) {
        return restaurantRepository.findNearbyRestaurants(latitude, longitude, radiusKm);
    }

    /**
     * 查找附近指定类型的餐厅
     *
     * @param latitude  用户纬度
     * @param longitude 用户经度
     * @param radiusKm  搜索半径（公里）
     * @param type      餐厅类型
     * @return 附近餐厅列表
     */
    @Transactional(readOnly = true)
    public List<Restaurant> findNearbyRestaurantsByType(double latitude, double longitude,
                                                         double radiusKm, String type) {
        return restaurantRepository.findNearbyRestaurantsByType(latitude, longitude, radiusKm, type);
    }

    /**
     * 查找附近指定类型的餐厅，按评分排序
     *
     * @param latitude  用户纬度
     * @param longitude 用户经度
     * @param radiusKm  搜索半径（公里）
     * @param type      餐厅类型
     * @return 附近餐厅列表（按评分降序）
     */
    @Transactional(readOnly = true)
    public List<Restaurant> findNearbyRestaurantsByTypeOrderByRating(double latitude, double longitude,
                                                                      double radiusKm, String type) {
        return restaurantRepository.findNearbyRestaurantsByTypeOrderByRating(
                latitude, longitude, radiusKm, type);
    }

    /**
     * 查找附近餐厅并计算距离（用于AI返回详细信息）
     *
     * @param latitude  用户纬度
     * @param longitude 用户经度
     * @param radiusKm  搜索半径（公里）
     * @param type      餐厅类型（可选）
     * @param sortBy    排序方式："distance"-距离, "rating"-评分
     * @param limit     返回数量限制
     * @return 带距离信息的餐厅列表
     */
    @Transactional(readOnly = true)
    public List<RestaurantWithDistance> findNearbyRestaurantsWithDistance(
            double latitude, double longitude, double radiusKm,
            String type, String sortBy, int limit) {

        List<Restaurant> restaurants;

        if (type != null && !type.isEmpty()) {
            if ("rating".equalsIgnoreCase(sortBy)) {
                restaurants = findNearbyRestaurantsByTypeOrderByRating(latitude, longitude, radiusKm, type);
            } else {
                restaurants = findNearbyRestaurantsByType(latitude, longitude, radiusKm, type);
            }
        } else {
            restaurants = findNearbyRestaurants(latitude, longitude, radiusKm);
            if ("rating".equalsIgnoreCase(sortBy)) {
                restaurants = restaurants.stream()
                        .sorted(Comparator.comparing(Restaurant::getRating).reversed())
                        .collect(Collectors.toList());
            }
        }

        // 计算距离并限制数量
        List<RestaurantWithDistance> result = restaurants.stream()
                .map(r -> new RestaurantWithDistance(r, r.calculateDistance(latitude, longitude)))
                .limit(limit > 0 ? limit : restaurants.size())
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 餐厅带距离信息的包装类
     */
    public record RestaurantWithDistance(Restaurant restaurant, double distanceKm) {
        @Override
        public String toString() {
            return String.format("%s (%s) - 评分: %.1f, 距离: %.2f公里, 地址: %s",
                    restaurant.getName(),
                    restaurant.getType(),
                    restaurant.getRating(),
                    distanceKm,
                    restaurant.getAddress());
        }
    }
}
