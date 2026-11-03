package com.platform.api.auth;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface SysUserIdentityRepository extends CrudRepository<SysUserIdentity, Long> {

    /** 按 provider + openId 查询（唯一键） */
    Optional<SysUserIdentity> findByProviderAndOpenId(String provider, String openId);

    /** 按 unionId 查（跨应用关联，可能多条） */
    List<SysUserIdentity> findByProviderAndUnionId(String provider, String unionId);

    /** 查询某用户的全部三方绑定 */
    List<SysUserIdentity> findByUserId(Long userId);
}
