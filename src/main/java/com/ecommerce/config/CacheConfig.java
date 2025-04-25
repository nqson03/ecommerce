package com.ecommerce.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JavaType;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.data.domain.PageImpl;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.beans.factory.annotation.Qualifier;




import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Lớp cấu hình Redis và Cache cho ứng dụng
 * Định nghĩa các tên cache, thời gian sống (TTL) và cấu hình kết nối Redis
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Tên các cache
    public static final String PRODUCTS_CACHE = "products";
    public static final String PRODUCT_CACHE = "product";
    public static final String CATEGORY_PRODUCTS_CACHE = "categoryProducts";
    public static final String SEARCH_PRODUCTS_CACHE = "searchProducts";
    public static final String REVIEWS_CACHE = "reviews";
    public static final String CART_CACHE = "cart";
    public static final String JWT_BLACKLIST_CACHE = "jwtBlacklist";
    public static final String CATEGORIES_CACHE = "categories";
    public static final String CATEGORY_CACHE = "category";
    public static final String USER_CACHE = "user";
    public static final String USERS_CACHE = "users";
    public static final String ORDERS_CACHE = "orders";
    public static final String ORDER_CACHE = "order";
    public static final String USER_ORDERS_CACHE = "userOrders";
    
    // Thời gian sống của cache (giây)
    public static final long PRODUCTS_CACHE_TTL = 3600; // 1 giờ
    public static final long PRODUCT_CACHE_TTL = 3600; // 1 giờ
    public static final long CATEGORY_PRODUCTS_CACHE_TTL = 3600; // 1 giờ
    public static final long SEARCH_PRODUCTS_CACHE_TTL = 1800; // 30 phút
    public static final long REVIEWS_CACHE_TTL = 1800; // 30 phút
    public static final long CART_CACHE_TTL = 600; // 10 phút
    public static final long JWT_BLACKLIST_CACHE_TTL = 86400; // 24 giờ
    public static final long CATEGORIES_CACHE_TTL = 7200; // 2 giờ
    public static final long CATEGORY_CACHE_TTL = 3600; // 1 giờ
    public static final long USER_CACHE_TTL = 1800; // 30 phút
    public static final long USERS_CACHE_TTL = 1800; // 30 phút
    public static final long ORDERS_CACHE_TTL = 1800; // 30 phút
    public static final long ORDER_CACHE_TTL = 1800; // 30 phút
    public static final long USER_ORDERS_CACHE_TTL = 1800; // 30 phút

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    /**
     * Custom deserializer for PageImpl to handle deserialization properly
     */
    public static class PageImplDeserializer extends JsonDeserializer<PageImpl<?>> {
        @Override
        public PageImpl<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            
            // Extract content
            JsonNode contentNode = node.get("content");
            JavaType contentType = ctxt.getTypeFactory().constructCollectionType(List.class, Object.class);
            List<?> content = ctxt.readTreeAsValue(contentNode, contentType);
            
            // Extract pagination information
            int number = node.has("number") ? node.get("number").asInt() : 0;
            int size = node.has("size") ? node.get("size").asInt() : content.size();
            long totalElements = node.has("totalElements") ? node.get("totalElements").asLong() : content.size();
            
            // Create PageRequest
            PageRequest pageRequest = PageRequest.of(number, size > 0 ? size : 1);
            
            // Create PageImpl
            return new PageImpl<>(content, pageRequest, totalElements);
        }
    }

    /**
     * Cấu hình ObjectMapper để hỗ trợ Java 8 date/time types và thông tin kiểu
     */
    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);      

        // Thêm custom deserializer cho PageImpl
        SimpleModule pageModule = new SimpleModule();
        pageModule.addDeserializer(PageImpl.class, new PageImplDeserializer());
        objectMapper.registerModule(pageModule);

        // Thêm thông tin kiểu dữ liệu trong JSON để hỗ trợ deserialization chính xác
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), 
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        
        return objectMapper;
    }

    /**
     * Cấu hình GenericJackson2JsonRedisSerializer với ObjectMapper đã cấu hình
     */
    @Bean
    public GenericJackson2JsonRedisSerializer redisSerializer(@Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    /**
     * Cấu hình kết nối đến Redis server
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (!redisPassword.isEmpty()) {
            configuration.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(configuration);
    }

    /**
     * Cấu hình RedisTemplate để thao tác trực tiếp với Redis
     * Sử dụng cho các trường hợp cần tùy biến nhiều hơn so với Spring Cache
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, 
                                                      GenericJackson2JsonRedisSerializer redisSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(redisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(redisSerializer);
        template.afterPropertiesSet();
        
        return template;
    }

    /**
     * Cấu hình mặc định cho Redis cache
     */
    @Bean
    public RedisCacheConfiguration defaultCacheConfig(GenericJackson2JsonRedisSerializer redisSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(PRODUCTS_CACHE_TTL)) // Default TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .disableCachingNullValues();
    }

    /**
     * Cấu hình Cache Manager với các TTL khác nhau cho từng loại cache
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, RedisCacheConfiguration defaultCacheConfig) {
        // Tạo map cấu hình cho từng loại cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Cấu hình TTL cho từng loại cache
        cacheConfigurations.put(PRODUCTS_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(PRODUCTS_CACHE_TTL)));
                
        cacheConfigurations.put(PRODUCT_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(PRODUCT_CACHE_TTL)));
                
        cacheConfigurations.put(CATEGORY_PRODUCTS_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CATEGORY_PRODUCTS_CACHE_TTL)));
                
        cacheConfigurations.put(SEARCH_PRODUCTS_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(SEARCH_PRODUCTS_CACHE_TTL)));
                
        cacheConfigurations.put(REVIEWS_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(REVIEWS_CACHE_TTL)));
                
        cacheConfigurations.put(CART_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CART_CACHE_TTL)));
                
        cacheConfigurations.put(JWT_BLACKLIST_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(JWT_BLACKLIST_CACHE_TTL)));
                
        cacheConfigurations.put(CATEGORIES_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CATEGORIES_CACHE_TTL)));
                
        cacheConfigurations.put(CATEGORY_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CATEGORY_CACHE_TTL)));
                
        cacheConfigurations.put(USER_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(USER_CACHE_TTL)));
                
        cacheConfigurations.put(USERS_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(USERS_CACHE_TTL)));
                
        cacheConfigurations.put(ORDERS_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(ORDERS_CACHE_TTL)));
                
        cacheConfigurations.put(ORDER_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(ORDER_CACHE_TTL)));
                
        cacheConfigurations.put(USER_ORDERS_CACHE, 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(USER_ORDERS_CACHE_TTL)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}