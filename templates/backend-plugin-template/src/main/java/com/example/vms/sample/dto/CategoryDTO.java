package com.example.vms.sample.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A category. {@code source_id} is the id rules store in their "Where" step; the rule wizard
 * block of the frontend writes it into {@code source_ids}. {@code camera_id} is the host camera
 * behind the category, or null: the host attaches it to alarms of this category.
 */
public record CategoryDTO(
        @JsonProperty("id") long id,
        @JsonProperty("source_id") String sourceId,
        @JsonProperty("name") String name,
        @JsonProperty("camera_id") Integer cameraId,
        @JsonProperty("created_at") long createdAt
) {
}
