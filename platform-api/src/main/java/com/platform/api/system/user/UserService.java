package com.platform.api.system.user;

import com.platform.api.common.BusinessException;
import com.platform.api.common.IdUtils;
import com.platform.api.common.JdbcUtil;
import com.platform.api.common.OptionItem;
import org.springframework.data.domain.Page;
import com.platform.api.common.query.DynamicQueryExecutor;
import com.platform.api.common.query.RawCondition;
import com.platform.api.config.AppProperties;
import com.platform.api.security.LoginUser;
import com.platform.api.system.org.OrgScope;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UserService {

    /** 用户名规则：字母开头，仅支持字母、数字、-、_，最长 64 位（与 sys_user.username 长度一致） */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0,63}$");

    /** 手机号规则：11 位大陆手机号 */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private static final RowMapper<UserDtos.UserItem> USER_ITEM_MAPPER = (rs, rowNum) -> new UserDtos.UserItem(
            String.valueOf(rs.getLong("id")),
            rs.getString("avatar"),
            rs.getString("create_date"),
            rs.getString("create_time"),
            rs.getString("org_name"),
            rs.getString("email"),
            JdbcUtil.getInt(rs, "gender"),
            rs.getString("mobile"),
            rs.getString("nickname"),
            rs.getString("role_names"),
            JdbcUtil.getInt(rs, "status"),
            rs.getString("username"));

    private static final String BASE_SELECT = """
            SELECT u.id AS id,
                   u.avatar AS avatar,
                   u.create_date AS create_date,
                   u.create_time AS create_time,
                   o.name AS org_name,
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
              LEFT JOIN sys_org o ON o.id = u.org_id
            """;

    private final SysUserRepository userRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final DynamicQueryExecutor executor;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;
    private final OrgScope orgScope;

    public UserService(SysUserRepository userRepository,
                       NamedParameterJdbcTemplate jdbc,
                       DynamicQueryExecutor executor,
                       PasswordEncoder passwordEncoder,
                       AppProperties properties,
                       OrgScope orgScope) {
        this.userRepository = userRepository;
        this.jdbc = jdbc;
        this.executor = executor;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.orgScope = orgScope;
    }

    public UserDtos.UserInfo me(LoginUser loginUser) {
        if (loginUser == null) {
            throw BusinessException.unauthorized("访问令牌无效或已过期");
        }
        Long userId = loginUser.userId();
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        Map<String, Object> params = Map.of("userId", userId);
        List<String> orgNames = jdbc.query(
                "SELECT o.name FROM sys_user u LEFT JOIN sys_org o ON o.id = u.org_id WHERE u.id = :userId",
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

        // 追加权限（用户级）并入按钮/接口权限
        List<String> extraPermissions = jdbc.query(
                "SELECT DISTINCT m.perm FROM sys_menu m "
                        + "JOIN sys_user_menu um ON um.menu_id = m.id "
                        + "WHERE um.user_id = :userId "
                        + "AND m.perm IS NOT NULL AND m.perm <> ''",
                params, (rs, rowNum) -> rs.getString("perm"));

        List<String> roles = rolePairs.stream().map(pair -> pair[0]).toList();
        List<String> roleNames = rolePairs.stream().map(pair -> pair[1]).toList();
        Set<String> perms = new LinkedHashSet<>(permissions);
        perms.addAll(extraPermissions);
        if (roles.contains("ROOT")) {
            perms.add("*:*:*");
        }

        return new UserDtos.UserInfo(String.valueOf(userId), user.getUsername(), user.getNickname(),
                user.getAvatar(), user.getGender(), orgNames.isEmpty() ? null : orgNames.get(0),
                roles, roleNames, new ArrayList<>(perms), false);
    }

    public Page<UserDtos.UserItem> page(UserDtos.UserQuery query, LoginUser loginUser) {
        List<RawCondition> extras = new ArrayList<>();
        OrgScope.Scope scope = orgScope.scopeOf(loginUser);
        if (scope != null) {
            extras.add(RawCondition.of(
                    "EXISTS (SELECT 1 FROM sys_org so WHERE so.id = u.org_id AND "
                            + orgScope.subtreeCondition("so") + ")",
                    orgScope.params(scope)));
        }
        String keywords = query.getKeywords();
        if (keywords != null && !keywords.isBlank()) {
            extras.add(RawCondition.of(
                    "(u.username LIKE :keywords OR u.nickname LIKE :keywords OR u.mobile LIKE :keywords)",
                    Map.of("keywords", "%" + keywords.trim() + "%")));
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
            extras.add(RawCondition.of(
                    "EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id IN (:roleIds))",
                    Map.of("roleIds", roleIds)));
        }
        return executor.page(BASE_SELECT, query, USER_ITEM_MAPPER, extras.toArray(RawCondition[]::new));
    }

    public UserDtos.UserForm form(Long id, LoginUser loginUser) {
        orgScope.checkUser(loginUser, id);
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        List<String> roleIds = jdbc.queryForList(
                        "SELECT role_id FROM sys_user_role WHERE user_id = :id", Map.of("id", id), Long.class)
                .stream().map(String::valueOf).toList();
        return new UserDtos.UserForm(String.valueOf(user.getId()), user.getAvatar(),
                user.getOrgId() == null ? null : String.valueOf(user.getOrgId()),
                user.getEmail(), user.getGender(), user.getMobile(), user.getNickname(),
                roleIds, user.getStatus(), user.getUsername());
    }

    public void create(UserDtos.UserForm form, LoginUser loginUser) {
        String username = validateUsername(form.username());
        String mobile = validateMobile(form.mobile(), null);
        if (userRepository.existsByUsername(username)) {
            throw BusinessException.badRequest("用户名已存在");
        }
        Long orgId = IdUtils.parseLong(form.orgId());
        checkOrgWritable(loginUser, orgId);

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(properties.getDefaultPassword()));
        user.setNickname(form.nickname());
        user.setAvatar(form.avatar());
        user.setGender(form.gender() == null ? 0 : form.gender());
        user.setMobile(mobile);
        user.setEmail(form.email());
        user.setStatus(form.status() == null ? 1 : form.status());
        user.setOrgId(orgId);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateDate(now.toLocalDate());
        user.setCreateTime(now.toLocalTime().withNano(0));
        user.setUpdateDatetime(now);

        SysUser saved = userRepository.save(user);
        replaceRoles(saved.getId(), form.roleIds());
    }

    /**
     * 用户名校验：非空、字母开头，仅支持字母、数字、-、_。返回 trim 后的用户名。
     */
    private String validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw BusinessException.badRequest("用户名不能为空");
        }
        String trimmed = username.trim();
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            throw BusinessException.badRequest("用户名须以字母开头，只能包含字母、数字、-、_，长度不超过64位");
        }
        return trimmed;
    }

    /**
     * 手机号校验：必输、格式正确、全局唯一（excludeUserId 用于编辑时排除自身）。返回 trim 后的手机号。
     */
    private String validateMobile(String mobile, Long excludeUserId) {
        if (mobile == null || mobile.isBlank()) {
            throw BusinessException.badRequest("手机号码不能为空");
        }
        String trimmed = mobile.trim();
        if (!MOBILE_PATTERN.matcher(trimmed).matches()) {
            throw BusinessException.badRequest("手机号码格式不正确");
        }
        var existing = userRepository.findByMobile(trimmed);
        if (existing.isPresent() && !existing.get().getId().equals(excludeUserId)) {
            throw BusinessException.badRequest("手机号码已存在");
        }
        return trimmed;
    }

    public void update(Long id, UserDtos.UserForm form, LoginUser loginUser) {
        orgScope.checkUser(loginUser, id);
        SysUser existing = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        Long orgId = IdUtils.parseLong(form.orgId());
        checkOrgWritable(loginUser, orgId);

        String username = existing.getUsername();
        if (form.username() != null && !form.username().isBlank()) {
            String candidate = form.username().trim();
            if (!candidate.equals(username)) {
                validateUsername(candidate);
                if (userRepository.existsByUsername(candidate)) {
                    throw BusinessException.badRequest("用户名已存在");
                }
                username = candidate;
            }
        }
        String mobile = validateMobile(form.mobile(), id);

        existing.setUsername(username);
        existing.setNickname(form.nickname());
        existing.setAvatar(form.avatar());
        existing.setGender(form.gender() == null ? existing.getGender() : form.gender());
        existing.setMobile(mobile);
        existing.setEmail(form.email());
        existing.setStatus(form.status() == null ? existing.getStatus() : form.status());
        existing.setOrgId(orgId);
        existing.setUpdateDatetime(LocalDateTime.now());
        userRepository.save(existing);
        replaceRoles(id, form.roleIds());
    }

    public void updateStatus(Long id, Integer status, LoginUser loginUser) {
        orgScope.checkUser(loginUser, id);
        if (status == null || status != 0 && status != 1) {
            throw BusinessException.badRequest("用户状态不合法");
        }
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        user.setStatus(status);
        user.setUpdateDatetime(LocalDateTime.now());
        userRepository.save(user);
    }

    public void resetPassword(Long id, String password, LoginUser loginUser) {
        orgScope.checkUser(loginUser, id);
        if (password == null || password.isBlank() || password.trim().length() < 6) {
            throw BusinessException.badRequest("密码长度不能少于6位");
        }
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        user.setPassword(passwordEncoder.encode(password.trim()));
        user.setUpdateDatetime(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 用户追加权限：角色定义菜单集 + 已追加菜单集。
     */
    public UserDtos.UserPerms perms(Long id, LoginUser loginUser) {
        orgScope.checkUser(loginUser, id);
        Map<String, Object> params = Map.of("userId", id);
        List<String> roleMenuIds = jdbc.queryForList(
                "SELECT DISTINCT rm.menu_id FROM sys_role_menu rm "
                        + "JOIN sys_role r ON r.id = rm.role_id "
                        + "JOIN sys_user_role ur ON ur.role_id = r.id "
                        + "WHERE ur.user_id = :userId AND r.status = 1",
                params, Long.class).stream().map(String::valueOf).toList();
        List<String> extraMenuIds = jdbc.queryForList(
                "SELECT menu_id FROM sys_user_menu WHERE user_id = :userId", params, Long.class)
                .stream().map(String::valueOf).toList();
        return new UserDtos.UserPerms(roleMenuIds, extraMenuIds);
    }

    /**
     * 保存用户追加权限：剔除角色已含项，非 ROOT 仅能分配自己可见范围内的菜单。
     */
    @Transactional
    public void updatePerms(Long id, UserDtos.UserPermsForm form, LoginUser loginUser) {
        orgScope.checkUser(loginUser, id);
        if (!userRepository.existsById(id)) {
            throw BusinessException.notFound("用户不存在");
        }
        Set<Long> roleMenuIds = new LinkedHashSet<>(jdbc.queryForList(
                "SELECT DISTINCT rm.menu_id FROM sys_role_menu rm "
                        + "JOIN sys_role r ON r.id = rm.role_id "
                        + "JOIN sys_user_role ur ON ur.role_id = r.id "
                        + "WHERE ur.user_id = :userId AND r.status = 1",
                Map.of("userId", id), Long.class));

        // 非 ROOT 管理员：可分配菜单限定在自己架构子树对应的菜单归属内
        Set<Long> assignableMenuOrgIds = null;
        OrgScope.Scope scope = orgScope.scopeOf(loginUser);
        if (scope != null) {
            assignableMenuOrgIds = orgScope.subtreeIds(scope);
        }

        Set<Long> extras = new LinkedHashSet<>();
        if (form.menuIds() != null) {
            for (String menuId : form.menuIds()) {
                Long parsed = IdUtils.parseLong(menuId);
                if (parsed == null || roleMenuIds.contains(parsed) || !extras.add(parsed)) {
                    continue;
                }
                if (assignableMenuOrgIds != null) {
                    Long menuOrgId = jdbc.queryForObject(
                            "SELECT org_id FROM sys_menu WHERE id = :id", Map.of("id", parsed), Long.class);
                    if (menuOrgId == null || !assignableMenuOrgIds.contains(menuOrgId)) {
                        throw BusinessException.forbidden("无权分配该菜单权限");
                    }
                }
            }
        }

        jdbc.update("DELETE FROM sys_user_menu WHERE user_id = :userId", Map.of("userId", id));
        for (Long menuId : extras) {
            jdbc.update("INSERT INTO sys_user_menu (user_id, menu_id) VALUES (:userId, :menuId)",
                    Map.of("userId", id, "menuId", menuId));
        }
    }

    public void delete(String ids, LoginUser loginUser) {
        List<Long> idList = IdUtils.parse(ids);
        if (idList.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的用户");
        }
        for (Long id : idList) {
            orgScope.checkUser(loginUser, id);
        }
        Map<String, Object> params = Map.of("ids", idList);
        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (:ids)", params);
        jdbc.update("DELETE FROM sys_user_menu WHERE user_id IN (:ids)", params);
        userRepository.deleteAllById(idList);
    }

    public List<OptionItem> options() {
        List<SysUser> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        users.sort(java.util.Comparator.comparing(SysUser::getId));
        return users.stream().map(user -> new OptionItem(
                String.valueOf(user.getId()), user.getNickname())).toList();
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

    /**
     * 非 ROOT 用户创建/编辑用户时，所属架构必须在自己可见范围内。
     */
    private void checkOrgWritable(LoginUser loginUser, Long orgId) {
        if (loginUser == null || loginUser.isRoot()) {
            return;
        }
        if (orgId == null) {
            throw BusinessException.badRequest("请选择所属架构");
        }
        orgScope.check(loginUser, orgId);
    }
}
