package com.example.vms.sample.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire format of an item. VMS JSON convention: snake_case field names, timestamps as epoch
 * milliseconds.
 *
 * <p>The same record is the payload of alarms, notifications and WebSocket messages, so the
 * frontend has one shape to render everywhere. The category name and camera are copied in: an
 * alarm keeps showing what the item had when it was created, even after the category is renamed,
 * rebound to another camera or deleted.
 */
public record ItemDTO(
        @JsonProperty("id") long id,
        @JsonProperty("text") String text,
        @JsonProperty("category_id") long categoryId,
        @JsonProperty("category_name") String categoryName,
        @JsonProperty("camera_id") Integer cameraId,
        @JsonProperty("created_by") int createdBy,
        @JsonProperty("created_at") long createdAt
) {
}
