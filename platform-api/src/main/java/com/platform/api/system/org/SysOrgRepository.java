package com.platform.api.system.org;

import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;

public interface SysOrgRepository extends CrudRepository<SysOrg, Long> {

    long countByParentIdIn(Collection<Long> parentIds);

    List<SysOrg> findByParentId(Long parentId);
}
