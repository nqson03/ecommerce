package com.ecommerce.config;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.beans.factory.BeanFactory;
import java.lang.reflect.Method;

@Aspect
@Component
public class RateLimitingAspect {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingAspect.class);

    private final RateLimitingConfig rateLimitingConfig;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    @Autowired
    private BeanFactory beanFactory;

    public RateLimitingAspect(RateLimitingConfig rateLimitingConfig) {
        this.rateLimitingConfig = rateLimitingConfig;
    }

    /**
     * Áp dụng giới hạn tốc độ cho tất cả các phương thức có annotation @RateLimit
     */
    @Around("@annotation(com.ecommerce.annotation.RateLimit)")
    public Object rateLimitWithAnnotation(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        
        // Lấy thông tin từ annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimitAnnotation = method.getAnnotation(RateLimit.class);
        
        // Lấy tên endpoint từ controller và method
        String endpointName = getEndpointName(joinPoint);
        
        // Tìm khóa định danh cho request - IP hoặc UserID hoặc khóa tùy chỉnh
        String key = resolveRateLimitKey(request, rateLimitAnnotation);
        
        // Xác định giá trị giới hạn và thời gian làm mới
        boolean allowed = checkRateLimit(endpointName, key, request, rateLimitAnnotation);
        
        if (!allowed) {
            log.warn("Rate limit exceeded for endpoint: {}, key: {}", endpointName, key);
            throw new RateLimitExceededException("Too much request. Try again!");
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * Áp dụng giới hạn tốc độ mặc định cho tất cả các API endpoint khác
     */
    @Around("(@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.RequestMapping)) && " +
            "!@annotation(com.ecommerce.annotation.RateLimit)")
    public Object rateLimitDefault(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ipAddress = getClientIpAddress(request);
        String endpointName = getEndpointName(joinPoint);
        
        // Sử dụng endpoint name để có được cấu hình chính xác
        if (!rateLimitingConfig.tryAcquireForEndpoint(ipAddress, endpointName)) {
            log.warn("Default rate limit exceeded for endpoint: {}, IP: {}", endpointName, ipAddress);
            throw new RateLimitExceededException("Too much request. Try again!");
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * Trích xuất tên endpoint từ signature của method
     */
    private String getEndpointName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName().toLowerCase();
        String methodName = signature.getName().toLowerCase();
        
        // Bỏ "Controller" nếu có trong tên class
        if (className.endsWith("controller")) {
            className = className.substring(0, className.length() - 10);
        }
        
        return className + "." + methodName;
    }
    
    /**
     * Kiểm tra giới hạn tốc độ dựa trên cấu hình từ annotation
     */
    private boolean checkRateLimit(String endpointName, String key, HttpServletRequest request, RateLimit rateLimitAnnotation) {
        // Kiểm tra nếu cần phân biệt giữa người dùng đã xác thực và chưa xác thực
        if (rateLimitAnnotation.anonymousLimit() > 0 || rateLimitAnnotation.authenticatedLimit() > 0) {
            boolean isAuthenticated = isUserAuthenticated();
            
            if (isAuthenticated && rateLimitAnnotation.authenticatedLimit() > 0) {
                return rateLimitingConfig.tryAcquire(
                    endpointName, 
                    key, 
                    rateLimitAnnotation.authenticatedLimit(), 
                    rateLimitAnnotation.refreshPeriod() > 0 ? rateLimitAnnotation.refreshPeriod() : 60
                );
            } else if (!isAuthenticated && rateLimitAnnotation.anonymousLimit() > 0) {
                return rateLimitingConfig.tryAcquire(
                    endpointName,
                    key, 
                    rateLimitAnnotation.anonymousLimit(), 
                    rateLimitAnnotation.refreshPeriod() > 0 ? rateLimitAnnotation.refreshPeriod() : 60
                );
            }
        }
        
        // Sử dụng cấu hình endpoint nếu được chỉ định
        if (!rateLimitAnnotation.endpoint().isEmpty()) {
            return rateLimitingConfig.tryAcquireForEndpoint(key, rateLimitAnnotation.endpoint());
        }
        
        // Sử dụng limit và refreshPeriod từ annotation nếu được chỉ định
        if (rateLimitAnnotation.limit() > 0) {
            int refreshPeriod = rateLimitAnnotation.refreshPeriod() > 0 ? rateLimitAnnotation.refreshPeriod() : 60;
            return rateLimitingConfig.tryAcquire(endpointName, key, rateLimitAnnotation.limit(), refreshPeriod);
        }
        
        // Sử dụng cấu hình mặc định cho endpoint
        return rateLimitingConfig.tryAcquireForEndpoint(key, endpointName);
    }
    
    /**
     * Xác định khóa cho việc giới hạn tốc độ
     */
    private String resolveRateLimitKey(HttpServletRequest request, RateLimit rateLimitAnnotation) {
        // Nếu có khóa tùy chỉnh được chỉ định
        if (!rateLimitAnnotation.key().isEmpty()) {
            try {
                StandardEvaluationContext context = new StandardEvaluationContext();
                context.setBeanResolver(new BeanFactoryResolver(beanFactory));
                context.setVariable("request", request);
                
                // Thêm thông tin xác thực nếu có
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null) {
                    context.setVariable("authentication", authentication);
                }
                
                Expression expression = expressionParser.parseExpression(rateLimitAnnotation.key());
                Object value = expression.getValue(context);
                if (value != null) {
                    return value.toString();
                }
            } catch (Exception e) {
                log.error("Lỗi xử lý khóa tùy chỉnh: {}", e.getMessage(), e);
            }
        }
        
        // Nếu người dùng đã đăng nhập, sử dụng ID người dùng
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !(authentication.getPrincipal() instanceof String)) {
            return "user:" + authentication.getName();
        }
        
        // Mặc định sử dụng địa chỉ IP
        return "ip:" + getClientIpAddress(request);
    }
    
    /**
     * Kiểm tra xem người dùng hiện tại đã xác thực chưa
     */
    private boolean isUserAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               !(authentication.getPrincipal() instanceof String);
    }

    /**
     * Lấy địa chỉ IP của client, hỗ trợ proxy
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
            return xForwardedForHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}