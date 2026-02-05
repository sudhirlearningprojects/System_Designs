package org.sudhir512kj.probability.model;

public enum MarketStatus {
    OPEN,       // Active trading
    LOCKED,     // Awaiting resolution
    RESOLVED,   // Settled
    CANCELLED   // Cancelled by admin
}
