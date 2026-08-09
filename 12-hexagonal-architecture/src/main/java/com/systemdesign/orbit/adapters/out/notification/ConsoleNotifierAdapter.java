package com.systemdesign.orbit.adapters.out.notification;

import com.systemdesign.orbit.core.ports.out.NotifierPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Outbound/driven adapter for NotifierPort — logs to the console instead of sending email/SMS. */
public class ConsoleNotifierAdapter implements NotifierPort {

    private static final Logger log = LoggerFactory.getLogger("Notifier");

    @Override
    public void notify(String customerId, String message) {
        log.info("[notify -> {}] {}", customerId, message);
    }
}
