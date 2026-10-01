package com.example.vms.sample.ws;

import com.example.vms.sample.PluginExtension;
import com.example.vms.sample.core.ItemEvent;
import com.example.vms.sample.core.ItemEventBus;
import com.example.vms.sample.permissions.SamplePermissions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.incoresoft.middleware.http.serialization.Serialization;
import com.incoresoft.middleware.model.user.User;
import com.incoresoft.middleware.model.user.UsersManager;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.incoresoft.middleware.http.AppRole.API_CALL;
import static com.incoresoft.middleware.http.AppRole.LOGGED_IN;

/**
 * Live feed of item changes at {@code /api/v2/ws/sample_plugin/items}.
 *
 * <p>Notes that are easy to get wrong:
 * <ul>
 *   <li>The route roles authenticate the upgrade request, but they do not check plugin
 *       permissions. Check those yourself in {@code onConnect} and close the socket with 1008 when
 *       the user is not allowed; check again on every send, permissions can be revoked meanwhile.</li>
 *   <li>{@code enableAutomaticPings()} plus the periodic health-check message keep the connection
 *       alive through reverse proxies.</li>
 *   <li>Sessions must be removed on both close and error, otherwise the set grows forever.</li>
 *   <li>The scheduler thread has to be stopped in {@code terminate()}: the plugin classloader is
 *       discarded when the plugin is unloaded, and a live thread keeps the whole plugin in memory.</li>
 * </ul>
 */
@Singleton
public class ItemsWebSocketController {
    private static final Logger log = LoggerFactory.getLogger(ItemsWebSocketController.class);

    public static final String WS_PATH = "/api/v2/ws/" + PluginExtension.PLUGIN_NAME + "/items";

    private static final long HEALTH_CHECK_INTERVAL_S = 30;
    /** RFC 6455 policy violation. */
    private static final int WS_CLOSE_POLICY_VIOLATION = 1008;

    /** Open sessions and the id of the user behind each one. */
    private final Map<WsContext, Integer> sessions = new ConcurrentHashMap<>();
    private final UsersManager usersManager;
    private final ScheduledExecutorService scheduler;

    @Inject
    public ItemsWebSocketController(Javalin app, UsersManager usersManager, ItemEventBus eventBus) {
        this.usersManager = usersManager;
        app.ws(WS_PATH, ws -> {
            ws.onConnect(context -> {
                // getUser returns null when the session is gone; treat it like a missing permission.
                User user = usersManager.getUser(context);
                if (user == null || !user.hasPermission(SamplePermissions.VIEW_SAMPLE_ITEMS)) {
                    context.closeSession(WS_CLOSE_POLICY_VIOLATION, "Forbidden");
                    return;
                }
                context.enableAutomaticPings();
                sessions.put(context, user.getId());
                log.debug("Sample items WS client connected, total {}", sessions.size());
            });
            ws.onClose(context -> sessions.remove(context));
            ws.onError(context -> sessions.remove(context));
        }, LOGGED_IN, API_CALL);

        eventBus.addListener(this::onItemEvent);

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sample-plugin-ws-health");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::sendHealthCheck,
                HEALTH_CHECK_INTERVAL_S, HEALTH_CHECK_INTERVAL_S, TimeUnit.SECONDS);
    }

    private void onItemEvent(ItemEvent event) {
        if (sessions.isEmpty()) {
            return;
        }
        broadcast(switch (event.type()) {
            case CREATED -> ItemLiveMessage.created(event.item());
            case DELETED -> ItemLiveMessage.deleted(event.item());
        });
    }

    private void sendHealthCheck() {
        if (!sessions.isEmpty()) {
            broadcast(ItemLiveMessage.healthCheck());
        }
    }

    private void broadcast(ItemLiveMessage message) {
        String payload;
        try {
            payload = Serialization.objectMapper().writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize WS message", e);
            return;
        }
        sessions.forEach((session, userId) -> {
            if (!session.session.isOpen()) {
                sessions.remove(session);
                return;
            }
            // Permissions can be revoked while the socket is open: check the user on every send.
            if (!mayReceive(userId)) {
                sessions.remove(session);
                closeQuietly(session, WS_CLOSE_POLICY_VIOLATION);
                return;
            }
            try {
                session.send(payload);
            } catch (RuntimeException e) {
                log.debug("Dropping a WS session that failed to receive a message", e);
                sessions.remove(session);
            }
        });
    }

    /** A deleted user, or one whose roles cannot be resolved, receives nothing. */
    private boolean mayReceive(int userId) {
        try {
            User user = usersManager.get(userId);
            return user != null && user.hasPermission(SamplePermissions.VIEW_SAMPLE_ITEMS);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Called from {@code PluginExtension.terminate()}. */
    public void terminate() {
        scheduler.shutdownNow();
        sessions.keySet().forEach(session -> closeQuietly(session, null));
        sessions.clear();
    }

    private static void closeQuietly(WsContext session, Integer code) {
        try {
            if (code == null) {
                session.closeSession();
            } else {
                session.closeSession(code, "Forbidden");
            }
        } catch (RuntimeException ignored) {
            // already closed
        }
    }
}
