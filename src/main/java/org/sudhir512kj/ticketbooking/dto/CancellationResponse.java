package org.sudhir512kj.ticketbooking.dto;

import java.math.BigDecimal;

public class CancellationResponse {
    private String status;
    private BigDecimal refundAmount;
    private String refundId;
    
    public CancellationResponse() {}
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    
    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }
}