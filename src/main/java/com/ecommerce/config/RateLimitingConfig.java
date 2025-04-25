package com.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(RateLimitingConfig.EndpointRateLimitProperties.class)
public class RateLimitingConfig {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingConfig.class);

    @Value("${app.ratelimiting.enabled:true}")
    private boolean enabled;

    @Value("${app.ratelimiting.default-limit:100}")
    private int defaultLimit;

    @Value("${app.ratelimiting.default-refresh-period:60}")
    private int defaultRefreshPeriod;

    private final RedisTemplate<String, Object> redisTemplate;
    private final EndpointRateLimitProperties endpointProperties;

    public RateLimitingConfig(RedisTemplate<String, Object> redisTemplate, 
                             EndpointRateLimitProperties endpointProperties) {
        this.redisTemplate = redisTemplate;
        this.endpointProperties = endpointProperties;
    }
    
    @PostConstruct
    public void init() {
        log.info("Loaded rate limit configurations for {} endpoints", 
                endpointProperties.getSettings().size());
        
        // Log chi tiết từng cấu hình endpoint
        endpointProperties.getSettings().forEach((endpoint, setting) -> {
            log.info("Rate limit for {}: {} requests per {} seconds", 
                    endpoint, setting.getLimit(), setting.getRefreshPeriod());
        });
    }

    /**
     * Kiểm tra xem một yêu cầu có vượt quá giới hạn tốc độ không, sử dụng cấu hình mặc định
     * @param key Khóa định danh cho yêu cầu (thường là IP hoặc ID người dùng)
     * @return true nếu yêu cầu được chấp nhận, false nếu vượt quá giới hạn
     */
    public boolean tryAcquire(String key) {
        return tryAcquire("default", key, defaultLimit, defaultRefreshPeriod);
    }

    /**
     * Kiểm tra xem một yêu cầu có vượt quá giới hạn tốc độ không cho một endpoint cụ thể
     * @param key Khóa định danh cho yêu cầu
     * @param endpointName Tên của endpoint
     * @return true nếu yêu cầu được chấp nhận, false nếu vượt quá giới hạn
     */
    public boolean tryAcquireForEndpoint(String key, String endpointName) {
        RateLimitSetting setting = endpointProperties.getSettings().getOrDefault(endpointName, 
                new RateLimitSetting(defaultLimit, defaultRefreshPeriod));
        return tryAcquire(endpointName, key, setting.getLimit(), setting.getRefreshPeriod());
    }

    /**
     * Kiểm tra xem một yêu cầu có vượt quá giới hạn tốc độ không với limit và thời gian chỉ định
     * @param endpointName Tên endpoint (để phân biệt các khóa Redis)
     * @param key Khóa định danh cho yêu cầu
     * @param limit Số lượng request tối đa
     * @param refreshPeriodSeconds Thời gian làm mới tính bằng giây
     * @return true nếu yêu cầu được chấp nhận, false nếu vượt quá giới hạn
     */
    public boolean tryAcquire(String endpointName, String key, int limit, int refreshPeriodSeconds) {
        if (!enabled) {
            return true;
        }

        String redisKey = String.format("rate_limit:%s:%s", endpointName, key);
        Long currentCount = redisTemplate.opsForValue().increment(redisKey, 1);

        // Nếu là lần đầu tiên, thiết lập thời gian hết hạn
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(redisKey, refreshPeriodSeconds, TimeUnit.SECONDS);
        }

        return currentCount != null && currentCount <= limit;
    }

    // @Bean
    public RateLimitingAspect rateLimitingAspect() {
        return new RateLimitingAspect(this);
    }

    /**
     * Lớp cấu hình cho từng endpoint được định nghĩa trong application.properties
     */
    @ConfigurationProperties("app.ratelimiting.endpoints")
    public static class EndpointRateLimitProperties {
        private final Map<String, RateLimitSetting> settings = new ConcurrentHashMap<>();

        public Map<String, RateLimitSetting> getSettings() {
            return settings;
        }
    }

    /**
     * Lớp nội bộ để lưu cấu hình giới hạn tốc độ
     */
    public static class RateLimitSetting {
        private int limit;
        private int refreshPeriod;

        public RateLimitSetting() {
            // Constructor không tham số cho Spring binding
        }

        public RateLimitSetting(int limit, int refreshPeriod) {
            this.limit = limit;
            this.refreshPeriod = refreshPeriod;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getRefreshPeriod() {
            return refreshPeriod;
        }

        public void setRefreshPeriod(int refreshPeriod) {
            this.refreshPeriod = refreshPeriod;
        }
    }
}