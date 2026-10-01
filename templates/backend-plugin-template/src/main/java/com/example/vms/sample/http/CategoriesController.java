package com.example.vms.sample.http;

import com.example.vms.sample.PluginExtension;
import com.example.vms.sample.db.CategoriesRepository;
import com.example.vms.sample.db.ItemsRepository;
import com.example.vms.sample.dto.CategoryDTO;
import com.example.vms.sample.dto.CategoryRequestDTO;
import com.example.vms.sample.rules.SampleRuleManager;
import com.google.inject.Inject;
import com.incoresoft.middleware.http.exceptions.ValidationException;
import com.incoresoft.vms.drivers.sdk.DeviceManager;
import com.incoresoft.vms.drivers.sdk.item.DeviceItemType;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;

import static com.example.vms.sample.permissions.SamplePermissions.MANAGE_SAMPLE_CATEGORIES;
import static com.example.vms.sample.permissions.SamplePermissions.VIEW_SAMPLE_ITEMS;
import static com.incoresoft.middleware.http.AppRole.API_CALL;
import static com.incoresoft.middleware.http.AppRole.LOGGED_IN;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

/**
 * Categories API. Reading is for everyone who works with items; changing is a setting of the
 * plugin, done in Admin Center, and needs its own permission.
 */
public class CategoriesController {
    public static final String API_PATH = "/api/v2/" + PluginExtension.PLUGIN_NAME + "/categories";
    static final int NAME_MAX_LENGTH = 128;

    private final CategoriesRepository categories;
    private final ItemsRepository items;
    private final SampleRuleManager ruleManager;
    private final DeviceManager deviceManager;

    @Inject
    public CategoriesController(CategoriesRepository categories,
                                ItemsRepository items,
                                SampleRuleManager ruleManager,
                                DeviceManager deviceManager) {
        this.categories = categories;
        this.items = items;
        this.ruleManager = ruleManager;
        this.deviceManager = deviceManager;
    }

    @Inject
    public void init(Javalin app) {
        app.routes(() -> path(API_PATH, () -> {
            get(ctx -> ctx.json(categories.findAll()), LOGGED_IN, API_CALL, VIEW_SAMPLE_ITEMS);
            post(this::create, LOGGED_IN, API_CALL, MANAGE_SAMPLE_CATEGORIES);
            path("{id}", () -> {
                put(this::update, LOGGED_IN, API_CALL, MANAGE_SAMPLE_CATEGORIES);
                delete(this::remove, LOGGED_IN, API_CALL, MANAGE_SAMPLE_CATEGORIES);
            });
        }));
    }

    private void create(Context ctx) {
        CategoryRequestDTO body = Requests.body(ctx, CategoryRequestDTO.class);
        String name = validName(body, null);
        Integer cameraId = validCameraId(body);
        CategoryDTO created = categories.insert(name, cameraId);
        ruleManager.getRuleType().invalidateSources();
        ctx.status(HttpStatus.CREATED).json(created);
    }

    private void update(Context ctx) {
        long id = Requests.pathId(ctx);
        categories.find(id).orElseThrow(NotFoundResponse::new);
        CategoryRequestDTO body = Requests.body(ctx, CategoryRequestDTO.class);
        String name = validName(body, id);
        Integer cameraId = validCameraId(body);
        categories.update(id, name, cameraId);
        ruleManager.getRuleType().invalidateSources();
        CategoryDTO updated = categories.find(id).orElseThrow(NotFoundResponse::new);
        ctx.json(updated);
    }

    /**
     * A category with items cannot be deleted: the operator would lose them silently. Rules that
     * still point at the category keep its source id and simply stop matching; the host hides the
     * alarms of a source that no longer exists.
     */
    private void remove(Context ctx) {
        long id = Requests.pathId(ctx);
        categories.find(id).orElseThrow(NotFoundResponse::new);
        if (items.existInCategory(id)) {
            throw new ValidationException("SAMPLE_CATEGORY_IN_USE", null);
        }
        categories.delete(id);
        ruleManager.getRuleType().invalidateSources();
        ctx.status(HttpStatus.NO_CONTENT);
    }

    /** The camera is optional, but when given it must be an existing host camera. */
    private Integer validCameraId(CategoryRequestDTO body) {
        Integer cameraId = body.cameraId();
        if (cameraId == null) {
            return null;
        }
        boolean exists = deviceManager.getItem(cameraId)
                .map(item -> item.type() == DeviceItemType.CAMERA)
                .orElse(false);
        if (!exists) {
            throw new ValidationException("CAMERA_NOT_FOUND", "camera_id");
        }
        return cameraId;
    }

    private String validName(CategoryRequestDTO body, Long exceptId) {
        String name = Requests.requiredText(body.name(), "name", NAME_MAX_LENGTH, "SAMPLE_NAME_TOO_LONG");
        if (categories.nameTaken(name, exceptId)) {
            throw new ValidationException("SAMPLE_CATEGORY_NAME_EXISTS", "name");
        }
        return name;
    }
}
