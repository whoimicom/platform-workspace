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

          <el-table v-loading="tableLoading" :data="tableList" border max-height="560">
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
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button
                  v-hasPerm="'gen:config:import'"
                  link
                  type="primary"
                  @click="handleImport(scope.row.tableName)"
                >
                  {{ scope.row.configured ? "编辑" : "导入" }}
                </el-button>
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
            <el-table-column label="表名" prop="tableName" min-width="160" />
            <el-table-column label="功能名" prop="functionName" min-width="140" />
            <el-table-column label="模块" prop="moduleName" width="100" />
            <el-table-column label="业务名" prop="businessName" width="140" />
            <el-table-column label="类名" prop="className" width="140" />
            <el-table-column label="生成风格" width="120">
              <template #default="scope">
                <el-tag effect="plain">{{ scope.row.genStyle }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" prop="updateTime" width="170" />
            <el-table-column label="操作" width="180" fixed="right">
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

    <el-drawer v-model="drawer.visible" title="生成配置" size="960px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="功能名" prop="functionName">
              <el-input v-model="form.functionName" placeholder="页面标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模块名" prop="moduleName">
              <el-input v-model="form.moduleName" placeholder="如 rental" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务名" prop="businessName">
              <el-input v-model="form.businessName" placeholder="camelCase，如 superviseLock" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类名" prop="className">
              <el-input v-model="form.className" placeholder="PascalCase，如 SuperviseLock" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="前端路径" prop="frontendPath">
              <el-input v-model="form.frontendPath" placeholder="如 rental/superviseLock" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限前缀" prop="permPrefix">
              <el-input v-model="form.permPrefix" placeholder="如 rental:superviseLock:list" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生成风格" prop="genStyle">
              <el-select v-model="form.genStyle" style="width: 100%">
                <el-option label="动态 SQL（dynamic-sql）" value="dynamic-sql" />
                <el-option label="Data JDBC 原生（data-jdbc）" value="data-jdbc" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="根包名" prop="packageName">
              <el-input v-model="form.packageName" placeholder="com.platform.api" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="form.author" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-divider content-position="left">字段规则</el-divider>
      <el-table :data="columns" border size="small" max-height="420">
        <el-table-column label="列名" prop="columnName" width="150" />
        <el-table-column label="类型" prop="columnType" width="130" />
        <el-table-column label="注释" prop="columnComment" min-width="130" />
        <el-table-column label="属性" prop="propertyName" width="140" />
        <el-table-column label="Java 类型" width="160">
          <template #default="scope">
            <el-select
              v-model="scope.row.javaType"
              filterable
              allow-create
              default-first-option
              size="small"
              style="width: 100%"
            >
              <el-option v-for="item in javaTypeOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="必填" width="70" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.isRequired" :disabled="scope.row.isPk" />
          </template>
        </el-table-column>
        <el-table-column label="列表" width="70" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.isList" />
          </template>
        </el-table-column>
        <el-table-column label="筛选" width="70" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.isFilter" :disabled="scope.row.isPk" />
          </template>
        </el-table-column>
        <el-table-column label="筛选规则" width="130">
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
} from "@/api/gen";
import { usePageTable } from "@/composables";

defineOptions({
  name: "GenConfig",
  inheritAttrs: false,
});

const javaTypeOptions = [
  "String",
  "Integer",
  "Long",
  "BigDecimal",
  "Double",
  "Float",
  "Boolean",
  "LocalDate",
  "LocalDateTime",
];

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

const activeTab = ref("tables");

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

const drawer = reactive({ visible: false, id: 0, saving: false });
const formRef = ref<FormInstance>();
const form = reactive<GenConfigForm>({
  functionName: "",
  moduleName: "",
  businessName: "",
  className: "",
  frontendPath: "",
  permPrefix: "",
  genStyle: "dynamic-sql",
  packageName: "com.platform.api",
  author: "",
  columns: [],
});
const columns = ref<GenColumnItem[]>([]);

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

const previewDialog = reactive<{ visible: boolean; files: GenFileItem[]; active: string }>({
  visible: false,
  files: [],
  active: "",
});

const basename = (path: string) => path.split("/").pop() ?? path;

const handleImport = async (tableName: string) => {
  const id = await GenAPI.importTable(tableName);
  ElMessage.success("导入成功");
  fetchTableData();
  fetchConfigData();
  openDrawer(id);
};

const openDrawer = async (id: number) => {
  const detail = await GenAPI.getDetail(id);
  Object.assign(form, {
    functionName: detail.table.functionName,
    moduleName: detail.table.moduleName,
    businessName: detail.table.businessName,
    className: detail.table.className,
    frontendPath: detail.table.frontendPath,
    permPrefix: detail.table.permPrefix,
    genStyle: detail.table.genStyle,
    packageName: detail.table.packageName,
    author: detail.table.author ?? "",
    columns: [],
  });
  columns.value = detail.columns;
  drawer.id = id;
  drawer.visible = true;
};

const handleSaveConfig = async () => {
  await formRef.value?.validate();
  drawer.saving = true;
  try {
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

onMounted(() => {
  handleTableQuery();
  handleConfigQuery();
});
</script>

<style scoped>
.gen-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.gen-preview-path {
  margin-bottom: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
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
