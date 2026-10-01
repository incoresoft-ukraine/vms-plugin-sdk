package com.example.vms.sample.http;

import com.example.vms.sample.PluginExtension;
import com.google.inject.Inject;
import com.incoresoft.middleware.http.AppRole;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.io.InputStream;
import java.util.Map;

/**
 * Serves the plugin's frontend from the jar. This is a HOST CONTRACT: the VMS frontend loads a
 * plugin from exactly these URLs, so keep the paths as they are.
 * <ul>
 *   <li>{@code GET /resources/<plugin_name>/icon} → the icon resource at the jar root;</li>
 *   <li>{@code GET /resources/<plugin_name>/*} → files under {@code public/} in the jar: the Module
 *       Federation entry {@code app.js}, its chunks, {@code component.json}, {@code settings.json}.</li>
 * </ul>
 * Both routes are public ({@link AppRole#ANYONE}): the browser fetches them before login. Everything
 * but {@code app.js} may be cached, since chunk names carry a content hash.
 */
public class FrontendResources {
    private static final String ICON = "icon.svg";
    private static final String NOT_CACHED = "app.js";
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "js", "text/javascript",
            "css", "text/css",
            "json", "application/json",
            "svg", "image/svg+xml",
            "png", "image/png",
            "jpg", "image/jpeg",
            "woff", "font/woff",
            "woff2", "font/woff2",
            "ttf", "font/ttf");

    private final String prefix = "/resources/" + PluginExtension.PLUGIN_NAME + "/";

    @Inject
    public FrontendResources(Javalin javalin) {
        javalin.get(prefix + "icon", ctx -> send(ctx, ICON), AppRole.ANYONE);
        javalin.get(prefix + "*", ctx -> {
            String path = ctx.path().substring(prefix.length());
            if (!path.equals(NOT_CACHED)) {
                ctx.header("Cache-Control", "public, max-age=3600");
            }
            send(ctx, "public/" + path);
        }, AppRole.ANYONE);
    }

    private void send(Context ctx, String resource) {
        if (resource.contains("..")) {
            throw new NotFoundResponse();
        }
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new NotFoundResponse();
        }
        int dot = resource.lastIndexOf('.');
        String extension = dot < 0 ? "" : resource.substring(dot + 1);
        ctx.contentType(CONTENT_TYPES.getOrDefault(extension, "application/octet-stream"));
        ctx.result(stream);
    }
}
