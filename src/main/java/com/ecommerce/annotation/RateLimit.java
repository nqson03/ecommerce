package com.ecommerce.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation để áp dụng giới hạn tốc độ cho các endpoint API
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * Số lượng yêu cầu tối đa trong khoảng thời gian làm mới
     */
    int limit() default -1;
    
    /**
     * Thời gian làm mới tính bằng giây
     */
    int refreshPeriod() default -1;
    
    /**
     * Giới hạn cho người dùng chưa xác thực
     */
    int anonymousLimit() default -1;
    
    /**
     * Giới hạn cho người dùng đã xác thực
     */
    int authenticatedLimit() default -1;
    
    /**
     * Tên endpoint để sử dụng cấu hình từ hệ thống
     */
    String endpoint() default "";
    
    /**
     * Biểu thức SpEL để xác định khóa tùy chỉnh
     */
    String key() default "";
}
