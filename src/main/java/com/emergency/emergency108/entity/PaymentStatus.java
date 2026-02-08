package com.emergency.emergency108.entity;

/**
 * Payment status for emergencies requiring payment.
 * 
 * NOT_REQUIRED: Government emergency (free service)
 * PENDING: Payment not yet initiated
 * AUTHORIZED: Payment authorization placed (hold on card)
 * PAID: Payment captured successfully
 * FAILED: Payment authorization/capture failed
 * REFUNDED: Payment refunded to user
 * 
 * FUTURE INTEGRATION:
 * - Razorpay/Stripe payment gateway
 * - Payment intent tracking
 * - Automatic capture on completion
 */
public enum PaymentStatus {
    NOT_REQUIRED,  // Free emergency
    PENDING,       // Payment not captured
    AUTHORIZED,    // Payment hold placed
    PAID,          // Payment captured
    FAILED,        // Payment failed
    REFUNDED       // Payment refunded
}
