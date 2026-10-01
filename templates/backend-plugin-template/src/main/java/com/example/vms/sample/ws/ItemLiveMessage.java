package com.example.vms.sample.ws;

import com.example.vms.sample.dto.ItemDTO;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message pushed over the live WebSocket.
 *
 * <p>The {@code type} field lets the client tell payloads apart, including the periodic
 * {@code HEALTH_CHECK} that keeps proxies from closing an idle connection.
 */
public record ItemLiveMessage(
        @JsonProperty("type") String type,
        @JsonProperty("item") ItemDTO item
) {
    public static final String TYPE_ITEM_CREATED = "ITEM_CREATED";
    public static final String TYPE_ITEM_DELETED = "ITEM_DELETED";
    public static final String TYPE_HEALTH_CHECK = "HEALTH_CHECK";

    public static ItemLiveMessage created(ItemDTO item) {
        return new ItemLiveMessage(TYPE_ITEM_CREATED, item);
    }

    public static ItemLiveMessage deleted(ItemDTO item) {
        return new ItemLiveMessage(TYPE_ITEM_DELETED, item);
    }

    public static ItemLiveMessage healthCheck() {
        return new ItemLiveMessage(TYPE_HEALTH_CHECK, null);
    }
}
