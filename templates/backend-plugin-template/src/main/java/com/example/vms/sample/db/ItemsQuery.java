package com.example.vms.sample.db;

import java.util.List;

/**
 * Filters of the items list. Every field is optional: null or an empty list means "any".
 * The client page uses the category and the Search page uses the rest.
 *
 * @param categoryIds   categories the items belong to
 * @param cameraIds     cameras the items were created with
 * @param text          a fragment of the text, matched without regard to case
 * @param from          created at or after this moment, epoch milliseconds
 * @param to            created at or before this moment, epoch milliseconds
 * @param newestFirst   sort order
 */
public record ItemsQuery(
        List<Long> categoryIds,
        List<Integer> cameraIds,
        String text,
        Long from,
        Long to,
        boolean newestFirst
) {
}
