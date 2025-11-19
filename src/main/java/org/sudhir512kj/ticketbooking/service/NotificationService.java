package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.ticketbooking.model.*;
import org.sudhir512kj.ticketbooking.repository.*;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    private static final String EMAIL_TOPIC = "booking-email-notifications";
    private static final String SMS_TOPIC = "booking-sms-notifications";
    private static final String PUSH_TOPIC = "booking-push-notifications";
    
    public void sendBookingConfirmation(Booking booking) {
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("userId", booking.getUser().getId());
        notificationData.put("userEmail", booking.getUser().getEmail());
        notificationData.put("userPhone", booking.getUser().getPhoneNumber());
        notificationData.put("notificationType", "BOOKING_CONFIRMATION");
        notificationData.put("bookingReference", booking.getBookingReference());
        notificationData.put("eventName", booking.getShow().getEvent().getName());
        notificationData.put("showDate", booking.getShow().getShowDate().toString());
        notificationData.put("venueName", booking.getShow().getVenue().getName());
        notificationData.put("totalAmount", booking.getTotalAmount().toString());
        notificationData.put("qrCode", booking.getQrCode());
        
        // Send to multiple channels via Kafka
        kafkaTemplate.send(EMAIL_TOPIC, booking.getUser().getId().toString(), notificationData);
        kafkaTemplate.send(SMS_TOPIC, booking.getUser().getId().toString(), notificationData);
        kafkaTemplate.send(PUSH_TOPIC, booking.getUser().getId().toString(), notificationData);
    }
    
    public void resendTicket(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Cannot resend ticket for non-confirmed booking");
        }
        
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("userId", booking.getUser().getId());
        notificationData.put("userEmail", booking.getUser().getEmail());
        notificationData.put("notificationType", "TICKET_RESEND");
        notificationData.put("bookingReference", booking.getBookingReference());
        notificationData.put("eventName", booking.getShow().getEvent().getName());
        notificationData.put("qrCode", booking.getQrCode());
        
        // Send ticket via email primarily
        kafkaTemplate.send(EMAIL_TOPIC, booking.getUser().getId().toString(), notificationData);
    }
    
    public void sendEventReminder(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("userId", booking.getUser().getId());
        notificationData.put("userEmail", booking.getUser().getEmail());
        notificationData.put("userPhone", booking.getUser().getPhoneNumber());
        notificationData.put("notificationType", "EVENT_REMINDER");
        notificationData.put("eventName", booking.getShow().getEvent().getName());
        notificationData.put("showDate", booking.getShow().getShowDate().toString());
        notificationData.put("venueName", booking.getShow().getVenue().getName());
        notificationData.put("venueAddress", booking.getShow().getVenue().getAddress());
        
        // Send reminder via SMS and Push
        kafkaTemplate.send(SMS_TOPIC, booking.getUser().getId().toString(), notificationData);
        kafkaTemplate.send(PUSH_TOPIC, booking.getUser().getId().toString(), notificationData);
    }
    
    public void sendPaymentFailedNotification(Booking booking) {
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("userId", booking.getUser().getId());
        notificationData.put("userEmail", booking.getUser().getEmail());
        notificationData.put("userPhone", booking.getUser().getPhoneNumber());
        notificationData.put("notificationType", "PAYMENT_FAILED");
        notificationData.put("bookingReference", booking.getBookingReference());
        notificationData.put("eventName", booking.getShow().getEvent().getName());
        notificationData.put("amount", booking.getTotalAmount().toString());
        
        // Send urgent payment failure notification via all channels
        kafkaTemplate.send(EMAIL_TOPIC, booking.getUser().getId().toString(), notificationData);
        kafkaTemplate.send(SMS_TOPIC, booking.getUser().getId().toString(), notificationData);
        kafkaTemplate.send(PUSH_TOPIC, booking.getUser().getId().toString(), notificationData);
    }
    
    public void sendCancellationNotification(Booking booking) {
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("userId", booking.getUser().getId());
        notificationData.put("userEmail", booking.getUser().getEmail());
        notificationData.put("notificationType", "BOOKING_CANCELLED");
        notificationData.put("bookingReference", booking.getBookingReference());
        notificationData.put("eventName", booking.getShow().getEvent().getName());
        notificationData.put("refundAmount", booking.getTotalAmount().toString());
        
        kafkaTemplate.send(EMAIL_TOPIC, booking.getUser().getId().toString(), notificationData);
        kafkaTemplate.send(SMS_TOPIC, booking.getUser().getId().toString(), notificationData);
    }
}