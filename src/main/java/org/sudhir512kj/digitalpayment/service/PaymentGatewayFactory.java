package org.sudhir512kj.digitalpayment.service;

import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayFactory {
    
    public PaymentGatewayStrategy getPaymentGateway(String paymentMethod) {
        switch (paymentMethod.toUpperCase()) {
            case "UPI":
                return new UpiGateway();
            case "CREDIT_CARD":
            case "DEBIT_CARD":
                return new CardGateway();
            case "NET_BANKING":
                return new NetBankingGateway();
            default:
                throw new IllegalArgumentException("Unsupported payment method: " + paymentMethod);
        }
    }
}