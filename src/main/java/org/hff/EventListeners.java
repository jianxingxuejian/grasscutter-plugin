package org.hff;

import emu.grasscutter.server.event.game.ReceiveCommandFeedbackEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EventListeners {

    private static final Map<String, String> messages = new ConcurrentHashMap<>();

    public static void onCommandSend(ReceiveCommandFeedbackEvent event) {
        if (event.getPlayer() != null) {
            messages.put(event.getPlayer().getAccount().getId(), event.getMessage());
        }
    }

    public static String getMessage(String uid) {
        return messages.get(uid);
    }
}
