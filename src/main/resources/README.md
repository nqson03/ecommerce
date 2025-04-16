# Cấu hình ứng dụng

## Tệp cấu hình application.properties

Tệp `application.properties` chứa các cấu hình quan trọng của ứng dụng, bao gồm thông tin kết nối cơ sở dữ liệu, cấu hình JWT, và thông tin thanh toán VNPay. Vì tệp này chứa thông tin nhạy cảm, nó đã được thêm vào `.gitignore` để không bị đẩy lên repository.

## Cách cấu hình ứng dụng

1. Sao chép tệp `application-example.properties` thành `application.properties`
2. Cập nhật các thông tin sau trong tệp `application.properties`:
   - Thông tin kết nối cơ sở dữ liệu (username, password)
   - Khóa bí mật JWT
   - Thông tin cấu hình VNPay (tmnCode, hashSecret)
   - Các URL liên quan đến thanh toán

## Các cấu hình quan trọng

### Cấu hình cơ sở dữ liệu
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Cấu hình JWT
```properties
app.jwt.secret=your_jwt_secret_key
app.jwt.expiration=86400000
```

### Cấu hình VNPay
```properties
vnpay.version=2.1.0
vnpay.tmnCode=your_tmn_code
vnpay.hashSecret=your_hash_secret
vnpay.payUrl=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.returnUrl=http://localhost:8080/api/payment/vnpay-return
```

### Cấu hình URL frontend
```properties
app.frontend.payment-result-url=http://localhost:3000/payment-result
```