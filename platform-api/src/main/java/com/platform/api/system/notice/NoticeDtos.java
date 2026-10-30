package com.platform.api.system.notice;

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
    public static class NoticeQuery {

        private Integer pageNum = 1;
        private Integer pageSize = 10;
        private String title;
        private Integer publishStatus;
        private Integer isRead;

        public Integer getPageNum() {
            return pageNum;
        }

        public void setPageNum(Integer pageNum) {
            this.pageNum = pageNum;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

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
