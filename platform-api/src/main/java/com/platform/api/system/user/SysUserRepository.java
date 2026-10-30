package com.platform.api.system.user;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface SysUserRepository extends CrudRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
