package com.example.vms.sample.db;

import com.example.vms.sample.dto.ItemDTO;
import com.google.inject.Inject;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

/**
 * Items, read together with the name of their category.
 *
 * <p>Timestamps are {@code TIMESTAMP(3)} written through {@link Timestamp#from(Instant)}, the same
 * convention as the core: the host's retention task compares them in the server's time zone.
 */
public class ItemsRepository {
    public static final String TABLE_NAME = "sample_items";
    /** Column the retention task compares against the configured retention time. */
    public static final String CREATED_AT_COLUMN = "created_at";

    private static final Table<Record> ITEMS = table(name(TABLE_NAME));
    private static final Field<Long> ID = field(name(TABLE_NAME, "id"), Long.class);
    private static final Field<String> TEXT = field(name(TABLE_NAME, "text"), String.class);
    private static final Field<Long> CATEGORY_ID = field(name(TABLE_NAME, "category_id"), Long.class);
    /** The category's camera at the moment the item was created; a later rebinding does not touch it. */
    private static final Field<Integer> CAMERA_ID = field(name(TABLE_NAME, "camera_id"), Integer.class);
    private static final Field<Integer> CREATED_BY = field(name(TABLE_NAME, "created_by"), Integer.class);
    private static final Field<Timestamp> CREATED_AT = field(name(TABLE_NAME, CREATED_AT_COLUMN), Timestamp.class);

    private final DSLContext dsl;

    @Inject
    public ItemsRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** One page of the items matching the query. */
    public List<ItemDTO> findPage(ItemsQuery query, int limit, int offset) {
        return selectItems()
                .where(condition(query))
                .orderBy(query.newestFirst() ? ID.desc() : ID.asc())
                .limit(limit)
                .offset(offset)
                .fetch(ItemsRepository::toDto);
    }

    public long count(ItemsQuery query) {
        return dsl.fetchCount(ITEMS, condition(query));
    }

    public Optional<ItemDTO> find(long id) {
        return selectItems().where(ID.eq(id)).fetchOptional(ItemsRepository::toDto);
    }

    public boolean existInCategory(long categoryId) {
        return dsl.fetchExists(ITEMS, CATEGORY_ID.eq(categoryId));
    }

    public ItemDTO insert(String text, long categoryId, Integer cameraId, int userId) {
        Long id = dsl.insertInto(ITEMS)
                .columns(TEXT, CATEGORY_ID, CAMERA_ID, CREATED_BY, CREATED_AT)
                .values(text, categoryId, cameraId, userId, Timestamp.from(Instant.now()))
                .returningResult(ID)
                .fetchOne(ID);
        return find(id).orElseThrow();
    }

    public boolean delete(long id) {
        return dsl.deleteFrom(ITEMS).where(ID.eq(id)).execute() > 0;
    }

    private static Condition condition(ItemsQuery query) {
        Condition condition = DSL.noCondition();
        if (query.categoryIds() != null && !query.categoryIds().isEmpty()) {
            condition = condition.and(CATEGORY_ID.in(query.categoryIds()));
        }
        if (query.cameraIds() != null && !query.cameraIds().isEmpty()) {
            condition = condition.and(CAMERA_ID.in(query.cameraIds()));
        }
        if (query.text() != null && !query.text().isBlank()) {
            condition = condition.and(TEXT.containsIgnoreCase(query.text().trim()));
        }
        if (query.from() != null) {
            condition = condition.and(CREATED_AT.ge(new Timestamp(query.from())));
        }
        if (query.to() != null) {
            condition = condition.and(CREATED_AT.le(new Timestamp(query.to())));
        }
        return condition;
    }

    private org.jooq.SelectJoinStep<? extends Record> selectItems() {
        return dsl.select(ID, TEXT, CATEGORY_ID, CategoriesRepository.NAME, CAMERA_ID, CREATED_BY, CREATED_AT)
                .from(ITEMS)
                .join(CategoriesRepository.CATEGORIES).on(CategoriesRepository.ID.eq(CATEGORY_ID));
    }

    private static ItemDTO toDto(Record r) {
        return new ItemDTO(
                r.get(ID),
                r.get(TEXT),
                r.get(CATEGORY_ID),
                r.get(CategoriesRepository.NAME),
                r.get(CAMERA_ID),
                r.get(CREATED_BY),
                r.get(CREATED_AT).getTime()
        );
    }
}
