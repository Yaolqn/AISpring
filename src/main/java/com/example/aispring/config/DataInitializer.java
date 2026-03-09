package com.example.aispring.config;

import com.example.aispring.entity.Restaurant;
import com.example.aispring.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 数据初始化程序
 * 应用启动时自动生成测试数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RestaurantRepository restaurantRepository;
    private final Random random = new Random();

    // 测试数据：餐厅名称
    private static final String[][] RESTAURANT_NAMES = {
            {"海底捞火锅", "火锅"},
            {"小龙坎老火锅", "火锅"},
            {"呷哺呷哺", "火锅"},
            {"蜀大侠火锅", "火锅"},
            {"巴奴毛肚火锅", "火锅"},
            {"寿司之神", "日料"},
            {"元气寿司", "日料"},
            {"一兰拉面", "日料"},
            {"吉野家", "日料"},
            {"争鲜回转寿司", "日料"},
            {"全聚德", "中餐"},
            {"大董烤鸭", "中餐"},
            {"西贝莜面村", "中餐"},
            {"外婆家", "中餐"},
            {"绿茶餐厅", "中餐"},
            {"麦当劳", "快餐"},
            {"肯德基", "快餐"},
            {"汉堡王", "快餐"},
            {"必胜客", "西餐"},
            {"星巴克", "咖啡"},
            {"喜茶", "饮品"},
            {"奈雪的茶", "饮品"},
            {"太二酸菜鱼", "川菜"},
            {"眉州东坡", "川菜"},
            {"陶陶居", "粤菜"},
            {"点都德", "粤菜"},
            {"首尔烤肉", "韩餐"},
            {"汉拿山", "韩餐"},
            {"萨莉亚", "西餐"},
            {"达美乐披萨", "西餐"}
    };

    // 测试数据：地址前缀（广州番禺）
    private static final String[] ADDRESS_PREFIXES = {
            "广州市番禺区", "广州市番禺区", "广州市番禺区", "广州市番禺区",
            "广州市番禺区", "广州市番禺区", "广州市番禺区", "广州市番禺区"
    };

    // 测试数据：街道（广州番禺）
    private static final String[] STREETS = {
            "市桥街", "大石街", "洛浦街", "南村镇", "钟村镇",
            "石壁街", "沙湾街", "东环街", "桥南街", "小谷围街"
    };

    // 测试数据：描述模板
    private static final String[][] DESCRIPTION_TEMPLATES = {
            {"火锅", "正宗%s，麻辣鲜香，食材新鲜，服务周到，是朋友聚会的好去处。"},
            {"日料", "精致%s，新鲜食材，匠心制作，环境优雅，适合商务宴请。"},
            {"中餐", "传统%s，口味地道，菜品丰富，性价比高，家庭聚餐首选。"},
            {"快餐", "快捷%s，出餐迅速，味道不错，价格实惠，适合上班族。"},
            {"西餐", "浪漫%s，环境优雅，菜品精致，适合情侣约会。"},
            {"咖啡", "舒适%s，咖啡香浓，甜点可口，是休闲办公的好地方。"},
            {"饮品", "时尚%s，颜值在线，口感清爽，年轻人的最爱。"},
            {"川菜", "地道%s，麻辣过瘾，菜品丰富，爱吃辣的朋友不要错过。"},
            {"粤菜", "正宗%s，清淡鲜美，做工精细，老少皆宜。"},
            {"韩餐", "特色%s，烤肉飘香，小菜丰富，体验韩国美食文化。"}
    };

    @Override
    public void run(String... args) {
        log.info("开始重新生成餐厅测试数据...");
        
        // 删除所有现有数据
        restaurantRepository.deleteAll();
        log.info("已清空现有餐厅数据");
        
        // 生成新数据
        List<Restaurant> restaurants = generateRestaurants(25);
        restaurantRepository.saveAll(restaurants);
        
        log.info("成功生成 {} 条新的餐厅数据", restaurants.size());
    }

    /**
     * 生成餐厅测试数据
     *
     * @param count 生成数量
     * @return 餐厅列表
     */
    private List<Restaurant> generateRestaurants(int count) {
        List<Restaurant> restaurants = new ArrayList<>();

        // 使用广州番禺中心区域作为基准坐标
        double baseLat = 23.0458;  // 番禺中心纬度
        double baseLng = 113.3826; // 番禺中心经度

        for (int i = 0; i < count && i < RESTAURANT_NAMES.length; i++) {
            String[] nameType = RESTAURANT_NAMES[i];
            String name = nameType[0];
            String type = nameType[1];

            // 扩大分布范围：±0.5度约±50公里，覆盖整个广州及周边区域
            double lat = baseLat + (random.nextDouble() - 0.5) * 1.0;
            double lng = baseLng + (random.nextDouble() - 0.5) * 1.0;

            // 生成地址
            String address = generateAddress();

            // 生成评分 3.5 - 5.0
            double rating = 3.5 + random.nextDouble() * 1.5;
            rating = Math.round(rating * 10) / 10.0; // 保留一位小数

            // 生成描述
            String description = generateDescription(type);

            Restaurant restaurant = Restaurant.builder()
                    .name(name)
                    .address(address)
                    .latitude(lat)
                    .longitude(lng)
                    .type(type)
                    .rating(rating)
                    .description(description)
                    .build();

            restaurants.add(restaurant);
        }

        return restaurants;
    }

    /**
     * 生成随机地址
     */
    private String generateAddress() {
        String prefix = ADDRESS_PREFIXES[random.nextInt(ADDRESS_PREFIXES.length)];
        String street = STREETS[random.nextInt(STREETS.length)];
        int number = random.nextInt(200) + 1;
        String building = random.nextBoolean() ? "号" + (random.nextInt(20) + 1) + "号楼" : "";
        String floor = random.nextBoolean() ? random.nextInt(5) + 1 + "层" : "";
        
        return String.format("%s%s%d%s%s", prefix, street, number, building, floor);
    }

    /**
     * 生成餐厅描述
     */
    private String generateDescription(String type) {
        for (String[] template : DESCRIPTION_TEMPLATES) {
            if (template[0].equals(type)) {
                return String.format(template[1], type);
            }
        }
        return String.format("特色%s，环境舒适，服务热情，欢迎品尝。", type);
    }
}
