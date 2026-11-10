<template>
  <div class="page-container">
    <el-card class="page-search" shadow="never">
      <el-form ref="queryFormRef" :model="params" :inline="true">
        <el-form-item label="关键字" prop="keywords">
          <el-input
            v-model="params.keywords"
            placeholder="订单号 / 序列号 / 串码 / 型号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="订单号" prop="orderId">
          <el-input v-model="params.orderId" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="锁状态" prop="status">
          <el-select v-model="params.status" placeholder="全部" clearable style="width: 120px">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="租户主体" prop="tenant">
          <el-select v-model="params.tenant" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="item in tenantOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间" prop="createTime">
          <el-date-picker
            v-model="params.createTime"
            type="datetimerange"
            value-format="YYYY-MM-DD[T]HH:mm:ss"
            range-separator="~"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card ref="tableWrapperRef" class="page-content" shadow="never">
      <div class="page-toolbar">
        <div class="page-toolbar__left" />
        <div class="page-toolbar__right">
          <el-tooltip content="刷新" placement="top">
            <el-button class="page-icon-btn" @click="handleQuery">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="全屏" placement="top">
            <el-button class="page-icon-btn" @click="toggleFullscreen">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table
          v-loading="loading"
          :data="list"
          class="page-table"
          height="100%"
          highlight-current-row
          border
        >
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column key="orderId" label="订单号" prop="orderId" min-width="180" />
          <el-table-column
            key="superviseName"
            label="监管名称"
            prop="superviseName"
            min-width="120"
          />
          <el-table-column key="serialNumber" label="序列号" prop="serialNumber" min-width="160" />
          <el-table-column key="serialCode" label="串码" prop="serialCode" min-width="150" />
          <el-table-column key="model" label="型号" prop="model" min-width="140" />
          <el-table-column key="brandId" label="品牌" prop="brandId" min-width="90" />
          <el-table-column key="sysType" label="系统类型" prop="sysType" min-width="100" />
          <el-table-column key="status" label="是否上锁" min-width="100">
            <template #default="scope">{{ statusLabel(scope.row.status) }}</template>
          </el-table-column>
          <el-table-column key="onlineStatus" label="在线状态" min-width="90">
            <template #default="scope">{{ yesNo(scope.row.onlineStatus) }}</template>
          </el-table-column>
          <el-table-column key="supervise" label="监管中" min-width="90">
            <template #default="scope">{{ yesNo(scope.row.supervise) }}</template>
          </el-table-column>
          <el-table-column
            key="activationLockStatus"
            label="激活锁"
            prop="activationLockStatus"
            min-width="110"
          />
          <el-table-column key="tenant" label="租户主体" prop="tenant" min-width="100" />
          <el-table-column key="lastActiveTime" label="最后活动时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.lastActiveTime) }}</template>
          </el-table-column>
          <el-table-column key="createTime" label="创建时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <pagination
        v-if="total > 0"
        v-model:total="total"
        v-model:page="params.pageNum"
        v-model:limit="params.pageSize"
        @pagination="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { Refresh, FullScreen } from "@element-plus/icons-vue";
import type { FormInstance } from "element-plus";

import SuperviseLockAPI from "@/api/rental/supervise";
import type { SuperviseLockItem, SuperviseLockQuery } from "@/api/rental/supervise";
import { usePageTable } from "@/composables";

defineOptions({
  name: "RentalSuperviseLock",
  inheritAttrs: false,
});

const statusOptions = [
  { value: "1", label: "已锁" },
  { value: "0", label: "正常" },
];
const tenantOptions = [
  { value: "HHHZ", label: "好货惠租" },
  { value: "HHX", label: "换换星" },
  { value: "JTX", label: "JTX" },
  { value: "JXX", label: "JXX" },
  { value: "OBYG", label: "OBYG" },
];

const statusLabel = (value?: string) =>
  statusOptions.find((item) => item.value === value)?.label ?? value ?? "";
const yesNo = (value?: number) =>
  value === undefined || value === null ? "" : value === 1 ? "是" : "否";
const formatDateTime = (value?: string) => (value ? value.replace("T", " ") : "");

const tableWrapperRef = ref<HTMLElement | null>(null);
const { toggle: toggleFullscreen } = useFullscreen(tableWrapperRef);

const queryFormRef = ref<FormInstance>();

const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<
  SuperviseLockItem,
  SuperviseLockQuery
>({
  initialParams: {
    pageNum: 1,
    pageSize: 10,
    keywords: "",
    orderId: "",
    status: "",
    tenant: "",
    createTime: undefined,
  },
  request: SuperviseLockAPI.getPage,
  onBeforeReset: () => queryFormRef.value?.resetFields(),
});

onMounted(() => {
  handleQuery();
});
</script>
