package com.example.vms.sample.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request body of {@code POST /api/v2/sample_plugin/items}. */
public record CreateItemDTO(
        @JsonProperty("text") String text,
        @JsonProperty("category_id") Long categoryId
) {
}
