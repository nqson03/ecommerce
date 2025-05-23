package com.ecommerce.service.impl;

import com.ecommerce.dto.PaymentResult;
import com.ecommerce.exception.PaymentException;
import com.ecommerce.exception.UnauthorizedOperationException;
import com.ecommerce.model.Order;
import com.ecommerce.service.interfaces.OrderService;
import com.ecommerce.service.interfaces.PaymentService;
import com.ecommerce.service.interfaces.VNPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private OrderService orderService;

    public String createPaymentUrl(Long orderId, String username, String ipAddress) {
        // Validate order ownership and status
        Order order = validateOrderForPayment(orderId, username);
        
        // Create payment URL
        String paymentUrl = vnPayService.createPaymentUrl(order, ipAddress);
        
        logger.info("Created payment URL for order {} by user {}", orderId, username);
        return paymentUrl;
    }

    public PaymentResult processReturnCallback(Map<String, String> vnpParams) {
        // Validate signature
        if (!vnPayService.validateCallback(vnpParams)) {
            logger.error("Invalid signature from VNPay");
            throw new PaymentException("Invalid signature");
        }

        String orderNumber = vnpParams.get("vnp_TxnRef");
        PaymentResult result = vnPayService.getPaymentResult(vnpParams);

        // CHỈ LOG KẾT QUẢ - KHÔNG CẬP NHẬT TRẠNG THÁI VÀ STOCK
        if (result.isSuccess()) {
            logger.info("Payment successful (Return URL) for order: {} with transaction: {}", 
                orderNumber, result.getTransactionId());
            // DEMO: CẬP NHẬT TRẠNG THÁI VÀ TRỪ KHO VÌ IPN KHÔNG ĐƯỢC GỌI DO DỰ ÁN CHỈ LÀ DEMO
            orderService.updateOrderStatus(null, orderNumber, Order.OrderStatus.PROCESSING);
        } else {
            logger.warn("Payment failed (Return URL) for order: {} with code: {} - {}", 
                orderNumber, result.getResponseCode(), result.getMessage());
        }

        return result;
    }

    public PaymentService.IPNResponse processIPNCallback(Map<String, String> vnpParams) {
        logger.info("Processing IPN for transaction: {}", vnpParams.get("vnp_TxnRef"));
        
        // Validate signature
        if (!vnPayService.validateCallback(vnpParams)) {
            logger.error("Invalid signature in IPN");
            return new PaymentService.IPNResponse("97", "Invalid signature");
        }

        String orderNumber = vnpParams.get("vnp_TxnRef");
        String vnpAmount = vnpParams.get("vnp_Amount");
        String vnpResponseCode = vnpParams.get("vnp_ResponseCode");

        logger.info("Processing IPN for order: {} with response code: {}", orderNumber, vnpResponseCode);

        // Validate order exists
        Order order = orderService.findByOrderNumber(orderNumber).orElse(null);
        if (order == null) {
            logger.error("Order not found: {}", orderNumber);
            return new PaymentService.IPNResponse("01", "Order not found");
        }

        // Validate amount
        String expectedAmount = order.getTotalAmount()
            .multiply(new java.math.BigDecimal(100))
            .toBigInteger().toString();
        
        if (!expectedAmount.equals(vnpAmount)) {
            logger.error("Amount mismatch for order {}: expected={}, received={}", orderNumber, expectedAmount, vnpAmount);
            return new PaymentService.IPNResponse("04", "Invalid amount");
        }

        // Validate order status (IDEMPOTENCY CHECK)
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            logger.warn("Order {} already processed with status: {}", orderNumber, order.getStatus());
            return new PaymentService.IPNResponse("02", "Order already confirmed");
        }

        // CHỈ Ở ĐÂY MỚI THỰC SỰ CẬP NHẬT TRẠNG THÁI VÀ TRỪ KHO
        if ("00".equals(vnpResponseCode)) {
            logger.info("Payment confirmed via IPN for order: {} - UPDATING STATUS AND STOCK", orderNumber);
            orderService.updateOrderStatus(null, orderNumber, Order.OrderStatus.PROCESSING);
            return new PaymentService.IPNResponse("00", "Success");
        } else {
            logger.warn("Payment failed via IPN for order: {} with code: {}", orderNumber, vnpResponseCode);
            return new PaymentService.IPNResponse("00", "Payment failed but acknowledged");
        }
    }

    private Order validateOrderForPayment(Long orderId, String username) {
        Order order = orderService.getOrderEntityById(orderId);

        // Check ownership
        if (!order.getUser().getUsername().equals(username)) {
            logger.warn("User {} attempted to pay for order {} owned by {}", 
                username, orderId, order.getUser().getUsername());
            throw new UnauthorizedOperationException("You are not authorized to make payment for this order");
        }

        // Check payment method and status
        if (!order.getPaymentMethod().equals("VNPAY") || 
            order.getStatus() != Order.OrderStatus.PENDING) {
            logger.warn("Invalid payment attempt for order {}: method={}, status={}", 
                orderId, order.getPaymentMethod(), order.getStatus());
            throw new PaymentException("Invalid order or payment has already been made");
        }

        return order;
    }
} 