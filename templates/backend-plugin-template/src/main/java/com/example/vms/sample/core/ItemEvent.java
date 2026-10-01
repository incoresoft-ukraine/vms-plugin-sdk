package com.example.vms.sample.core;

import com.example.vms.sample.dto.ItemDTO;

/** Something happened to an item. Published on {@link ItemEventBus}. */
public record ItemEvent(Type type, ItemDTO item) {
    public enum Type {
        CREATED,
        DELETED
    }

    public static ItemEvent created(ItemDTO item) {
        return new ItemEvent(Type.CREATED, item);
    }

    public static ItemEvent deleted(ItemDTO item) {
        return new ItemEvent(Type.DELETED, item);
    }
}
