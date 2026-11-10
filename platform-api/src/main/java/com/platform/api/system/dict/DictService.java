package com.platform.api.system.dict;

import com.platform.api.common.BusinessException;
import com.platform.api.common.IdUtils;
import com.platform.api.common.JdbcUtil;
import com.platform.api.common.OptionItem;
import org.springframework.data.domain.Page;
import com.platform.api.common.query.DynamicQueryExecutor;
import com.platform.api.common.query.RawCondition;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DictService {

    private static final RowMapper<DictDtos.DictTypeItem> DICT_TYPE_MAPPER = (rs, rowNum) -> new DictDtos.DictTypeItem(
            String.valueOf(rs.getLong("id")),
            rs.getString("name"),
            rs.getString("dict_code"),
            rs.getString("type"),
            JdbcUtil.getInt(rs, "status"));

    private static final RowMapper<DictDtos.DictItem> DICT_ITEM_MAPPER = (rs, rowNum) -> new DictDtos.DictItem(
            String.valueOf(rs.getLong("id")),
            rs.getString("dict_code"),
            rs.getString("label"),
            rs.getString("value"),
            rs.getString("desc"),
            JdbcUtil.getInt(rs, "status"),
            JdbcUtil.getInt(rs, "sort"),
            rs.getString("tag_type"));

    private static final String DICT_TYPE_BASE_SELECT = "SELECT d.id, d.name, d.dict_code, d.type, d.status FROM sys_dict d";

    private static final String DICT_ITEM_BASE_SELECT =
            "SELECT id, dict_code, label, value, item_desc AS `desc`, status, sort, tag_type FROM sys_dict_item";

    private final NamedParameterJdbcTemplate jdbc;
    private final DynamicQueryExecutor executor;
    private final SysDictRepository dictRepository;
    private final SysDictItemRepository itemRepository;

    public DictService(NamedParameterJdbcTemplate jdbc,
                       DynamicQueryExecutor executor,
                       SysDictRepository dictRepository,
                       SysDictItemRepository itemRepository) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.dictRepository = dictRepository;
        this.itemRepository = itemRepository;
    }

    public Page<DictDtos.DictTypeItem> typePage(DictDtos.DictTypeQuery query) {
        List<RawCondition> extras = new ArrayList<>();
        if (query.getKeywords() != null && !query.getKeywords().isBlank()) {
            extras.add(RawCondition.of("(d.name LIKE :keywords OR d.dict_code LIKE :keywords)",
                    Map.of("keywords", "%" + query.getKeywords().trim() + "%")));
        }
        return executor.page(DICT_TYPE_BASE_SELECT, query, DICT_TYPE_MAPPER, extras.toArray(RawCondition[]::new));
    }

    public List<OptionItem> typeOptions() {
        List<SysDict> dicts = new ArrayList<>();
        dictRepository.findAll().forEach(dicts::add);
        dicts.sort(java.util.Comparator.comparing(SysDict::getId));
        return dicts.stream()
                .filter(dict -> dict.getStatus() != null && dict.getStatus() == 1)
                .map(dict -> new OptionItem(dict.getDictCode(), dict.getName()))
                .toList();
    }

    public DictDtos.DictTypeForm typeForm(Long id) {
        SysDict dict = dictRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("字典不存在"));
        return new DictDtos.DictTypeForm(String.valueOf(dict.getId()), dict.getName(),
                dict.getDictCode(), dict.getType(), dict.getStatus(), dict.getRemark());
    }

    public void createType(DictDtos.DictTypeForm form) {
        validateType(form);
        ensureCodeUnique(form.dictCode(), null);

        SysDict dict = new SysDict();
        dict.setName(form.name().trim());
        dict.setDictCode(form.dictCode().trim());
        dict.setType(normalizeType(form.type()));
        dict.setStatus(form.status() == null ? 1 : form.status());
        dict.setRemark(form.remark());
        LocalDateTime now = LocalDateTime.now();
        dict.setCreateTime(now);
        dict.setUpdateTime(now);
        dictRepository.save(dict);
    }

    public void updateType(Long id, DictDtos.DictTypeForm form) {
        validateType(form);
        ensureCodeUnique(form.dictCode(), id);

        SysDict dict = dictRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("字典不存在"));
        dict.setName(form.name().trim());
        dict.setDictCode(form.dictCode().trim());
        dict.setType(normalizeType(form.type()));
        dict.setStatus(form.status() == null ? 1 : form.status());
        dict.setRemark(form.remark());
        dict.setUpdateTime(LocalDateTime.now());
        dictRepository.save(dict);
    }

    @Transactional
    public void deleteTypes(String ids) {
        List<Long> idList = IdUtils.parse(ids);
        if (idList.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的字典");
        }
        List<String> codes = new ArrayList<>();
        dictRepository.findAllById(idList).forEach(dict -> codes.add(dict.getDictCode()));
        if (!codes.isEmpty()) {
            jdbc.update("DELETE FROM sys_dict_item WHERE dict_code IN (:codes)", Map.of("codes", codes));
        }
        dictRepository.deleteAllById(idList);
    }

    public Page<DictDtos.DictItem> itemPage(String dictCode, DictDtos.DictItemQuery query) {
        List<RawCondition> extras = new ArrayList<>();
        extras.add(RawCondition.of("dict_code = :dictCode", Map.of("dictCode", dictCode)));
        if (query.getKeywords() != null && !query.getKeywords().isBlank()) {
            extras.add(RawCondition.of("(label LIKE :keywords OR value LIKE :keywords)",
                    Map.of("keywords", "%" + query.getKeywords().trim() + "%")));
        }
        return executor.page(DICT_ITEM_BASE_SELECT, query, DICT_ITEM_MAPPER, extras.toArray(RawCondition[]::new));
    }

    public List<DictDtos.DictItemOption> itemOptions(String dictCode) {
        List<SysDictItem> items = new ArrayList<>(itemRepository.findByDictCode(dictCode));
        items.sort(java.util.Comparator
                .comparing((SysDictItem item) -> item.getSort() == null ? 0 : item.getSort())
                .thenComparing(SysDictItem::getId));
        return items.stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == 1)
                .map(item -> new DictDtos.DictItemOption(item.getValue(), item.getLabel(), item.getTagType()))
                .toList();
    }

    public DictDtos.DictItemForm itemForm(String dictCode, Long id) {
        SysDictItem item = itemRepository.findByDictCodeAndId(dictCode, id)
                .orElseThrow(() -> BusinessException.notFound("字典项不存在"));
        return new DictDtos.DictItemForm(String.valueOf(item.getId()), item.getDictCode(),
                item.getLabel(), item.getValue(), item.getItemDesc(),
                item.getStatus(), item.getSort(), item.getTagType());
    }

    public void createItem(String dictCode, DictDtos.DictItemForm form) {
        assertMutable(dictCode);
        validateItem(form, dictCode, null);

        SysDictItem item = new SysDictItem();
        item.setDictCode(dictCode);
        item.setLabel(form.label().trim());
        item.setValue(form.value().trim());
        item.setItemDesc(form.desc() == null ? null : form.desc().trim());
        item.setStatus(form.status() == null ? 1 : form.status());
        item.setSort(form.sort() == null ? 1 : form.sort());
        item.setTagType(form.tagType());
        LocalDateTime now = LocalDateTime.now();
        item.setCreateTime(now);
        item.setUpdateTime(now);
        itemRepository.save(item);
    }

    public void updateItem(String dictCode, Long id, DictDtos.DictItemForm form) {
        assertMutable(dictCode);
        validateItem(form, dictCode, id);

        SysDictItem item = itemRepository.findByDictCodeAndId(dictCode, id)
                .orElseThrow(() -> BusinessException.notFound("字典项不存在"));
        item.setLabel(form.label().trim());
        item.setValue(form.value().trim());
        item.setItemDesc(form.desc() == null ? null : form.desc().trim());
        item.setStatus(form.status() == null ? 1 : form.status());
        item.setSort(form.sort() == null ? 1 : form.sort());
        item.setTagType(form.tagType());
        item.setUpdateTime(LocalDateTime.now());
        itemRepository.save(item);
    }

    public void deleteItems(String dictCode, String ids) {
        assertMutable(dictCode);
        List<Long> idList = IdUtils.parse(ids);
        if (idList.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的字典项");
        }
        // 仅删除属于当前字典的项，防止跨字典误删
        List<SysDictItem> ownedItems = new ArrayList<>();
        itemRepository.findAllById(idList).forEach(ownedItems::add);
        List<Long> owned = ownedItems.stream()
                .filter(item -> dictCode.equals(item.getDictCode()))
                .map(SysDictItem::getId)
                .toList();
        itemRepository.deleteAllById(owned);
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
        var existing = itemRepository.findByDictCodeAndValue(dictCode, form.value().trim());
        if (existing.isPresent()
                && (excludeId == null || !existing.get().getId().equals(excludeId))) {
            throw BusinessException.badRequest("同一字典下字典值已存在");
        }
    }

    private void ensureCodeUnique(String dictCode, Long excludeId) {
        var existing = dictRepository.findByDictCode(dictCode.trim());
        if (existing.isPresent()
                && (excludeId == null || !existing.get().getId().equals(excludeId))) {
            throw BusinessException.badRequest("字典编码已存在");
        }
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "DYNAMIC";
        }
        String normalized = type.trim().toUpperCase();
        if (!normalized.equals("STATIC") && !normalized.equals("DYNAMIC")) {
            throw BusinessException.badRequest("字典类型仅支持 STATIC / DYNAMIC");
        }
        return normalized;
    }

    private void assertMutable(String dictCode) {
        var dict = dictRepository.findByDictCode(dictCode);
        if (dict.isPresent() && "STATIC".equalsIgnoreCase(dict.get().getType())) {
            throw BusinessException.forbidden("静态字典不允许修改，请在前端源码中维护");
        }
    }
}
