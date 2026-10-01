package com.example.vms.sample.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One page of a list, in the shape the host uses for its own paginated answers:
 * the rows, the total number of rows and the number of pages for the requested page size.
 */
public record PageDTO<T>(
        @JsonProperty("data") List<T> data,
        @JsonProperty("total") long total,
        @JsonProperty("pages") int pages
) {
    public static <T> PageDTO<T> of(List<T> data, long total, int limit) {
        return new PageDTO<>(data, total, (int) Math.ceil(total / (double) limit));
    }
}
