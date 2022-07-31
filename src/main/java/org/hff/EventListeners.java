package org.hff;

import emu.grasscutter.server.event.game.ReceiveCommandFeedbackEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EventListeners {

    private static final Map<String, String> messages = new ConcurrentHashMap<>();

    public static void onCommandSend(ReceiveCommandFeedbackEvent event) {
        if (event.getPlayer() != null) {
            messages.put(event.getPlayer().getAccount().getId(), event.getMessage());
        } else {
            messages.put("admin", event.getMessage());
        }
    }

    public static String getMessage(String accountId) {
        String message = messages.get(accountId);
        if (message != null) {
            messages.remove(accountId);
        }
        return message;
    }

}
