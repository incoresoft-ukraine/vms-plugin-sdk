package com.example.vms.sample.db;

import com.example.vms.sample.dto.CategoryDTO;
import com.google.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

/**
 * Categories, managed in Admin Center.
 *
 * <p>jOOQ's dynamic API is used so the template builds without code generation; real plugins
 * usually generate {@code Tables.*} from the same Liquibase changelog.
 */
public class CategoriesRepository {
    public static final String TABLE_NAME = "sample_categories";

    static final Table<Record> CATEGORIES = table(name(TABLE_NAME));
    static final Field<Long> ID = field(name(TABLE_NAME, "id"), Long.class);
    static final Field<String> SOURCE_ID = field(name(TABLE_NAME, "source_id"), String.class);
    static final Field<String> NAME = field(name(TABLE_NAME, "name"), String.class);
    /** Host camera id (device item), null when the category has no camera. */
    static final Field<Integer> CAMERA_ID = field(name(TABLE_NAME, "camera_id"), Integer.class);
    static final Field<Timestamp> CREATED_AT = field(name(TABLE_NAME, "created_at"), Timestamp.class);

    private final DSLContext dsl;

    @Inject
    public CategoriesRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<CategoryDTO> findAll() {
        return dsl.select(ID, SOURCE_ID, NAME, CAMERA_ID, CREATED_AT)
                .from(CATEGORIES)
                .orderBy(NAME.asc())
                .fetch(CategoriesRepository::toDto);
    }

    public Optional<CategoryDTO> find(long id) {
        return dsl.select(ID, SOURCE_ID, NAME, CAMERA_ID, CREATED_AT)
                .from(CATEGORIES)
                .where(ID.eq(id))
                .fetchOptional(CategoriesRepository::toDto);
    }

    public boolean nameTaken(String name, Long exceptId) {
        var condition = NAME.equalIgnoreCase(name);
        if (exceptId != null) {
            condition = condition.and(ID.ne(exceptId));
        }
        return dsl.fetchExists(CATEGORIES, condition);
    }

    /** The source id is generated once and never changes: rules keep referring to it. */
    public CategoryDTO insert(String name, Integer cameraId) {
        Long id = dsl.insertInto(CATEGORIES)
                .columns(SOURCE_ID, NAME, CAMERA_ID, CREATED_AT)
                .values(UUID.randomUUID().toString(), name, cameraId, Timestamp.from(Instant.now()))
                .returningResult(ID)
                .fetchOne(ID);
        return find(id).orElseThrow();
    }

    public void update(long id, String name, Integer cameraId) {
        dsl.update(CATEGORIES).set(NAME, name).set(CAMERA_ID, cameraId).where(ID.eq(id)).execute();
    }

    public void delete(long id) {
        dsl.deleteFrom(CATEGORIES).where(ID.eq(id)).execute();
    }

    private static CategoryDTO toDto(Record r) {
        return new CategoryDTO(r.get(ID), r.get(SOURCE_ID), r.get(NAME), r.get(CAMERA_ID), r.get(CREATED_AT).getTime());
    }
}
