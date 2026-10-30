package com.platform.api.system.user;

import com.platform.api.common.BusinessException;
import com.platform.api.common.IdUtils;
import com.platform.api.common.JdbcUtil;
import com.platform.api.common.OptionItem;
import com.platform.api.common.PageResult;
import com.platform.api.config.AppProperties;
import com.platform.api.security.LoginUser;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "u.id",
            "username", "u.username",
            "createTime", "u.create_time",
            "updateTime", "u.update_time");

    private static final String BASE_SELECT = """
            SELECT u.id AS id,
                   u.avatar AS avatar,
                   DATE_FORMAT(u.create_time, '%Y-%m-%d %H:%i:%s') AS create_time,
                   d.name AS dept_name,
                   u.email AS email,
                   u.gender AS gender,
                   u.mobile AS mobile,
                   u.nickname AS nickname,
                   (SELECT GROUP_CONCAT(r.name ORDER BY r.sort SEPARATOR ',')
                      FROM sys_role r
                      JOIN sys_user_role ur ON ur.role_id = r.id
                     WHERE ur.user_id = u.id) AS role_names,
                   u.status AS status,
                   u.username AS username
              FROM sys_user u
              LEFT JOIN sys_dept d ON d.id = u.dept_id
            """;

    private final SysUserRepository userRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public UserService(SysUserRepository userRepository,
                       NamedParameterJdbcTemplate jdbc,
                       PasswordEncoder passwordEncoder,
                       AppProperties properties) {
        this.userRepository = userRepository;
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    public UserDtos.UserInfo me(LoginUser loginUser) {
        if (loginUser == null) {
            throw BusinessException.unauthorized("A0230", "访问令牌无效或已过期");
        }
        Long userId = loginUser.userId();
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        Map<String, Object> params = Map.of("userId", userId);
        List<String> deptNames = jdbc.query(
                "SELECT d.name FROM sys_user u LEFT JOIN sys_dept d ON d.id = u.dept_id WHERE u.id = :userId",
                params, (rs, rowNum) -> rs.getString("name"));

        List<String[]> rolePairs = jdbc.query(
                "SELECT r.code, r.name FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id "
                        + "WHERE ur.user_id = :userId AND r.status = 1 ORDER BY r.sort ASC",
                params, (rs, rowNum) -> new String[]{rs.getString("code"), rs.getString("name")});

        List<String> permissions = jdbc.query(
                "SELECT DISTINCT m.perm FROM sys_menu m "
                        + "JOIN sys_role_menu rm ON rm.menu_id = m.id "
                        + "JOIN sys_role r ON r.id = rm.role_id "
                        + "JOIN sys_user_role ur ON ur.role_id = r.id "
                        + "WHERE ur.user_id = :userId AND r.status = 1 "
                        + "AND m.perm IS NOT NULL AND m.perm <> ''",
                params, (rs, rowNum) -> rs.getString("perm"));

        List<String> roles = rolePairs.stream().map(pair -> pair[0]).toList();
        List<String> roleNames = rolePairs.stream().map(pair -> pair[1]).toList();
        Set<String> perms = new LinkedHashSet<>(permissions);
        if (roles.contains("ROOT")) {
            perms.add("*:*:*");
        }

        return new UserDtos.UserInfo(String.valueOf(userId), user.getUsername(), user.getNickname(),
                user.getAvatar(), user.getGender(), deptNames.isEmpty() ? null : deptNames.get(0),
                roles, roleNames, new ArrayList<>(perms), false);
    }

    public PageResult<UserDtos.UserItem> page(UserDtos.UserQuery query) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();

        String keywords = query.getKeywords();
        if (keywords != null && !keywords.isBlank()) {
            where.append(" AND (u.username LIKE :keywords OR u.nickname LIKE :keywords OR u.mobile LIKE :keywords)");
            params.put("keywords", "%" + keywords.trim() + "%");
        }
        if (query.getStatus() != null) {
            where.append(" AND u.status = :status");
            params.put("status", query.getStatus());
        }
        Long deptId = IdUtils.parseLong(query.getDeptId());
        if (deptId != null) {
            where.append(" AND u.dept_id = :deptId");
            params.put("deptId", deptId);
        }
        List<Long> roleIds = new ArrayList<>();
        if (query.getRoleIds() != null) {
            for (String roleId : query.getRoleIds()) {
                Long id = IdUtils.parseLong(roleId);
                if (id != null) {
                    roleIds.add(id);
                }
            }
        }
        if (!roleIds.isEmpty()) {
            where.append(" AND EXISTS (SELECT 1 FROM sys_user_role ur "
                    + "WHERE ur.user_id = u.id AND ur.role_id IN (:roleIds))");
            params.put("roleIds", roleIds);
        }
        if (query.getCreateTime() != null && query.getCreateTime().size() >= 2) {
            String start = normalizeDate(query.getCreateTime().get(0));
            String end = normalizeDate(query.getCreateTime().get(1));
            if (start != null && end != null) {
                where.append(" AND u.create_time >= :startTime AND u.create_time <= :endTime");
                params.put("startTime", start + " 00:00:00");
                params.put("endTime", end + " 23:59:59");
            }
        }

        long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user u" + where, params, Long.class);

        int pageNum = query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        int pageSize = query.getPageSize() == null ? 10 : Math.min(100, Math.max(1, query.getPageSize()));
        params.put("limit", pageSize);
        params.put("offset", (long) (pageNum - 1) * pageSize);

        List<UserDtos.UserItem> list = jdbc.query(
                BASE_SELECT + where + " ORDER BY " + resolveOrderBy(query.getSort())
                        + " LIMIT :limit OFFSET :offset",
                params, (rs, rowNum) -> new UserDtos.UserItem(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("avatar"),
                        rs.getString("create_time"),
                        rs.getString("dept_name"),
                        rs.getString("email"),
                        JdbcUtil.getInt(rs, "gender"),
                        rs.getString("mobile"),
                        rs.getString("nickname"),
                        rs.getString("role_names"),
                        JdbcUtil.getInt(rs, "status"),
                        rs.getString("username")));

        return new PageResult<>(list, total);
    }

    public UserDtos.UserForm form(Long id) {
        List<FormRow> rows = jdbc.query(
                "SELECT id, avatar, dept_id, email, gender, mobile, nickname, status, username "
                        + "FROM sys_user WHERE id = :id",
                Map.of("id", id),
                (rs, rowNum) -> new FormRow(rs.getLong("id"), rs.getString("avatar"),
                        JdbcUtil.getLong(rs, "dept_id"), rs.getString("email"),
                        JdbcUtil.getInt(rs, "gender"), rs.getString("mobile"),
                        rs.getString("nickname"), JdbcUtil.getInt(rs, "status"),
                        rs.getString("username")));
        if (rows.isEmpty()) {
            throw BusinessException.notFound("用户不存在");
        }
        FormRow row = rows.get(0);
        List<String> roleIds = jdbc.queryForList(
                        "SELECT role_id FROM sys_user_role WHERE user_id = :id", Map.of("id", id), Long.class)
                .stream().map(String::valueOf).toList();
        return new UserDtos.UserForm(String.valueOf(row.id()), row.avatar(),
                row.deptId() == null ? null : String.valueOf(row.deptId()),
                row.email(), row.gender(), row.mobile(), row.nickname(), roleIds, row.status(), row.username());
    }

    public void create(UserDtos.UserForm form) {
        if (form.username() == null || form.username().isBlank()) {
            throw BusinessException.badRequest("用户名不能为空");
        }
        String username = form.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw BusinessException.badRequest("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(properties.getDefaultPassword()));
        user.setNickname(form.nickname());
        user.setAvatar(form.avatar());
        user.setGender(form.gender() == null ? 0 : form.gender());
        user.setMobile(form.mobile());
        user.setEmail(form.email());
        user.setStatus(form.status() == null ? 1 : form.status());
        user.setDeptId(IdUtils.parseLong(form.deptId()));
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        SysUser saved = userRepository.save(user);
        replaceRoles(saved.getId(), form.roleIds());
    }

    public void update(Long id, UserDtos.UserForm form) {
        SysUser existing = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        String username = existing.getUsername();
        if (form.username() != null && !form.username().isBlank()) {
            String candidate = form.username().trim();
            if (!candidate.equals(username)) {
                if (userRepository.existsByUsername(candidate)) {
                    throw BusinessException.badRequest("用户名已存在");
                }
                username = candidate;
            }
        }

        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("nickname", form.nickname());
        params.put("avatar", form.avatar());
        params.put("gender", form.gender() == null ? existing.getGender() : form.gender());
        params.put("mobile", form.mobile());
        params.put("email", form.email());
        params.put("status", form.status() == null ? existing.getStatus() : form.status());
        params.put("deptId", IdUtils.parseLong(form.deptId()));
        params.put("updateTime", LocalDateTime.now());
        params.put("id", id);

        jdbc.update("UPDATE sys_user SET username = :username, nickname = :nickname, avatar = :avatar, "
                + "gender = :gender, mobile = :mobile, email = :email, status = :status, "
                + "dept_id = :deptId, update_time = :updateTime WHERE id = :id", params);
        replaceRoles(id, form.roleIds());
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || status != 0 && status != 1) {
            throw BusinessException.badRequest("用户状态不合法");
        }
        if (!userRepository.existsById(id)) {
            throw BusinessException.notFound("用户不存在");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("updateTime", LocalDateTime.now());
        params.put("id", id);
        jdbc.update("UPDATE sys_user SET status = :status, update_time = :updateTime WHERE id = :id", params);
    }

    public void resetPassword(Long id, String password) {
        if (password == null || password.isBlank() || password.trim().length() < 6) {
            throw BusinessException.badRequest("密码长度不能少于6位");
        }
        if (!userRepository.existsById(id)) {
            throw BusinessException.notFound("用户不存在");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("password", passwordEncoder.encode(password.trim()));
        params.put("updateTime", LocalDateTime.now());
        params.put("id", id);
        jdbc.update("UPDATE sys_user SET password = :password, update_time = :updateTime WHERE id = :id", params);
    }

    public void delete(String ids) {
        List<Long> idList = IdUtils.parse(ids);
        if (idList.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的用户");
        }
        Map<String, Object> params = Map.of("ids", idList);
        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (:ids)", params);
        jdbc.update("DELETE FROM sys_user WHERE id IN (:ids)", params);
    }

    public List<OptionItem> options() {
        return jdbc.query("SELECT id, nickname FROM sys_user ORDER BY id ASC",
                new HashMap<>(), (rs, rowNum) -> new OptionItem(
                        String.valueOf(rs.getLong("id")), rs.getString("nickname")));
    }

    private void replaceRoles(Long userId, List<String> roleIds) {
        jdbc.update("DELETE FROM sys_user_role WHERE user_id = :userId", Map.of("userId", userId));
        if (roleIds == null) {
            return;
        }
        for (String roleId : roleIds) {
            Long parsed = IdUtils.parseLong(roleId);
            if (parsed != null) {
                jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (:userId, :roleId)",
                        Map.of("userId", userId, "roleId", parsed));
            }
        }
    }

    private String resolveOrderBy(List<String> sort) {
        String defaultOrder = "u.create_time DESC, u.id DESC";
        if (sort == null || sort.isEmpty()) {
            return defaultOrder;
        }
        List<String> parts = new ArrayList<>();
        for (String expression : sort) {
            if (expression == null) {
                continue;
            }
            String[] pieces = expression.split(",");
            if (pieces.length != 2) {
                continue;
            }
            String column = SORT_COLUMNS.get(pieces[0].trim());
            if (column == null) {
                continue;
            }
            String direction = "desc".equalsIgnoreCase(pieces[1].trim()) ? "DESC" : "ASC";
            parts.add(column + " " + direction);
        }
        return parts.isEmpty() ? defaultOrder : String.join(", ", parts);
    }

    private String normalizeDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() >= 10 ? trimmed.substring(0, 10) : null;
    }

    private record FormRow(Long id, String avatar, Long deptId, String email, Integer gender,
                           String mobile, String nickname, Integer status, String username) {
    }
}
