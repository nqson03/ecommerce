# E-commerce Platform 

A Spring Boot-based e-commerce platform, providing features for product listing, shopping cart management, order processing, and user reviews.

## Features

- User authentication and authorization
- Product catalog with categories and search
- Shopping cart functionality
- Order management
- Product reviews and ratings
- File upload for product images and user profiles

## Technologies

- Java 17
- Spring Boot 3.1.5
- Spring Security with JWT
- Spring Data JPA
- MySQL Database
- Redis Cache
- Maven

## Getting Started

### Prerequisites

- JDK 17 or later
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+ (for caching and rate limiting)

### Database Setup

1. Create a MySQL database named `ecommerce`
2. Update the database configuration in `application.properties` if needed

### Application Configuration

1. Copy `application-example.properties` to `application.properties`
2. Update the following information in `application.properties`:
   - Database connection details (username, password)
   - JWT secret key
   - VNPay configuration (tmnCode, hashSecret)
   - Payment-related URLs
   - Redis configuration (if needed)

### Important Configurations

#### Database Configuration
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=your_username
spring.datasource.password=your_password
```

#### JWT Configuration
```properties
app.jwt.secret=your_jwt_secret_key
app.jwt.expiration=86400000
```

#### VNPay Configuration
```properties
vnpay.version=2.1.0
vnpay.tmnCode=your_tmn_code
vnpay.hashSecret=your_hash_secret
vnpay.payUrl=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.returnUrl=http://localhost:8080/api/payment/vnpay-return
```

#### Redis and Caching Configuration
```properties
spring.redis.host=localhost
spring.redis.port=6379
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
```

### Running the Application

```bash
mvn spring-boot:run
```