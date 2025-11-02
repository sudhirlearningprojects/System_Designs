package org.sudhir512kj.digitalpayment.service;

import org.sudhir512kj.digitalpayment.dto.PaymentRequest;
import org.sudhir512kj.digitalpayment.dto.PaymentResponse;

public interface PaymentGatewayStrategy {
    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse getTransactionStatus(String pspTransactionId);
    boolean supportsPaymentMethod(String paymentMethod);
}

class UpiGateway implements PaymentGatewayStrategy {
    public PaymentResponse processPayment(PaymentRequest request) {
        // UPI payment processing logic
        return new PaymentResponse("UPI_" + System.currentTimeMillis(), "SUCCESS", "Payment processed via UPI");
    }
    
    public PaymentResponse getTransactionStatus(String pspTransactionId) {
        // Check UPI transaction status
        return new PaymentResponse(pspTransactionId, "SUCCESS", "Transaction completed");
    }
    
    public boolean supportsPaymentMethod(String paymentMethod) {
        return "UPI".equals(paymentMethod);
    }
}

class CardGateway implements PaymentGatewayStrategy {
    public PaymentResponse processPayment(PaymentRequest request) {
        // Card payment processing logic
        return new PaymentResponse("CARD_" + System.currentTimeMillis(), "SUCCESS", "Payment processed via Card");
    }
    
    public PaymentResponse getTransactionStatus(String pspTransactionId) {
        // Check card transaction status
        return new PaymentResponse(pspTransactionId, "SUCCESS", "Transaction completed");
    }
    
    public boolean supportsPaymentMethod(String paymentMethod) {
        return "CREDIT_CARD".equals(paymentMethod) || "DEBIT_CARD".equals(paymentMethod);
    }
}

class NetBankingGateway implements PaymentGatewayStrategy {
    public PaymentResponse processPayment(PaymentRequest request) {
        // Net banking payment processing logic
        return new PaymentResponse("NB_" + System.currentTimeMillis(), "SUCCESS", "Payment processed via Net Banking");
    }
    
    public PaymentResponse getTransactionStatus(String pspTransactionId) {
        // Check net banking transaction status
        return new PaymentResponse(pspTransactionId, "SUCCESS", "Transaction completed");
    }
    
    public boolean supportsPaymentMethod(String paymentMethod) {
        return "NET_BANKING".equals(paymentMethod);
    }
}