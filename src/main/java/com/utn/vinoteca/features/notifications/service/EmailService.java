package com.utn.vinoteca.features.notifications.service;

import com.utn.vinoteca.features.offer.dto.OfferResponseDTO;
import com.utn.vinoteca.features.offer_product.dto.OfferProductResponseDTO;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.orderItem.dto.OrderItemResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@vinoteca.com}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(String to, String name) {
        try {
            String body = "Hi " + name + ",\n\n"
                    + "Welcome to Vinoteca UTN!\n"
                    + "Your account has been created successfully.\n\n"
                    + "Browse our catalog: " + baseUrl + "/wines/search\n\n"
                    + "Thanks,\nVinoteca UTN Team";

            sendSimpleEmail(to, "Welcome to Vinoteca UTN", body);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendPasswordChangedEmail(String to, String name) {
        try {
            String body = "Hi " + name + ",\n\n"
                    + "Your password has been changed successfully.\n"
                    + "Date: " + DATE_FMT.format(java.time.LocalDateTime.now()) + "\n\n"
                    + "If you did not request this change, please contact support immediately.\n\n"
                    + "Thanks,\nVinoteca UTN Team";

            sendSimpleEmail(to, "Your password has been changed", body);
        } catch (Exception e) {
            log.error("Failed to send password changed email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendOrderCreatedEmail(String to, String name, OrderResponseDTO order) {
        try {
            String orderId = order.getExternalId().toString().substring(0, 8);

            StringBuilder body = new StringBuilder();
            body.append("Hi ").append(name).append(",\n\n");
            body.append("Your order has been created successfully.\n\n");
            body.append("Order #").append(orderId).append("\n");
            body.append("Date: ").append(DATE_FMT.format(order.getCreatedAt())).append("\n");
            body.append("Status: ").append(order.getStatus()).append("\n");
            body.append("Delivery: ").append(order.getDeliveryType()).append("\n\n");
            body.append("Items:\n");

            for (OrderItemResponseDTO item : order.getItems()) {
                body.append("  - Qty: ").append(item.getQuantity())
                        .append(" | Unit price: $").append(item.getUnitPrice())
                        .append(" | Subtotal: $").append(item.getSubtotal())
                        .append("\n");
            }

            body.append("\nTotal: $").append(order.getTotal()).append("\n\n");
            body.append("Thanks,\nVinoteca UTN Team");

            sendSimpleEmail(to, "Order created - #" + orderId, body.toString());
        } catch (Exception e) {
            log.error("Failed to send order created email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendOrderStatusChangedEmail(String to, String name, OrderResponseDTO order, String newStatus) {
        try {
            String orderId = order.getExternalId().toString().substring(0, 8);

            String body = "Hi " + name + ",\n\n"
                    + "Your order #" + orderId + " status has been updated.\n\n"
                    + "New status: " + newStatus + "\n"
                    + "Total: $" + order.getTotal() + "\n\n"
                    + "Thanks,\nVinoteca UTN Team";

            sendSimpleEmail(to, "Order status updated - #" + orderId, body);
        } catch (Exception e) {
            log.error("Failed to send order status email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendOrderCancelledEmail(String to, String name, OrderResponseDTO order, String reason) {
        try {
            String orderId = order.getExternalId().toString().substring(0, 8);

            String body = "Hi " + name + ",\n\n"
                    + "Your order #" + orderId + " has been cancelled.\n\n"
                    + "Reason: " + (reason != null ? reason : "No reason provided") + "\n"
                    + "Total: $" + order.getTotal() + "\n\n"
                    + "If you have any questions, please contact support.\n\n"
                    + "Thanks,\nVinoteca UTN Team";

            sendSimpleEmail(to, "Order cancelled - #" + orderId, body);
        } catch (Exception e) {
            log.error("Failed to send order cancelled email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendNewPromotionEmail(String to, String name, OfferResponseDTO offer) {
        try {
            StringBuilder body = new StringBuilder();
            body.append("Hi ").append(name).append(",\n\n");
            body.append("New promotion available: ").append(offer.name()).append("\n\n");
            body.append("Discount: ").append(offer.discountPercentage()).append("%\n");
            body.append("Valid from: ").append(DATE_ONLY.format(offer.startDate())).append(" to ").append(DATE_ONLY.format(offer.endDate())).append("\n\n");

            if (offer.offerProducts() != null && !offer.offerProducts().isEmpty()) {
                body.append("Wines included:\n");
                for (OfferProductResponseDTO wine : offer.offerProducts()) {
                    body.append("  - Wine variant: ").append(wine.wineVariantExternalId())
                            .append(" | Discounted price: $").append(wine.discountedPrice())
                            .append("\n");
                }
            }

            body.append("\nThanks,\nVinoteca UTN Team");

            sendSimpleEmail(to, "New promotion: " + offer.name(), body.toString());
        } catch (Exception e) {
            log.error("Failed to send promotion email to {}: {}", to, e.getMessage());
        }
    }

    private void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        log.info("Email sent to {} - Subject: {}", to, subject);
    }
}
