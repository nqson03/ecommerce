package com.ecommerce.service;

import com.ecommerce.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    @Value("${vnpay.version:2.1.0}")
    private String vnpVersion;

    @Value("${vnpay.tmnCode:}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret:}")
    private String vnpHashSecret;

    @Value("${vnpay.payUrl:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${vnpay.returnUrl:http://localhost:8080/api/payment/vnpay-return}")
    private String vnpReturnUrl;

    /**
     * Tạo URL thanh toán VNPay
     * @param order Đơn hàng cần thanh toán
     * @param ipAddress Địa chỉ IP của người dùng
     * @return URL thanh toán VNPay
     */
    public String createPaymentUrl(Order order, String ipAddress) {
        String vnpTxnRef = order.getOrderNumber();
        String vnpOrderInfo = "Thanh toan don hang " + vnpTxnRef;
        String vnpOrderType = "billpayment";
        String vnpAmount = String.valueOf(order.getTotalAmount().multiply(new java.math.BigDecimal(100)).intValue());
        
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Amount", vnpAmount);
        vnpParams.put("vnp_CurrCode", "VND");
        
        // Có thể thanh toán bằng ATM, IB, VISA...
        vnpParams.put("vnp_BankCode", "NCB");
        
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", vnpOrderInfo);
        vnpParams.put("vnp_OrderType", vnpOrderType);
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", ipAddress);
        
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        
        calendar.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);
        
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnpSecureHash = hmacSHA512(vnpHashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        
        return vnpPayUrl + "?" + queryUrl;
    }
    
    /**
     * Xác thực callback từ VNPay
     * @param vnpParams Các tham số từ VNPay gửi về
     * @return true nếu xác thực thành công, false nếu thất bại
     */
    public boolean validateCallback(Map<String, String> vnpParams) {
        String vnpSecureHash = vnpParams.get("vnp_SecureHash");
        if (vnpSecureHash == null) {
            return false;
        }
        
        // Xóa các tham số không cần thiết
        vnpParams.remove("vnp_SecureHashType");
        vnpParams.remove("vnp_SecureHash");
        
        // Sắp xếp các tham số theo thứ tự
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        
        // Tạo chuỗi hash
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        
        // Tính toán secure hash
        String calculatedHash = hmacSHA512(vnpHashSecret, hashData.toString());
        
        // So sánh secure hash
        return calculatedHash.equals(vnpSecureHash);
    }
    
    /**
     * Kiểm tra trạng thái thanh toán từ VNPay
     * @param vnpParams Các tham số từ VNPay gửi về
     * @return true nếu thanh toán thành công, false nếu thất bại
     */
    public boolean isPaymentSuccess(Map<String, String> vnpParams) {
        String vnpResponseCode = vnpParams.get("vnp_ResponseCode");
        return "00".equals(vnpResponseCode);
    }
    
    /**
     * Tạo mã HMAC SHA512
     * @param key Khóa bí mật
     * @param data Dữ liệu cần mã hóa
     * @return Chuỗi mã hóa
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512_HMAC.init(secret_key);
            byte[] hash = sha512_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * Chuyển đổi mảng byte thành chuỗi hex
     * @param bytes Mảng byte cần chuyển đổi
     * @return Chuỗi hex
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}