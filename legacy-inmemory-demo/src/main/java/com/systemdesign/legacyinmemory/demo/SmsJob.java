package com.systemdesign.legacyinmemory.demo;

/** Demo-only payload for Act 2's "flaky sms" retry/DLQ walkthrough. */
public record SmsJob(String to, boolean willFailForever) {
}
