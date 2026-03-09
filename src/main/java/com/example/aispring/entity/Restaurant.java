package com.example.aispring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 餐厅实体类
 * 存储餐厅的基本信息，包括位置坐标用于距离计算
 */
@Data
@Entity
@Table(name = "restaurant")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 餐厅名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 详细地址
     */
    @Column(nullable = false, length = 255)
    private String address;

    /**
     * 纬度 - 用于计算距离
     */
    @Column(nullable = false)
    private Double latitude;

    /**
     * 经度 - 用于计算距离
     */
    @Column(nullable = false)
    private Double longitude;

    /**
     * 餐厅类型/菜系，如：火锅、日料、快餐、川菜、粤菜等
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * 评分，范围 1.0 - 5.0
     */
    @Column(nullable = false)
    private Double rating;

    /**
     * 餐厅描述
     */
    @Column(length = 1000)
    private String description;

    /**
     * 计算与给定坐标之间的距离（使用Haversine公式，单位：公里）
     *
     * @param lat 目标纬度
     * @param lon 目标经度
     * @return 距离（公里）
     */
    public double calculateDistance(double lat, double lon) {
        final int R = 6371; // 地球半径（公里）

        double latDistance = Math.toRadians(lat - this.latitude);
        double lonDistance = Math.toRadians(lon - this.longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(lat))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
