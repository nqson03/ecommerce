package com.ecommerce.service.impl;

import com.ecommerce.model.Order;
import com.ecommerce.model.User;
import com.ecommerce.service.interfaces.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${app.email.from:noreply@ecommerce.com}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void sendOrderConfirmationEmail(User user, Order order) {
        if (!emailEnabled) {
            log.info("Email is disabled, skipping order confirmation email for order: {}", order.getOrderNumber());
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("order", order);
            context.setVariable("orderDate", order.getOrderDate().format(DATE_FORMATTER));
            context.setVariable("totalAmount", formatCurrency(order.getTotalAmount()));

            String htmlContent = templateEngine.process("email/order-confirmation", context);

            sendEmail(
                user.getEmail(),
                "Xác nhận đặt hàng thành công - Đơn hàng #" + order.getOrderNumber(),
                htmlContent
            );

            log.info("Order confirmation email sent successfully to: {} for order: {}", 
                    user.getEmail(), order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to send order confirmation email to: {} for order: {}", 
                    user.getEmail(), order.getOrderNumber(), e);
        }
    }

    @Override
    public void sendOrderFailureEmail(User user, Order order, String reason) {
        if (!emailEnabled) {
            log.info("Email is disabled, skipping order failure email for order: {}", order.getOrderNumber());
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("order", order);
            context.setVariable("reason", reason);
            context.setVariable("orderDate", order.getOrderDate().format(DATE_FORMATTER));

            String htmlContent = templateEngine.process("email/order-failure", context);

            sendEmail(
                user.getEmail(),
                "Đặt hàng không thành công - Đơn hàng #" + order.getOrderNumber(),
                htmlContent
            );

            log.info("Order failure email sent successfully to: {} for order: {}", 
                    user.getEmail(), order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to send order failure email to: {} for order: {}", 
                    user.getEmail(), order.getOrderNumber(), e);
        }
    }

    @Override
    public void sendOrderStatusUpdateEmail(User user, Order order) {
        if (!emailEnabled) {
            log.info("Email is disabled, skipping order status update email for order: {}", order.getOrderNumber());
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("order", order);
            context.setVariable("statusText", getStatusText(order.getStatus()));
            context.setVariable("orderDate", order.getOrderDate().format(DATE_FORMATTER));
            
            if (order.getDeliveryDate() != null) {
                context.setVariable("deliveryDate", order.getDeliveryDate().format(DATE_FORMATTER));
            }

            String htmlContent = templateEngine.process("email/order-status-update", context);

            sendEmail(
                user.getEmail(),
                "Cập nhật trạng thái đơn hàng #" + order.getOrderNumber(),
                htmlContent
            );

            log.info("Order status update email sent successfully to: {} for order: {}", 
                    user.getEmail(), order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to send order status update email to: {} for order: {}", 
                    user.getEmail(), order.getOrderNumber(), e);
        }
    }

    @Override
    public void sendOrderCancellationEmail(User user, Order order) {
        if (!emailEnabled) {
            log.info("Email is disabled, skipping order cancellation email for order: {}", order.getOrderNumber());
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("order", order);
            context.setVariable("orderDate", order.getOrderDate().format(DATE_FORMATTER));
            context.setVariable("totalAmount", formatCurrency(order.getTotalAmount()));

            String htmlContent = templateEngine.process("email/order-cancellation", context);

            sendEmail(
                user.getEmail(),
                "Hủy đơn hàng #" + order.getOrderNumber(),
                htmlContent
            );

            log.info("Order cancellation email sent successfully to: {} for order: {}", 
                    user.getEmail(), order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to send order cancellation email to: {} for order: {}", 
                    user.getEmail(), order.getOrderNumber(), e);
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private String getStatusText(Order.OrderStatus status) {
        return switch (status) {
            case PENDING -> "Đang chờ xử lý";
            case PROCESSING -> "Đang xử lý";
            case SHIPPED -> "Đang giao hàng";
            case DELIVERED -> "Đã giao hàng";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f VNĐ", amount);
    }
} 