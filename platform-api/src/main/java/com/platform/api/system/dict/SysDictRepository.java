package com.platform.api.system.dict;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface SysDictRepository extends CrudRepository<SysDict, Long> {

    Optional<SysDict> findByDictCode(String dictCode);
}
