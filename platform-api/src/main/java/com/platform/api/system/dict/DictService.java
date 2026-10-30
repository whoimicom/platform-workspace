package com.platform.api.system.dict;

import com.platform.api.common.BusinessException;
import com.platform.api.common.IdUtils;
import com.platform.api.common.JdbcUtil;
import com.platform.api.common.OptionItem;
import com.platform.api.common.PageResult;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DictService {

    private final NamedParameterJdbcTemplate jdbc;

    public DictService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PageResult<DictDtos.DictTypeItem> typePage(DictDtos.DictTypeQuery query) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (query.getKeywords() != null && !query.getKeywords().isBlank()) {
            where.append(" AND (d.name LIKE :keywords OR d.dict_code LIKE :keywords)");
            params.put("keywords", "%" + query.getKeywords().trim() + "%");
        }
        if (query.getStatus() != null) {
            where.append(" AND d.status = :status");
            params.put("status", query.getStatus());
        }

        long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_dict d" + where, params, Long.class);

        int pageNum = query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        int pageSize = query.getPageSize() == null ? 10 : Math.min(100, Math.max(1, query.getPageSize()));
        params.put("limit", pageSize);
        params.put("offset", (long) (pageNum - 1) * pageSize);

        List<DictDtos.DictTypeItem> list = jdbc.query(
                "SELECT d.id, d.name, d.dict_code, d.status FROM sys_dict d" + where
                        + " ORDER BY d.id ASC LIMIT :limit OFFSET :offset",
                params, (rs, rowNum) -> new DictDtos.DictTypeItem(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("name"),
                        rs.getString("dict_code"),
                        JdbcUtil.getInt(rs, "status")));

        return new PageResult<>(list, total);
    }

    public List<OptionItem> typeOptions() {
        return jdbc.query("SELECT dict_code, name FROM sys_dict WHERE status = 1 ORDER BY id ASC",
                new HashMap<>(), (rs, rowNum) -> new OptionItem(
                        rs.getString("dict_code"), rs.getString("name")));
    }

    public DictDtos.DictTypeForm typeForm(Long id) {
        List<DictDtos.DictTypeForm> rows = jdbc.query(
                "SELECT id, name, dict_code, status, remark FROM sys_dict WHERE id = :id",
                Map.of("id", id),
                (rs, rowNum) -> new DictDtos.DictTypeForm(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("name"),
                        rs.getString("dict_code"),
                        JdbcUtil.getInt(rs, "status"),
                        rs.getString("remark")));
        if (rows.isEmpty()) {
            throw BusinessException.notFound("字典不存在");
        }
        return rows.get(0);
    }

    public void createType(DictDtos.DictTypeForm form) {
        validateType(form);
        ensureCodeUnique(form.dictCode(), null);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name", form.name().trim());
        params.addValue("dictCode", form.dictCode().trim());
        params.addValue("status", form.status() == null ? 1 : form.status());
        params.addValue("remark", form.remark());
        params.addValue("createTime", LocalDateTime.now());
        params.addValue("updateTime", LocalDateTime.now());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO sys_dict (name, dict_code, status, remark, create_time, update_time) "
                        + "VALUES (:name, :dictCode, :status, :remark, :createTime, :updateTime)",
                params, keyHolder, new String[]{"id"});
    }

    public void updateType(Long id, DictDtos.DictTypeForm form) {
        validateType(form);
        ensureCodeUnique(form.dictCode(), id);

        Map<String, Object> params = new HashMap<>();
        params.put("name", form.name().trim());
        params.put("dictCode", form.dictCode().trim());
        params.put("status", form.status() == null ? 1 : form.status());
        params.put("remark", form.remark());
        params.put("updateTime", LocalDateTime.now());
        params.put("id", id);
        int updated = jdbc.update("UPDATE sys_dict SET name = :name, dict_code = :dictCode, status = :status, "
                + "remark = :remark, update_time = :updateTime WHERE id = :id", params);
        if (updated == 0) {
            throw BusinessException.notFound("字典不存在");
        }
    }

    @Transactional
    public void deleteTypes(String ids) {
        List<Long> idList = IdUtils.parse(ids);
        if (idList.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的字典");
        }
        List<String> codes = jdbc.queryForList(
                "SELECT dict_code FROM sys_dict WHERE id IN (:ids)", Map.of("ids", idList), String.class);
        if (!codes.isEmpty()) {
            jdbc.update("DELETE FROM sys_dict_item WHERE dict_code IN (:codes)", Map.of("codes", codes));
        }
        jdbc.update("DELETE FROM sys_dict WHERE id IN (:ids)", Map.of("ids", idList));
    }

    public PageResult<DictDtos.DictItem> itemPage(String dictCode, DictDtos.DictItemQuery query) {
        StringBuilder where = new StringBuilder(" WHERE dict_code = :dictCode");
        Map<String, Object> params = new HashMap<>();
        params.put("dictCode", dictCode);
        if (query.getKeywords() != null && !query.getKeywords().isBlank()) {
            where.append(" AND (label LIKE :keywords OR value LIKE :keywords)");
            params.put("keywords", "%" + query.getKeywords().trim() + "%");
        }

        long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_dict_item" + where, params, Long.class);

        int pageNum = query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        int pageSize = query.getPageSize() == null ? 10 : Math.min(100, Math.max(1, query.getPageSize()));
        params.put("limit", pageSize);
        params.put("offset", (long) (pageNum - 1) * pageSize);

        List<DictDtos.DictItem> list = jdbc.query(
                "SELECT id, dict_code, label, value, status, sort, tag_type FROM sys_dict_item" + where
                        + " ORDER BY sort ASC, id ASC LIMIT :limit OFFSET :offset",
                params, (rs, rowNum) -> new DictDtos.DictItem(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("dict_code"),
                        rs.getString("label"),
                        rs.getString("value"),
                        JdbcUtil.getInt(rs, "status"),
                        JdbcUtil.getInt(rs, "sort"),
                        rs.getString("tag_type")));

        return new PageResult<>(list, total);
    }

    public List<DictDtos.DictItemOption> itemOptions(String dictCode) {
        return jdbc.query("SELECT value, label, tag_type FROM sys_dict_item "
                        + "WHERE dict_code = :dictCode AND status = 1 ORDER BY sort ASC, id ASC",
                Map.of("dictCode", dictCode),
                (rs, rowNum) -> new DictDtos.DictItemOption(
                        rs.getString("value"), rs.getString("label"), rs.getString("tag_type")));
    }

    public DictDtos.DictItemForm itemForm(String dictCode, Long id) {
        List<DictDtos.DictItemForm> rows = jdbc.query(
                "SELECT id, dict_code, label, value, status, sort, tag_type FROM sys_dict_item "
                        + "WHERE dict_code = :dictCode AND id = :id",
                Map.of("dictCode", dictCode, "id", id),
                (rs, rowNum) -> new DictDtos.DictItemForm(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("dict_code"),
                        rs.getString("label"),
                        rs.getString("value"),
                        JdbcUtil.getInt(rs, "status"),
                        JdbcUtil.getInt(rs, "sort"),
                        rs.getString("tag_type")));
        if (rows.isEmpty()) {
            throw BusinessException.notFound("字典项不存在");
        }
        return rows.get(0);
    }

    public void createItem(String dictCode, DictDtos.DictItemForm form) {
        validateItem(form, dictCode, null);

        Map<String, Object> params = new HashMap<>();
        params.put("dictCode", dictCode);
        params.put("label", form.label().trim());
        params.put("value", form.value().trim());
        params.put("status", form.status() == null ? 1 : form.status());
        params.put("sort", form.sort() == null ? 1 : form.sort());
        params.put("tagType", form.tagType());
        params.put("createTime", LocalDateTime.now());
        params.put("updateTime", LocalDateTime.now());
        jdbc.update("INSERT INTO sys_dict_item (dict_code, label, value, status, sort, tag_type, "
                + "create_time, update_time) VALUES (:dictCode, :label, :value, :status, :sort, :tagType, "
                + ":createTime, :updateTime)", params);
    }

    public void updateItem(String dictCode, Long id, DictDtos.DictItemForm form) {
        validateItem(form, dictCode, id);

        Map<String, Object> params = new HashMap<>();
        params.put("label", form.label().trim());
        params.put("value", form.value().trim());
        params.put("status", form.status() == null ? 1 : form.status());
        params.put("sort", form.sort() == null ? 1 : form.sort());
        params.put("tagType", form.tagType());
        params.put("updateTime", LocalDateTime.now());
        params.put("dictCode", dictCode);
        params.put("id", id);
        int updated = jdbc.update("UPDATE sys_dict_item SET label = :label, value = :value, status = :status, "
                + "sort = :sort, tag_type = :tagType, update_time = :updateTime "
                + "WHERE dict_code = :dictCode AND id = :id", params);
        if (updated == 0) {
            throw BusinessException.notFound("字典项不存在");
        }
    }

    public void deleteItems(String dictCode, String ids) {
        List<Long> idList = IdUtils.parse(ids);
        if (idList.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的字典项");
        }
        jdbc.update("DELETE FROM sys_dict_item WHERE dict_code = :dictCode AND id IN (:ids)",
                Map.of("dictCode", dictCode, "ids", idList));
    }

    private void validateType(DictDtos.DictTypeForm form) {
        if (form.name() == null || form.name().isBlank()) {
            throw BusinessException.badRequest("字典名称不能为空");
        }
        if (form.dictCode() == null || form.dictCode().isBlank()) {
            throw BusinessException.badRequest("字典编码不能为空");
        }
    }

    private void validateItem(DictDtos.DictItemForm form, String dictCode, Long excludeId) {
        if (form.label() == null || form.label().isBlank()) {
            throw BusinessException.badRequest("字典项标签不能为空");
        }
        if (form.value() == null || form.value().isBlank()) {
            throw BusinessException.badRequest("字典项值不能为空");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("dictCode", dictCode);
        params.put("value", form.value().trim());
        String sql = "SELECT COUNT(*) FROM sys_dict_item WHERE dict_code = :dictCode AND value = :value";
        if (excludeId != null) {
            sql += " AND id <> :excludeId";
            params.put("excludeId", excludeId);
        }
        Long count = jdbc.queryForObject(sql, params, Long.class);
        if (count != null && count > 0) {
            throw BusinessException.badRequest("同一字典下字典值已存在");
        }
    }

    private void ensureCodeUnique(String dictCode, Long excludeId) {
        Map<String, Object> params = new HashMap<>();
        params.put("dictCode", dictCode.trim());
        String sql = "SELECT COUNT(*) FROM sys_dict WHERE dict_code = :dictCode";
        if (excludeId != null) {
            sql += " AND id <> :excludeId";
            params.put("excludeId", excludeId);
        }
        Long count = jdbc.queryForObject(sql, params, Long.class);
        if (count != null && count > 0) {
            throw BusinessException.badRequest("字典编码已存在");
        }
    }
}
