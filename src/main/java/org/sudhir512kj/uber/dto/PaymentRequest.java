package org.sudhir512kj.uber.dto;

import org.sudhir512kj.uber.model.Payment;
import java.util.UUID;

public class PaymentRequest {
    private UUID rideId;
    private Payment.PaymentMethod paymentMethod;
    private String cardToken; // For Stripe card payments
    
    public UUID getRideId() { return rideId; }
    public void setRideId(UUID rideId) { this.rideId = rideId; }
    
    public Payment.PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(Payment.PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }
}
