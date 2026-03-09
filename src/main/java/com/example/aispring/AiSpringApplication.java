package com.example.aispring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 餐厅探索与推荐系统 - 主入口
 * 
 * 功能特性：
 * 1. 餐厅CRUD管理
 * 2. 基于地理位置的餐厅搜索
 * 3. AI智能推荐（支持Function Calling）
 * 4. 前后端分离架构
 */
@SpringBootApplication
public class AiSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSpringApplication.class, args);
        System.out.println("=================================");
        System.out.println("🍽️ 餐厅探索与推荐系统已启动！");
        System.out.println("=================================");
        System.out.println("📱 前端页面：http://localhost:8080");
        System.out.println("📚 API文档：http://localhost:8080/api/restaurants");
        System.out.println("=================================");
    }

}
