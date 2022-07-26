package org.hff;

import emu.grasscutter.server.event.game.ReceiveCommandFeedbackEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EventListeners {

    private static final Map<Integer, String> message = new ConcurrentHashMap<>();

    public static void onCommandSend(ReceiveCommandFeedbackEvent event) {
        if (event.getPlayer() != null) {
            message.put(event.getPlayer().getUid(), event.getMessage());
        }
    }
}
