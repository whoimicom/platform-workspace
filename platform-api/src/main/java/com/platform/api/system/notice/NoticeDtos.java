package com.platform.api.system.notice;

import com.platform.api.common.query.Op;
import com.platform.api.common.query.PageQuery;
import com.platform.api.common.query.QueryField;
import com.platform.api.common.query.QuerySort;
import com.platform.api.common.query.QuerySortMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * 与前端 src/api/system/notice/types.ts 对应的数据结构。
 */
public final class NoticeDtos {

    private NoticeDtos() {
    }

    public record NoticeItem(String id, String title, String content, Integer type, String level,
                             Integer publishStatus, Integer isRead, Integer targetType,
                             String publisherName, String publishTime, String revokeTime,
                             String createTime) {
    }

    public record NoticeDetail(String id, String title, String content, Integer type, String level,
                               Integer publishStatus, Integer targetType, String targetUserIds,
                               String publisherName, String publishTime, String revokeTime,
                               String createTime, Integer isRead) {
    }

    /**
     * 表单同时兼容前端的 targetUsers（回显）与 targetUserIds（提交）。
     */
    public record NoticeForm(String id, String title, String content, Integer type, String level,
                             Integer status, Integer targetType, List<Long> targetUsers,
                             List<Long> targetUserIds) {
    }

    /**
     * 通知分页查询参数（查询串绑定使用）。
     */
    @QuerySort(value = {
            @QuerySortMapping(property = "title", column = "n.title"),
            @QuerySortMapping(property = "publishTime", column = "n.publish_time"),
            @QuerySortMapping(property = "createTime", column = "n.create_time")
    }, defaultSort = "n.create_time DESC, n.id DESC")
    public static class NoticeQuery extends PageQuery {

        @QueryField(value = "n.title", op = Op.LIKE)
        private String title;

        @QueryField(value = "n.publish_status")
        private Integer publishStatus;

        /** 0=未读 1=已读，仅在“我的通知”分页生效 */
        private Integer isRead;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getPublishStatus() {
            return publishStatus;
        }

        public void setPublishStatus(Integer publishStatus) {
            this.publishStatus = publishStatus;
        }

        public Integer getIsRead() {
            return isRead;
        }

        public void setIsRead(Integer isRead) {
            this.isRead = isRead;
        }
    }
}
