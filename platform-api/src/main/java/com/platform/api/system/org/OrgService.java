package com.platform.api.system.org;

import com.platform.api.common.BusinessException;
import com.platform.api.common.IdUtils;
import com.platform.api.common.OptionItem;
import com.platform.api.security.LoginUser;
import com.platform.api.system.role.SysRole;
import com.platform.api.system.user.SysUserRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrgService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysOrgRepository orgRepository;
    private final SysUserRepository userRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final OrgScope orgScope;

    public OrgService(SysOrgRepository orgRepository,
                      SysUserRepository userRepository,
                      NamedParameterJdbcTemplate jdbc,
                      OrgScope orgScope) {
        this.orgRepository = orgRepository;
        this.userRepository = userRepository;
        this.jdbc = jdbc;
        this.orgScope = orgScope;
    }

    public List<OrgDtos.OrgItem> tree(String keywords, Integer status, LoginUser loginUser) {
        List<OrgRow> rows = loadRows();
        long base = 0L;
        OrgScope.Scope scope = orgScope.scopeOf(loginUser);
        if (scope != null) {
            OrgScope.Scope finalScope = scope;
            rows = rows.stream()
                    .filter(row -> row.id() == finalScope.rootId()
                            || (row.treePath() != null && row.treePath().startsWith(finalScope.path() + ",")))
                    .toList();
            base = rows.stream().filter(row -> row.id() == finalScope.rootId())
                    .findFirst().map(OrgRow::parentId).orElse(0L);
        }
        java.util.function.Predicate<OrgRow> predicate = row -> {
            boolean keywordMatched = keywords == null || keywords.isBlank()
                    || (row.name() != null && row.name().contains(keywords.trim()));
            boolean statusMatched = status == null || (row.status() != null && row.status().equals(status));
            return keywordMatched && statusMatched;
        };
        return build(rows, base, predicate);
    }

    public List<OptionItem> options(LoginUser loginUser) {
        List<OrgRow> rows = loadRows();
        long base = 0L;
        OrgScope.Scope scope = orgScope.scopeOf(loginUser);
        if (scope != null) {
            OrgScope.Scope finalScope = scope;
            rows = rows.stream()
                    .filter(row -> row.id() == finalScope.rootId()
                            || (row.treePath() != null && row.treePath().startsWith(finalScope.path() + ",")))
                    .toList();
            base = rows.stream().filter(row -> row.id() == finalScope.rootId())
                    .findFirst().map(OrgRow::parentId).orElse(0L);
        }
        return buildOptions(rows, base);
    }

    public OrgDtos.OrgForm form(Long id, LoginUser loginUser) {
        orgScope.check(loginUser, id);
        SysOrg org = orgRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("架构不存在"));
        return new OrgDtos.OrgForm(String.valueOf(org.getId()), org.getName(), org.getCode(),
                String.valueOf(org.getParentId()), org.getSort(), org.getStatus());
    }

    @Transactional
    public void create(OrgDtos.OrgForm form, LoginUser loginUser) {
        validate(form);
        Long parentId = resolveParentId(form.parentId());
        orgScope.check(loginUser, parentId == 0L ? null : parentId);
        ensureParentExists(parentId);

        SysOrg org = new SysOrg();
        org.setParentId(parentId);
        org.setName(form.name().trim());
        org.setCode(form.code());
        org.setSort(form.sort() == null ? 1 : form.sort());
        org.setStatus(form.status() == null ? 1 : form.status());
        LocalDateTime now = LocalDateTime.now();
        org.setCreateTime(now);
        org.setUpdateTime(now);
        SysOrg saved = orgRepository.save(org);
        updateTreePath(saved.getId());
    }

    @Transactional
    public void update(Long id, OrgDtos.OrgForm form, LoginUser loginUser) {
        orgScope.check(loginUser, id);
        SysOrg org = orgRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("架构不存在"));
        validate(form);
        Long parentId = resolveParentId(form.parentId());
        if (parentId == id) {
            throw BusinessException.badRequest("上级架构不能是自身");
        }
        orgScope.check(loginUser, parentId == 0L ? null : parentId);
        ensureParentExists(parentId);

        org.setParentId(parentId);
        org.setName(form.name().trim());
        org.setCode(form.code());
        org.setSort(form.sort() == null ? 1 : form.sort());
        org.setStatus(form.status() == null ? 1 : form.status());
        org.setUpdateTime(LocalDateTime.now());
        orgRepository.save(org);
        updateTreePath(id);
        updateChildTreePaths(id);
    }

    @Transactional
    public void delete(String ids, LoginUser loginUser) {
        List<Long> idList = IdUtils.parse(ids);
        if (idList.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的架构");
        }
        for (Long id : idList) {
            orgScope.check(loginUser, id);
        }
        if (orgRepository.countByParentIdIn(idList) > 0) {
            throw BusinessException.badRequest("请先删除子架构");
        }
        if (userRepository.countByOrgIdIn(idList) > 0) {
            throw BusinessException.badRequest("架构下存在用户，无法删除");
        }
        // sys_role_org 为复合主键关联表，保留 SQL 删除
        jdbc.update("DELETE FROM sys_role_org WHERE org_id IN (:ids)", Map.of("ids", idList));
        orgRepository.deleteAllById(idList);
    }

    private List<OrgRow> loadRows() {
        List<SysOrg> entities = new ArrayList<>();
        orgRepository.findAll().forEach(entities::add);
        entities.sort(Comparator.comparing((SysOrg org) -> org.getSort() == null ? 0 : org.getSort())
                .thenComparing(SysOrg::getId));
        return entities.stream().map(OrgService::toRow).toList();
    }

    private static OrgRow toRow(SysOrg org) {
        return new OrgRow(org.getId(), org.getParentId() == null ? 0L : org.getParentId(),
                org.getName(), org.getCode(), org.getSort(), org.getStatus(), org.getTreePath(),
                org.getCreateTime() == null ? null : TIME_FORMAT.format(org.getCreateTime()),
                org.getUpdateTime() == null ? null : TIME_FORMAT.format(org.getUpdateTime()));
    }

    /**
     * 命中节点保留祖先链，保证树形结构完整。
     */
    private List<OrgDtos.OrgItem> build(List<OrgRow> rows, long parentId,
                                        java.util.function.Predicate<OrgRow> predicate) {
        Map<Long, List<OrgRow>> grouped = new HashMap<>();
        rows.forEach(row -> grouped.computeIfAbsent(row.parentId(), key -> new ArrayList<>()).add(row));

        List<OrgDtos.OrgItem> result = new ArrayList<>();
        for (OrgRow row : grouped.getOrDefault(parentId, List.of())) {
            List<OrgDtos.OrgItem> children = build(rows, row.id(), predicate);
            if (predicate.test(row) || !children.isEmpty()) {
                result.add(toItem(row, children));
            }
        }
        result.sort(Comparator.comparing(item -> item.sort() == null ? 0 : item.sort()));
        return result;
    }

    private OrgDtos.OrgItem toItem(OrgRow row, List<OrgDtos.OrgItem> children) {
        return new OrgDtos.OrgItem(String.valueOf(row.id()), String.valueOf(row.parentId()),
                row.name(), row.sort(), row.status(), row.treePath(),
                row.createTime(), row.updateTime(), children);
    }

    private List<OptionItem> buildOptions(List<OrgRow> rows, long parentId) {
        List<OptionItem> result = new ArrayList<>();
        for (OrgRow row : rows) {
            if (row.parentId() != parentId) {
                continue;
            }
            result.add(new OptionItem(String.valueOf(row.id()), row.name(), buildOptions(rows, row.id())));
        }
        return result;
    }

    private void updateTreePath(Long id) {
        SysOrg org = orgRepository.findById(id).orElse(null);
        if (org == null) {
            return;
        }
        Long parentId = org.getParentId() == null ? 0L : org.getParentId();
        String treePath;
        if (parentId == 0L) {
            treePath = String.valueOf(id);
        } else {
            SysOrg parent = orgRepository.findById(parentId).orElse(null);
            String parentPath = parent == null || parent.getTreePath() == null
                    ? String.valueOf(parentId) : parent.getTreePath();
            treePath = parentPath + "," + id;
        }
        org.setTreePath(treePath);
        orgRepository.save(org);
    }

    private void updateChildTreePaths(Long parentId) {
        for (SysOrg child : orgRepository.findByParentId(parentId)) {
            updateTreePath(child.getId());
            updateChildTreePaths(child.getId());
        }
    }

    private Long resolveParentId(String parentId) {
        Long parsed = IdUtils.parseLong(parentId);
        return parsed == null ? 0L : parsed;
    }

    private void ensureParentExists(Long parentId) {
        if (parentId == 0L) {
            return;
        }
        if (!orgRepository.existsById(parentId)) {
            throw BusinessException.badRequest("上级架构不存在");
        }
    }

    private void validate(OrgDtos.OrgForm form) {
        if (form.name() == null || form.name().isBlank()) {
            throw BusinessException.badRequest("架构名称不能为空");
        }
    }

    private record OrgRow(long id, long parentId, String name, String code, Integer sort, Integer status,
                          String treePath, String createTime, String updateTime) {
    }
}
