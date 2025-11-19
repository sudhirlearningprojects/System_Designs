package org.sudhir512kj.ticketbooking.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationConsumerService {
    
    @KafkaListener(topics = "booking-email-notifications", groupId = "email-notification-group")
    public void handleEmailNotification(Map<String, Object> notificationData) {
        String notificationType = (String) notificationData.get("notificationType");
        String userEmail = (String) notificationData.get("userEmail");
        
        switch (notificationType) {
            case "BOOKING_CONFIRMATION":
                sendBookingConfirmationEmail(notificationData);
                break;
            case "TICKET_RESEND":
                resendTicketEmail(notificationData);
                break;
            case "PAYMENT_FAILED":
                sendPaymentFailedEmail(notificationData);
                break;
            case "BOOKING_CANCELLED":
                sendCancellationEmail(notificationData);
                break;
        }
    }
    
    @KafkaListener(topics = "booking-sms-notifications", groupId = "sms-notification-group")
    public void handleSmsNotification(Map<String, Object> notificationData) {
        String notificationType = (String) notificationData.get("notificationType");
        String userPhone = (String) notificationData.get("userPhone");
        
        switch (notificationType) {
            case "BOOKING_CONFIRMATION":
                sendBookingConfirmationSms(notificationData);
                break;
            case "EVENT_REMINDER":
                sendEventReminderSms(notificationData);
                break;
            case "PAYMENT_FAILED":
                sendPaymentFailedSms(notificationData);
                break;
            case "BOOKING_CANCELLED":
                sendCancellationSms(notificationData);
                break;
        }
    }
    
    @KafkaListener(topics = "booking-push-notifications", groupId = "push-notification-group")
    public void handlePushNotification(Map<String, Object> notificationData) {
        String notificationType = (String) notificationData.get("notificationType");
        String userId = (String) notificationData.get("userId");
        
        switch (notificationType) {
            case "BOOKING_CONFIRMATION":
                sendBookingConfirmationPush(notificationData);
                break;
            case "EVENT_REMINDER":
                sendEventReminderPush(notificationData);
                break;
            case "PAYMENT_FAILED":
                sendPaymentFailedPush(notificationData);
                break;
        }
    }
    
    private void sendBookingConfirmationEmail(Map<String, Object> data) {
        // Integration with SendGrid/AWS SES
        System.out.println("📧 EMAIL: Booking confirmed for " + data.get("eventName") + 
                          " | Ref: " + data.get("bookingReference") + 
                          " | To: " + data.get("userEmail"));
    }
    
    private void resendTicketEmail(Map<String, Object> data) {
        System.out.println("📧 EMAIL: Ticket resent for " + data.get("eventName") + 
                          " | QR: " + data.get("qrCode") + 
                          " | To: " + data.get("userEmail"));
    }
    
    private void sendPaymentFailedEmail(Map<String, Object> data) {
        System.out.println("📧 EMAIL: Payment failed for " + data.get("eventName") + 
                          " | Amount: ₹" + data.get("amount") + 
                          " | To: " + data.get("userEmail"));
    }
    
    private void sendCancellationEmail(Map<String, Object> data) {
        System.out.println("📧 EMAIL: Booking cancelled for " + data.get("eventName") + 
                          " | Refund: ₹" + data.get("refundAmount") + 
                          " | To: " + data.get("userEmail"));
    }
    
    private void sendBookingConfirmationSms(Map<String, Object> data) {
        // Integration with Twilio/AWS SNS
        System.out.println("📱 SMS: Your booking for " + data.get("eventName") + 
                          " is confirmed! Ref: " + data.get("bookingReference") + 
                          " | To: " + data.get("userPhone"));
    }
    
    private void sendEventReminderSms(Map<String, Object> data) {
        System.out.println("📱 SMS: Reminder: " + data.get("eventName") + 
                          " tomorrow at " + data.get("venueName") + 
                          " | To: " + data.get("userPhone"));
    }
    
    private void sendPaymentFailedSms(Map<String, Object> data) {
        System.out.println("📱 SMS: Payment failed for " + data.get("eventName") + 
                          ". Please retry. | To: " + data.get("userPhone"));
    }
    
    private void sendCancellationSms(Map<String, Object> data) {
        System.out.println("📱 SMS: Booking cancelled. Refund of ₹" + data.get("refundAmount") + 
                          " will be processed in 3-5 days | To: " + data.get("userPhone"));
    }
    
    private void sendBookingConfirmationPush(Map<String, Object> data) {
        // Integration with FCM/APNS
        System.out.println("🔔 PUSH: Booking confirmed! " + data.get("eventName") + 
                          " | User: " + data.get("userId"));
    }
    
    private void sendEventReminderPush(Map<String, Object> data) {
        System.out.println("🔔 PUSH: Don't forget! " + data.get("eventName") + 
                          " starts tomorrow | User: " + data.get("userId"));
    }
    
    private void sendPaymentFailedPush(Map<String, Object> data) {
        System.out.println("🔔 PUSH: Payment failed. Tap to retry | User: " + data.get("userId"));
    }
}