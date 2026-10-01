package com.example.vms.sample.core;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Tiny in-plugin publish/subscribe bus.
 *
 * <p>It exists so that the HTTP layer stays unaware of everything that has to happen when an item
 * appears: the rule engine bridge and the WebSocket feed subscribe here instead. Real plugins have
 * the same shape, with the events coming from a device driver or a message queue rather than from
 * their own REST controller.
 *
 * <p>A listener that throws must never break the publisher, hence the try/catch: an alarm that
 * cannot be raised is not a reason to fail the HTTP request that created the item.
 */
@Singleton
public class ItemEventBus {
    private static final Logger log = LoggerFactory.getLogger(ItemEventBus.class);

    private final List<Consumer<ItemEvent>> listeners = new CopyOnWriteArrayList<>();

    public void addListener(Consumer<ItemEvent> listener) {
        listeners.add(listener);
    }

    public void publish(ItemEvent event) {
        for (Consumer<ItemEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException e) {
                log.error("Item event listener failed for {}", event.type(), e);
            }
        }
    }
}
