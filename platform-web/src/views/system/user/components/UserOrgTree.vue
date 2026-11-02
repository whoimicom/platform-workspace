<template>
  <el-card shadow="never" class="org-card">
    <el-input v-model="orgName" class="org-card__search" placeholder="搜索架构" clearable>
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <el-tree
      ref="orgTreeRef"
      class="org-card__tree"
      :data="orgList"
      :props="{ children: 'children', label: 'label', disabled: '' }"
      :expand-on-click-node="false"
      :filter-node-method="handleFilter"
      default-expand-all
      @node-click="handleNodeClick"
    />
  </el-card>
</template>

<script setup lang="ts">
import OrgAPI from "@/api/system/org";
import type { OptionItem } from "@/api/common";
import type { TreeNodeData } from "element-plus/es/components/tree";

/**
 * 用户架构树：按架构筛选用户
 */
const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: undefined,
  },
});

const orgList = ref<OptionItem[]>();
const orgTreeRef = ref();
const orgName = ref("");

/**
 * 选中架构时上抛架构 ID
 */
const emits = defineEmits(["node-click"]);

const orgId = useVModel(props, "modelValue", emits);

watchEffect(
  () => {
    orgTreeRef.value?.filter(orgName.value);
  },
  {
    flush: "post",
  }
);

/**
 * 架构树的节点过滤
 */
function handleFilter(value: string, data: TreeNodeData): boolean {
  if (!value) {
    return true;
  }
  return String(data.label ?? "").includes(value);
}

/**
 * 点击架构树节点
 */
function handleNodeClick(data: OptionItem): void {
  orgId.value = data.value;
  emits("node-click");
}

onBeforeMount(() => {
  OrgAPI.getOptions().then((data) => {
    orgList.value = data;
  });
});
</script>

<style lang="scss" scoped>
.org-card {
  height: 100%;
  background: transparent;
  border: 0;
  border-radius: inherit;
  box-shadow: none;

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
    padding: 12px;
  }
}

.org-card__search {
  margin-bottom: 10px;
}

.org-card__tree {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;

  :deep(.el-tree-node__content) {
    height: 32px;
  }
}
</style>
