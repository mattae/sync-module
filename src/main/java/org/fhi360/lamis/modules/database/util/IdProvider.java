package org.fhi360.lamis.modules.database.util;

import lombok.extern.slf4j.Slf4j;
import org.lamisplus.modules.base.config.ContextProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class IdProvider {
    private static final JdbcTemplate jdbcTemplate = ContextProvider.getBean(JdbcTemplate.class);
    private static Map<String, AtomicLong> pks = new HashMap<>();

    public static Object columnFromPk(String table, String column, String pk) {
        if (pk == null) {
            return null;
        }
        return jdbcTemplate.queryForObject(String.format("select %s from %s where id = %s", column, table, pk), Object.class);
    }

    public static Object uuidFromPk(String table, String pk) {
        return columnFromPk(table, "uuid", pk);
    }

    public static Long pkFromColumn(String table, String column, String value, boolean nullable) {
        List<Long> ids;
        try {
            ids = jdbcTemplate.queryForList(String.format("select id from %s where %s = %s", table, column, value), Long.class);
        } catch (Exception e) {
            ids = jdbcTemplate.queryForList(String.format("select id from %s where %s = '%s'", table, column, value), Long.class);
        }
        return getId(ids, table, nullable);
    }

    public static Long pkFromUuid(String table, String uuid) {
        return pkFromColumn(table, "uuid", uuid, false);
    }

    public static Long relatedPkFromUuid(String table, String uuid) {
        return pkFromColumn(table, "uuid", uuid, true);
    }

    public static Long idFromModuleName(String name) {
        List<Long> ids = jdbcTemplate.queryForList("select id from module where name = ?", Long.class, name);
        return getId(ids, "module", false);
    }

    public static Long moduleDependencyId(String module, String dependency) {
        List<Long> ids = jdbcTemplate.queryForList("select id from module_dependencies where module_id = (select id from " +
            "module where name = ?) and dependency_id = (select id from module where name = ?)", Long.class, module, dependency);
        return getId(ids, "module_dependencies", false);
    }

    public static Long menuId(String name, String level, String module) {
        List<Long> ids = jdbcTemplate.queryForList("select id from menu where name = ? and type = ? and module_id = " +
            "(select id from module where name = ?)", Long.class, name, level, module);
        return getId(ids, "menu", false);
    }

    public static Long menuAuthorityId(String authority, String module) {
        List<Long> ids = jdbcTemplate.queryForList("select id from menu_authorities where authorities = ? and menu_id = " +
            "(select id from menu where name = ?)", Long.class, authority, module);
        return getId(ids, "menu_authorities", false);
    }

    public static Long artifactId(String module) {
        List<Long> ids = jdbcTemplate.queryForList("select id from module_artifact where module_id = " +
            "(select id from module where name = ?)", Long.class, module);
        return getId(ids, "module_artifact", false);
    }

    public static Long formId(String name, String module) {
        List<Long> ids = jdbcTemplate.queryForList("select id from form where name = ? and module_id = " +
            "(select id from module where name = ?)", Long.class, name, module);
        return getId(ids, "form", false);
    }

    public static Long webModuleId(String name, String module) {
        List<Long> ids = jdbcTemplate.queryForList("select id from web_module where name = ? and module_id = " +
            "(select id from module where name = ?)", Long.class, name, module);
        return getId(ids, "web_module", false);
    }

    public static Long webModuleAuthorityId(String authority, String module) {
        List<Long> ids = jdbcTemplate.queryForList("select id from web_module_authorities where authorities = ? and web_module_id = " +
            "(select id from web_module where name = ?)", Long.class, authority, module);
        return getId(ids, "menu", false);
    }

    private static Long getId(List<Long> ids, String table, boolean nullable) {
        if (ids.size() > 0) {
            return ids.get(0);
        }
        if (nullable) {
            return null;
        }
        AtomicLong atomicLong = pks.get(table);
        if (atomicLong == null) {
            final String query = "select coalesce(max(id), 0) + 1 from %s";
            Long id = jdbcTemplate.queryForObject(String.format(query, table), Long.class);
            pks.put(table, new AtomicLong(id));
            atomicLong = new AtomicLong(id);
        }
        return atomicLong.getAndIncrement();
    }

    static {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    pks.clear();
                }catch (Exception ignored) {}
            }
        }, 0, 300000);
    }
}
