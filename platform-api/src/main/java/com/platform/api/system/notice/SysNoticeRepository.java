package com.platform.api.system.notice;

import org.springframework.data.repository.CrudRepository;

import java.util.Collection;

public interface SysNoticeRepository extends CrudRepository<SysNotice, Long> {

    long countByIdInAndPublishStatus(Collection<Long> ids, Integer publishStatus);
}
