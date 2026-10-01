package com.example.vms.sample.http;

import com.incoresoft.middleware.http.exceptions.ValidationException;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Request parsing that answers 400/404 in the host's error format instead of letting parse errors
 * fall through to the host's catch-all 500.
 *
 * <p>Errors are {@code [{type, field, args}]}; {@code type} is a locale key. With a {@code field}
 * the UI shows the message under that input, without one it shows a toast.
 */
final class Requests {
    /** Host locale keys reused by the plugin. */
    static final String FIELD_REQUIRED = "FIELD_REQUIRED";
    static final String DESERIALIZATION_FAILED = "ERROR_DESERIALIZATION_FAILED";

    private Requests() {
    }

    static <T> T body(Context ctx, Class<T> type) {
        try {
            T body = ctx.bodyAsClass(type);
            if (body == null) {
                throw new ValidationException(DESERIALIZATION_FAILED, null);
            }
            return body;
        } catch (ValidationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ValidationException(DESERIALIZATION_FAILED, null);
        }
    }

    /** Integer query parameter clamped to [min, max]; {@code def} when absent or not a number. */
    static int intParam(Context ctx, String name, int def, int min, int max) {
        String value = ctx.queryParam(name);
        if (value == null || value.isBlank()) {
            return def;
        }
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value.trim())));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Optional whole-number query parameter; {@code null} when absent, {@code invalidKey} when not a number. */
    static Long longParam(Context ctx, String name, String invalidKey) {
        String value = ctx.queryParam(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(invalidKey, name);
        }
    }

    /**
     * List query parameter. The host frontend sends arrays as {@code ids=[1,2]}; a plain
     * {@code ids=1,2} is accepted too. Absent or empty gives an empty list.
     */
    static <T> List<T> listParam(Context ctx, String name, Function<String, T> parse, String invalidKey) {
        String value = ctx.queryParam(name);
        if (value == null) {
            return List.of();
        }
        String body = value.trim();
        if (body.startsWith("[") && body.endsWith("]")) {
            body = body.substring(1, body.length() - 1);
        }
        List<T> result = new ArrayList<>();
        for (String part : body.split(",")) {
            String item = part.trim().replace("\"", "");
            if (item.isEmpty()) {
                continue;
            }
            try {
                result.add(parse.apply(item));
            } catch (RuntimeException e) {
                throw new ValidationException(invalidKey, name);
            }
        }
        return result;
    }

    static long pathId(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new NotFoundResponse();
        }
    }

    /** Trimmed text; FIELD_REQUIRED when blank, {@code tooLongKey} when longer than {@code max}. */
    static String requiredText(String value, String field, int max, String tooLongKey) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new ValidationException(FIELD_REQUIRED, field);
        }
        if (text.length() > max) {
            throw new ValidationException(tooLongKey, field);
        }
        return text;
    }
}
