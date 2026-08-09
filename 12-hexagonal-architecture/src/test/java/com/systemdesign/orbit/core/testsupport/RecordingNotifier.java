package com.systemdesign.orbit.core.testsupport;

import com.systemdesign.orbit.core.ports.out.NotifierPort;
import java.util.ArrayList;
import java.util.List;

public class RecordingNotifier implements NotifierPort {

    public record Message(String customerId, String message) {
    }

    public final List<Message> messages = new ArrayList<>();

    @Override
    public void notify(String customerId, String message) {
        messages.add(new Message(customerId, message));
    }
}
