package com.example.vms.sample.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request body of creating and editing a category. {@code camera_id} is optional. */
public record CategoryRequestDTO(
        @JsonProperty("name") String name,
        @JsonProperty("camera_id") Integer cameraId
) {
}
