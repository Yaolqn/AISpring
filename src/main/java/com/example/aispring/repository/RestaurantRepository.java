package com.example.aispring.repository;

import com.example.aispring.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 餐厅数据访问层
 * 提供餐厅数据的CRUD操作和自定义查询
 */
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /**
     * 根据餐厅名称模糊搜索
     *
     * @param name 餐厅名称关键字
     * @return 匹配的餐厅列表
     */
    List<Restaurant> findByNameContainingIgnoreCase(String name);

    /**
     * 根据餐厅类型筛选
     *
     * @param type 餐厅类型
     * @return 该类型的餐厅列表
     */
    List<Restaurant> findByTypeIgnoreCase(String type);

    /**
     * 根据名称和类型同时筛选
     *
     * @param name 餐厅名称关键字
     * @param type 餐厅类型
     * @return 匹配的餐厅列表
     */
    List<Restaurant> findByNameContainingIgnoreCaseAndTypeIgnoreCase(String name, String type);

    /**
     * 按评分降序获取所有餐厅
     *
     * @return 按评分排序的餐厅列表
     */
    List<Restaurant> findAllByOrderByRatingDesc();

    /**
     * 按评分降序获取指定类型的餐厅
     *
     * @param type 餐厅类型
     * @return 按评分排序的餐厅列表
     */
    List<Restaurant> findByTypeIgnoreCaseOrderByRatingDesc(String type);

    /**
     * 使用Haversine公式查询指定范围内的餐厅，并按距离排序
     * 计算地球表面两点之间的距离
     *
     * @param latitude  中心点纬度
     * @param longitude 中心点经度
     * @param radiusKm  搜索半径（公里）
     * @return 指定范围内的餐厅列表
     */
    @Query(value = """
            SELECT * FROM (
                SELECT r.id, r.name, r.address, r.latitude, r.longitude, r.type, r.rating, r.description,
                       (6371 * ACOS(COS(RADIANS(:latitude)) * COS(RADIANS(r.latitude))
                       * COS(RADIANS(r.longitude) - RADIANS(:longitude))
                       + SIN(RADIANS(:latitude)) * SIN(RADIANS(r.latitude)))) AS distance
                FROM restaurant r
            ) AS subquery
            WHERE distance < :radiusKm
            ORDER BY distance ASC
            """, nativeQuery = true)
    List<Restaurant> findNearbyRestaurants(@Param("latitude") double latitude,
                                           @Param("longitude") double longitude,
                                           @Param("radiusKm") double radiusKm);

    /**
     * 查询指定类型且在指定范围内的餐厅，按距离排序
     *
     * @param latitude  中心点纬度
     * @param longitude 中心点经度
     * @param radiusKm  搜索半径（公里）
     * @param type      餐厅类型
     * @return 符合条件的餐厅列表
     */
    @Query(value = """
            SELECT * FROM (
                SELECT r.id, r.name, r.address, r.latitude, r.longitude, r.type, r.rating, r.description,
                       (6371 * ACOS(COS(RADIANS(:latitude)) * COS(RADIANS(r.latitude))
                       * COS(RADIANS(r.longitude) - RADIANS(:longitude))
                       + SIN(RADIANS(:latitude)) * SIN(RADIANS(r.latitude)))) AS distance
                FROM restaurant r
                WHERE LOWER(r.type) = LOWER(:type)
            ) AS subquery
            WHERE distance < :radiusKm
            ORDER BY distance ASC
            """, nativeQuery = true)
    List<Restaurant> findNearbyRestaurantsByType(@Param("latitude") double latitude,
                                                 @Param("longitude") double longitude,
                                                 @Param("radiusKm") double radiusKm,
                                                 @Param("type") String type);

    /**
     * 查询指定类型且在指定范围内的餐厅，按评分排序
     *
     * @param latitude  中心点纬度
     * @param longitude 中心点经度
     * @param radiusKm  搜索半径（公里）
     * @param type      餐厅类型
     * @return 符合条件的餐厅列表（按评分降序）
     */
    @Query(value = """
            SELECT * FROM (
                SELECT r.id, r.name, r.address, r.latitude, r.longitude, r.type, r.rating, r.description,
                       (6371 * ACOS(COS(RADIANS(:latitude)) * COS(RADIANS(r.latitude))
                       * COS(RADIANS(r.longitude) - RADIANS(:longitude))
                       + SIN(RADIANS(:latitude)) * SIN(RADIANS(r.latitude)))) AS distance
                FROM restaurant r
                WHERE LOWER(r.type) = LOWER(:type)
            ) AS subquery
            WHERE distance < :radiusKm
            ORDER BY rating DESC, distance ASC
            """, nativeQuery = true)
    List<Restaurant> findNearbyRestaurantsByTypeOrderByRating(@Param("latitude") double latitude,
                                                               @Param("longitude") double longitude,
                                                               @Param("radiusKm") double radiusKm,
                                                               @Param("type") String type);
}
