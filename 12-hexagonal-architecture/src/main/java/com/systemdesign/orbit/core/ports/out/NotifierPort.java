package com.systemdesign.orbit.core.ports.out;

/** Output port: what the core needs to tell a customer something happened. */
public interface NotifierPort {

    void notify(String customerId, String message);
}
