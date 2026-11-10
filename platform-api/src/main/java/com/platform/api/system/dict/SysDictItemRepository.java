package com.platform.api.system.dict;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface SysDictItemRepository extends CrudRepository<SysDictItem, Long> {

    List<SysDictItem> findByDictCode(String dictCode);

    Optional<SysDictItem> findByDictCodeAndId(String dictCode, Long id);

    Optional<SysDictItem> findByDictCodeAndValue(String dictCode, String value);
}
