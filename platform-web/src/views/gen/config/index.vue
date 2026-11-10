<template>
  <div class="page-container gen-page">
    <el-card class="page-content" shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="数据表" name="tables">
          <div class="gen-toolbar">
            <el-input
              v-model="tableParams.keywords"
              placeholder="表名 / 表注释"
              clearable
              style="width: 240px"
              @keyup.enter="handleTableQuery"
              @clear="handleTableQuery"
            />
            <el-button type="primary" @click="handleTableQuery">搜索</el-button>
          </div>

          <el-table
            ref="tableTableRef"
            v-loading="tableLoading"
            :data="tableList"
            border
            max-height="520"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column label="表名" prop="tableName" min-width="200" />
            <el-table-column label="表注释" prop="tableComment" min-width="200" />
            <el-table-column label="行数" prop="tableRows" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.configured ? 'success' : 'info'" effect="plain">
                  {{ scope.row.configured ? "已配置" : "未配置" }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-if="tableTotal > 0"
            v-model:total="tableTotal"
            v-model:page="tableParams.pageNum"
            v-model:limit="tableParams.pageSize"
            @pagination="fetchTableData"
          />

          <div class="gen-toolbar gen-toolbar--footer">
            <span class="gen-selection">已选 {{ tableSelection.length }} 张表</span>
            <el-select
              v-model="selectedMainTable"
              placeholder="选择主表（通常为左连接主表）"
              :disabled="tableSelection.length === 0"
              style="width: 280px"
            >
              <el-option
                v-for="row in tableSelection"
                :key="row.tableName"
                :label="row.tableName"
                :value="row.tableName"
              />
            </el-select>
            <el-button
              v-hasPerm="'gen:config:import'"
              type="primary"
              :loading="creating"
              :disabled="!canCreate"
              @click="handleCreateConfig"
            >
              生成配置
            </el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="生成配置" name="configs">
          <div class="gen-toolbar">
            <el-input
              v-model="configParams.keywords"
              placeholder="表名 / 功能名"
              clearable
              style="width: 240px"
              @keyup.enter="handleConfigQuery"
              @clear="handleConfigQuery"
            />
            <el-button type="primary" @click="handleConfigQuery">搜索</el-button>
          </div>

          <el-table v-loading="configLoading" :data="configList" border max-height="560">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column label="主表" prop="mainTable" min-width="150" />
            <el-table-column label="关联表" min-width="220">
              <template #default="scope">
                <span class="gen-table-list">{{ scope.row.tableList || scope.row.mainTable }}</span>
              </template>
            </el-table-column>
            <el-table-column label="功能名" prop="functionName" min-width="140" />
            <el-table-column label="模块" prop="moduleName" width="100" />
            <el-table-column label="类名" prop="className" width="140" />
            <el-table-column label="生成风格" width="120">
              <template #default="scope">
                <el-tag effect="plain">{{ scope.row.genStyle }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" prop="updateTime" width="170" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="scope">
                <el-button
                  v-hasPerm="'gen:config:edit'"
                  link
                  type="primary"
                  @click="openDrawer(scope.row.id)"
                >
                  编辑
                </el-button>
                <el-button
                  v-hasPerm="'gen:config:preview'"
                  link
                  type="primary"
                  @click="handlePreview(scope.row.id)"
                >
                  预览
                </el-button>
                <el-button
                  v-hasPerm="'gen:config:generate'"
                  link
                  type="warning"
                  :loading="generatingId === scope.row.id"
                  @click="handleGenerate(scope.row.id)"
                >
                  生成
                </el-button>
                <el-button
                  v-hasPerm="'gen:config:delete'"
                  link
                  type="danger"
                  @click="handleDelete(scope.row.id)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-if="configTotal > 0"
            v-model:total="configTotal"
            v-model:page="configParams.pageNum"
            v-model:limit="configParams.pageSize"
            @pagination="fetchConfigData"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-drawer v-model="drawer.visible" title="生成配置" size="92%">
      <el-collapse v-model="activeSections" class="gen-collapse">
        <el-collapse-item title="配置信息" name="config">
          <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
            <el-row :gutter="16">
              <el-col :span="6">
                <el-form-item label="配置名称" prop="configName">
                  <el-input v-model="form.configName" placeholder="默认与主表同名" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="功能名" prop="functionName">
                  <el-input v-model="form.functionName" placeholder="页面标题" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="模块名" prop="moduleName">
                  <el-input v-model="form.moduleName" placeholder="如 rental" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="业务名" prop="businessName">
                  <el-input v-model="form.businessName" placeholder="camelCase，如 superviseLock" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="类名" prop="className">
                  <el-input v-model="form.className" placeholder="PascalCase，如 SuperviseLock" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="前端路径" prop="frontendPath">
                  <el-input v-model="form.frontendPath" placeholder="如 rental/superviseLock" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="权限前缀" prop="permPrefix">
                  <el-input v-model="form.permPrefix" placeholder="如 rental:superviseLock:list" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="生成风格" prop="genStyle">
                  <el-select v-model="form.genStyle" style="width: 100%">
                    <el-option label="动态 SQL（不生成实体）" value="dynamic-sql" />
                    <el-option label="Data JDBC 原生（生成实体关联）" value="data-jdbc" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="根包名" prop="packageName">
                  <el-input v-model="form.packageName" placeholder="com.platform.api" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="作者" prop="author">
                  <el-input v-model="form.author" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-collapse-item>
        <el-collapse-item title="关联表（主表列 = 从表列）" name="tables">
          <el-table :data="tables" border size="small">
            <el-table-column label="表名" prop="tableName" min-width="160" />
            <el-table-column label="表注释" prop="tableComment" min-width="140" />
            <el-table-column label="关联方式" width="170">
              <template #default="scope">
                <el-select
                  v-model="scope.row.relationType"
                  size="small"
                  :disabled="scope.row.tableName === mainTableName"
                  style="width: 100%"
                >
                  <el-option label="主表" value="MAIN" />
                  <el-option label="一对多 ONE_TO_MANY" value="ONE_TO_MANY" />
                  <el-option label="一对一 ONE_TO_ONE" value="ONE_TO_ONE" />
                  <el-option label="多对一 MANY_TO_ONE" value="MANY_TO_ONE" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="主表字段" width="170">
              <template #default="scope">
                <el-select
                  v-model="scope.row.mainColumn"
                  size="small"
                  filterable
                  clearable
                  :disabled="scope.row.tableName === mainTableName"
                  style="width: 100%"
                >
                  <el-option
                    v-for="column in mainColumns"
                    :key="column.columnName"
                    :label="column.columnName"
                    :value="column.columnName"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="关联表字段" width="170">
              <template #default="scope">
                <el-select
                  v-model="scope.row.joinColumn"
                  size="small"
                  filterable
                  clearable
                  :disabled="scope.row.tableName === mainTableName"
                  style="width: 100%"
                >
                  <el-option
                    v-for="column in tableColumnsOf(scope.row.tableName)"
                    :key="column.columnName"
                    :label="column.columnName"
                    :value="column.columnName"
                  />
                </el-select>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>

      <el-divider content-position="left">字段规则（列表与查询条件可来自任意关联表）</el-divider>
      <div class="gen-toolbar">
        <el-select v-model="activeTableForColumns" style="width: 220px">
          <el-option label="全部表" value="all" />
          <el-option
            v-for="table in tables"
            :key="table.tableName"
            :label="table.tableName"
            :value="table.tableName"
          />
        </el-select>
      </div>
      <el-table :data="visibleColumns" border size="small" max-height="420">
        <el-table-column label="表" prop="tableName" min-width="100" />
        <el-table-column label="列名 | 属性名" min-width="150" align="center">
          <template #default="scope">
            <div class="gen-pair">
              <span class="gen-pair-left">{{ scope.row.columnName }}</span>
              <span class="gen-pair-sep">|</span>
              <span class="gen-pair-right">{{ scope.row.propertyName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="DB类型 | 实体属性" min-width="160" align="center">
          <template #default="scope">
            <div class="gen-pair">
              <span class="gen-pair-left">{{ scope.row.columnType }}</span>
              <span class="gen-pair-sep">|</span>
              <span class="gen-pair-right">{{ scope.row.javaType }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="列表" width="60" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.isList" />
          </template>
        </el-table-column>
        <el-table-column label="显示列名" min-width="140">
          <template #default="scope">
            <el-input
              v-model="scope.row.displayName"
              size="small"
              clearable
              :disabled="!scope.row.isList"
              :placeholder="scope.row.columnComment || '未设置注释'"
            />
          </template>
        </el-table-column>
        <el-table-column label="必填" width="60" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.isRequired" :disabled="scope.row.isPk" />
          </template>
        </el-table-column>
        <el-table-column label="筛选" width="60" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.isFilter" :disabled="scope.row.isPk" />
          </template>
        </el-table-column>
        <el-table-column label="筛选规则" min-width="110">
          <template #default="scope">
            <el-select
              v-model="scope.row.filterType"
              size="small"
              :disabled="!scope.row.isFilter"
              style="width: 100%"
            >
              <el-option
                v-for="item in filterOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="表单" width="60" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.isForm"
              :disabled="scope.row.tableName !== mainTableName || scope.row.isPk"
            />
          </template>
        </el-table-column>
        <el-table-column label="表单元素" min-width="110">
          <template #default="scope">
            <el-select
              v-model="scope.row.formComponent"
              size="small"
              :disabled="!scope.row.isForm"
              style="width: 100%"
            >
              <el-option
                v-for="item in formComponentOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="drawer.visible = false">取消</el-button>
        <el-button type="primary" :loading="drawer.saving" @click="handleSaveConfig">
          保存
        </el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="previewDialog.visible" title="代码预览" width="90%" top="5vh">
      <el-tabs v-model="previewDialog.active" type="border-card">
        <el-tab-pane
          v-for="file in previewDialog.files"
          :key="file.path"
          :label="basename(file.path)"
          :name="file.path"
        >
          <div class="gen-preview-path">{{ file.path }}</div>
          <pre class="gen-preview-code">{{ file.content }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";

import GenAPI from "@/api/gen";
import type {
  DbTableItem,
  GenColumnItem,
  GenConfigForm,
  GenConfigItem,
  GenFileItem,
  GenQuery,
  GenTableItem,
} from "@/api/gen";
import { usePageTable } from "@/composables";

defineOptions({
  name: "GenConfig",
  inheritAttrs: false,
});

const filterOptions = [
  { value: "EQ", label: "等值 =" },
  { value: "NE", label: "不等于 !=" },
  { value: "GT", label: "大于 >" },
  { value: "GE", label: "大于等于 >=" },
  { value: "LT", label: "小于 <" },
  { value: "LE", label: "小于等于 <=" },
  { value: "LIKE", label: "模糊 %值%" },
  { value: "LIKE_LEFT", label: "左模糊 %值" },
  { value: "LIKE_RIGHT", label: "右模糊 值%" },
  { value: "IN", label: "包含 IN" },
  { value: "NOT_IN", label: "不包含 NOT IN" },
  { value: "BETWEEN", label: "区间 BETWEEN" },
];

const formComponentOptions = [
  { value: "INPUT", label: "输入框" },
  { value: "TEXTAREA", label: "多行文本" },
  { value: "NUMBER", label: "数字" },
  { value: "DATE", label: "日期" },
  { value: "DATETIME", label: "日期时间" },
  { value: "SWITCH", label: "开关" },
  { value: "SELECT", label: "下拉选择" },
];

const activeTab = ref("tables");

const tableTableRef = ref();
const tableSelection = ref<DbTableItem[]>([]);
const selectedMainTable = ref("");
const creating = ref(false);

const {
  loading: tableLoading,
  list: tableList,
  total: tableTotal,
  params: tableParams,
  fetchData: fetchTableData,
  handleQuery: handleTableQuery,
} = usePageTable<DbTableItem, GenQuery>({
  initialParams: { pageNum: 1, pageSize: 10, keywords: "" },
  request: GenAPI.getDbTables,
});

const {
  loading: configLoading,
  list: configList,
  total: configTotal,
  params: configParams,
  fetchData: fetchConfigData,
  handleQuery: handleConfigQuery,
} = usePageTable<GenConfigItem, GenQuery>({
  initialParams: { pageNum: 1, pageSize: 10, keywords: "" },
  request: GenAPI.getConfigs,
});

const canCreate = computed(() => tableSelection.value.length > 0 && !!selectedMainTable.value);

const handleSelectionChange = (rows: DbTableItem[]) => {
  tableSelection.value = rows;
  if (!rows.some((row) => row.tableName === selectedMainTable.value)) {
    selectedMainTable.value = rows[0]?.tableName ?? "";
  }
};

const drawer = reactive({ visible: false, id: 0, saving: false });
const activeSections = ref<string[]>(["config", "tables"]);
const formRef = ref<FormInstance>();
const form = reactive<GenConfigForm>({
  configName: "",
  functionName: "",
  moduleName: "",
  businessName: "",
  className: "",
  frontendPath: "",
  permPrefix: "",
  genStyle: "dynamic-sql",
  packageName: "com.platform.api",
  author: "",
  tables: [],
  columns: [],
});
const tables = ref<GenTableItem[]>([]);
const columns = ref<GenColumnItem[]>([]);
const mainTableName = ref("");
const activeTableForColumns = ref("all");

const formRules: FormRules = {
  functionName: [{ required: true, message: "请输入功能名", trigger: "blur" }],
  moduleName: [{ required: true, message: "请输入模块名", trigger: "blur" }],
  businessName: [{ required: true, message: "请输入业务名", trigger: "blur" }],
  className: [{ required: true, message: "请输入类名", trigger: "blur" }],
  frontendPath: [{ required: true, message: "请输入前端路径", trigger: "blur" }],
  permPrefix: [{ required: true, message: "请输入权限前缀", trigger: "blur" }],
  genStyle: [{ required: true, message: "请选择生成风格", trigger: "change" }],
  packageName: [{ required: true, message: "请输入根包名", trigger: "blur" }],
};

const columnsByTable = computed<Record<string, GenColumnItem[]>>(() => {
  const map: Record<string, GenColumnItem[]> = {};
  columns.value.forEach((column) => {
    if (!map[column.tableName]) {
      map[column.tableName] = [];
    }
    map[column.tableName].push(column);
  });
  return map;
});

const mainColumns = computed(() => columnsByTable.value[mainTableName.value] ?? []);

const tableColumnsOf = (tableName: string) => columnsByTable.value[tableName] ?? [];

const visibleColumns = computed(() =>
  activeTableForColumns.value === "all"
    ? columns.value
    : columns.value.filter((column) => column.tableName === activeTableForColumns.value)
);

const previewDialog = reactive<{ visible: boolean; files: GenFileItem[]; active: string }>({
  visible: false,
  files: [],
  active: "",
});

const basename = (path: string) => path.split("/").pop() ?? path;

const handleCreateConfig = async () => {
  if (!canCreate.value) {
    ElMessage.warning("请先勾选数据表并选择主表");
    return;
  }
  creating.value = true;
  try {
    const id = await GenAPI.createConfig({
      tableNames: tableSelection.value.map((row) => row.tableName),
      mainTable: selectedMainTable.value,
    });
    ElMessage.success("生成配置已创建");
    fetchTableData();
    fetchConfigData();
    await openDrawer(id);
  } finally {
    creating.value = false;
  }
};

const openDrawer = async (id: number) => {
  const detail = await GenAPI.getDetail(id);
  Object.assign(form, {
    configName: detail.config.configName,
    functionName: detail.config.functionName,
    moduleName: detail.config.moduleName,
    businessName: detail.config.businessName,
    className: detail.config.className,
    frontendPath: detail.config.frontendPath,
    permPrefix: detail.config.permPrefix,
    genStyle: detail.config.genStyle,
    packageName: detail.config.packageName,
    author: detail.config.author ?? "",
    tables: [],
    columns: [],
  });
  mainTableName.value = detail.config.mainTable;
  tables.value = detail.tables;
  columns.value = detail.columns;
  activeTableForColumns.value = detail.config.mainTable;
  activeSections.value = ["config", "tables"];
  drawer.id = id;
  drawer.visible = true;
};

const handleSaveConfig = async () => {
  await formRef.value?.validate();
  const invalid = tables.value.find(
    (table) => table.tableName !== mainTableName.value && (!table.mainColumn || !table.joinColumn)
  );
  if (invalid) {
    ElMessage.warning(`请为关联表 ${invalid.tableName} 选择关联字段`);
    return;
  }
  drawer.saving = true;
  try {
    form.tables = tables.value;
    form.columns = columns.value;
    await GenAPI.update(drawer.id, form);
    ElMessage.success("保存成功");
    drawer.visible = false;
    fetchConfigData();
  } finally {
    drawer.saving = false;
  }
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm("确认删除该生成配置吗？", "警告", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    ElMessage.info("已取消删除");
    return;
  }
  await GenAPI.deleteByIds(String(id));
  ElMessage.success("删除成功");
  fetchConfigData();
  fetchTableData();
};

const handlePreview = async (id: number) => {
  previewDialog.files = await GenAPI.preview(id);
  previewDialog.active = previewDialog.files[0]?.path ?? "";
  previewDialog.visible = true;
};

const generatingId = ref(0);

const handleGenerate = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      "将把代码直接写入项目并覆盖同路径文件（含后端 Java、前端页面与菜单 SQL），确认生成吗？",
      "生成确认",
      { confirmButtonText: "生成", cancelButtonText: "取消", type: "warning" }
    );
  } catch {
    return;
  }
  generatingId.value = id;
  try {
    const paths = await GenAPI.generate(id);
    await ElMessageBox.alert(
      paths.map((p) => `<div style="font-family:monospace">${p}</div>`).join(""),
      `已写入 ${paths.length} 个文件（菜单 SQL 需手动执行后刷新页面）`,
      { dangerouslyUseHTMLString: true, confirmButtonText: "知道了" }
    );
  } finally {
    generatingId.value = 0;
  }
};

onMounted(() => {
  handleTableQuery();
  handleConfigQuery();
});
</script>

<style scoped>
.gen-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.gen-toolbar--footer {
  margin-top: 12px;
  justify-content: flex-end;
}

.gen-selection {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.gen-table-list {
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.gen-preview-path {
  margin-bottom: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.gen-collapse {
  border-top: none;
  border-bottom: none;
}

.gen-collapse :deep(.el-collapse-item__content) {
  padding-bottom: 8px;
}

.gen-pair {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.gen-pair-left {
  flex: 1;
  overflow: hidden;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gen-pair-sep {
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
}

.gen-pair-right {
  flex: 1;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gen-preview-code {
  margin: 0;
  max-height: 62vh;
  overflow: auto;
  padding: 12px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre;
}
</style>
