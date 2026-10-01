package com.example.vms.sample.http;

import com.example.vms.sample.PluginExtension;
import com.example.vms.sample.core.ItemEvent;
import com.example.vms.sample.core.ItemEventBus;
import com.example.vms.sample.db.CategoriesRepository;
import com.example.vms.sample.db.ItemsQuery;
import com.example.vms.sample.db.ItemsRepository;
import com.example.vms.sample.dto.CategoryDTO;
import com.example.vms.sample.dto.CreateItemDTO;
import com.example.vms.sample.dto.ItemDTO;
import com.example.vms.sample.dto.PageDTO;
import com.google.inject.Inject;
import com.incoresoft.middleware.http.exceptions.ValidationException;
import com.incoresoft.middleware.model.user.User;
import com.incoresoft.middleware.model.user.UsersManager;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;

import java.util.ArrayList;
import java.util.List;

import static com.example.vms.sample.permissions.SamplePermissions.MANAGE_SAMPLE_ITEMS;
import static com.example.vms.sample.permissions.SamplePermissions.VIEW_SAMPLE_ITEMS;
import static com.incoresoft.middleware.http.AppRole.API_CALL;
import static com.incoresoft.middleware.http.AppRole.LOGGED_IN;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

/**
 * Items API, used by operators in the client.
 *
 * <ul>
 *   <li>Base path {@code /api/v2/<plugin_name>/...}; the host reserves {@code /api/v1/*}.</li>
 *   <li>{@code LOGGED_IN} (session) and {@code API_CALL} (API token) authenticate the request.
 *       Permissions added as route roles are checked with AND semantics.</li>
 *   <li>Writes publish an event; the rule bridge and the live WebSocket feed listen to it.</li>
 * </ul>
 */
public class ItemsController {
    public static final String API_PATH = "/api/v2/" + PluginExtension.PLUGIN_NAME + "/items";
    static final int TEXT_MAX_LENGTH = 512;
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 500;

    private final ItemsRepository items;
    private final CategoriesRepository categories;
    private final UsersManager usersManager;
    private final ItemEventBus eventBus;

    @Inject
    public ItemsController(ItemsRepository items,
                           CategoriesRepository categories,
                           UsersManager usersManager,
                           ItemEventBus eventBus) {
        this.items = items;
        this.categories = categories;
        this.usersManager = usersManager;
        this.eventBus = eventBus;
    }

    /** Guice calls this after construction because of @Inject on the method. */
    @Inject
    public void init(Javalin app) {
        app.routes(() -> path(API_PATH, () -> {
            get(this::list, LOGGED_IN, API_CALL, VIEW_SAMPLE_ITEMS);
            post(this::create, LOGGED_IN, API_CALL, MANAGE_SAMPLE_ITEMS);
            path("{id}", () -> {
                get(this::getOne, LOGGED_IN, API_CALL, VIEW_SAMPLE_ITEMS);
                delete(this::remove, LOGGED_IN, API_CALL, MANAGE_SAMPLE_ITEMS);
            });
        }));
    }

    /**
     * One page of items. Lists grow without bound, so even a sample never returns everything at once.
     *
     * <p>Query parameters, all optional: {@code category_id} (the client page) or
     * {@code category_ids=[1,2]} (the Search page), {@code camera_ids=[1,2]}, {@code text},
     * {@code start_date} and {@code end_date} in epoch milliseconds, {@code sort_order} asc/desc
     * (newest first by default), {@code limit}, {@code offset}. The names follow the host's own
     * search endpoints, so the Search page can pass its filters through.
     */
    private void list(Context ctx) {
        List<Long> categoryIds = new ArrayList<>(
                Requests.listParam(ctx, "category_ids", Long::parseLong, "SAMPLE_CATEGORY_NOT_FOUND"));
        Long categoryId = Requests.longParam(ctx, "category_id", "SAMPLE_CATEGORY_NOT_FOUND");
        if (categoryId != null) {
            categoryIds.add(categoryId);
        }
        ItemsQuery query = new ItemsQuery(
                categoryIds,
                Requests.listParam(ctx, "camera_ids", Integer::parseInt, "CAMERA_NOT_FOUND"),
                ctx.queryParam("text"),
                Requests.longParam(ctx, "start_date", Requests.DESERIALIZATION_FAILED),
                Requests.longParam(ctx, "end_date", Requests.DESERIALIZATION_FAILED),
                !"asc".equalsIgnoreCase(ctx.queryParam("sort_order"))
        );
        int limit = Requests.intParam(ctx, "limit", DEFAULT_PAGE_SIZE, 1, MAX_PAGE_SIZE);
        int offset = Requests.intParam(ctx, "offset", 0, 0, Integer.MAX_VALUE);

        ctx.json(PageDTO.of(items.findPage(query, limit, offset), items.count(query), limit));
    }

    private void getOne(Context ctx) {
        ctx.json(items.find(Requests.pathId(ctx)).orElseThrow(NotFoundResponse::new));
    }

    private void create(Context ctx) {
        CreateItemDTO body = Requests.body(ctx, CreateItemDTO.class);
        String text = Requests.requiredText(body.text(), "text", TEXT_MAX_LENGTH, "SAMPLE_TEXT_TOO_LONG");
        if (body.categoryId() == null) {
            throw new ValidationException(Requests.FIELD_REQUIRED, "category_id");
        }
        CategoryDTO category = categories.find(body.categoryId())
                .orElseThrow(() -> new ValidationException("SAMPLE_CATEGORY_NOT_FOUND", "category_id"));
        User user = usersManager.getUser(ctx); // current user (session or API token)
        // The item remembers the category's camera as it is now.
        ItemDTO created = items.insert(text, category.id(), category.cameraId(), user.getId());
        eventBus.publish(ItemEvent.created(created));
        ctx.status(HttpStatus.CREATED).json(created);
    }

    private void remove(Context ctx) {
        long id = Requests.pathId(ctx);
        ItemDTO item = items.find(id).orElseThrow(NotFoundResponse::new);
        if (!items.delete(id)) {
            throw new NotFoundResponse();
        }
        eventBus.publish(ItemEvent.deleted(item));
        ctx.status(HttpStatus.NO_CONTENT);
    }
}
