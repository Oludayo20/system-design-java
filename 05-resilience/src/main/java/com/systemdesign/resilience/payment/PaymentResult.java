package com.systemdesign.resilience.payment;

public record PaymentResult(String provider, String reference, String status) {}
