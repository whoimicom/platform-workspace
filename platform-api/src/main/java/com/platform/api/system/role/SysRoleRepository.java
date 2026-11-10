package com.platform.api.system.role;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface SysRoleRepository extends CrudRepository<SysRole, Long> {

    Optional<SysRole> findByCode(String code);
}
