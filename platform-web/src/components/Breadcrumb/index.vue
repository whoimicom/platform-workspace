<template>
  <el-breadcrumb class="flex-y-center">
    <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="item.path">
      <!-- 末级或不可跳转的节点显示为纯文本，其余可点击 -->
      <span
        v-if="item.redirect === 'noredirect' || index === breadcrumbs.length - 1"
        class="color-gray-400"
      >
        {{ item.meta.title ?? "" }}
      </span>
      <a v-else @click.prevent="handleLink(item)">
        {{ item.meta.title ?? "" }}
      </a>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import type { RouteLocationMatched } from "vue-router";
import { compile } from "path-to-regexp";
import router from "@/router";

type BreadcrumbRoute = {
  path: string;
  name?: RouteLocationMatched["name"];
  redirect?: string;
  meta: RouteLocationMatched["meta"];
};

const currentRoute = useRoute();

/**
 * 面包屑取 matched 链，不拼首页（首页与一级菜单平级，非父级）
 */
const pathCompile = (path: string) => {
  // 补全动态路由参数，如 /user/:id
  const { params } = currentRoute;
  const toPath = compile(path);
  return toPath(params);
};

const breadcrumbs = ref<BreadcrumbRoute[]>([]);

/**
 * 生成面包屑：取路由 matched 中有标题的层级，
 * 用 meta.breadcrumb = false 可以隐藏某一级
 */
function getBreadcrumb() {
  const matched: BreadcrumbRoute[] = currentRoute.matched
    .filter((item) => item.meta && item.meta.title)
    .map(({ path, name, redirect, meta }) => ({
      path,
      name,
      redirect: typeof redirect === "string" ? redirect : undefined,
      meta,
    }));

  breadcrumbs.value = matched.filter((item) => {
    return item.meta && item.meta.title && item.meta.breadcrumb !== false;
  });
}

/**
 * 跳转：有 redirect 走 redirect，否则按路径（含动态参数先 compile）
 */
function handleLink(item: BreadcrumbRoute) {
  const { redirect, path } = item;
  if (redirect) {
    router.push(redirect).catch((err) => {
      console.warn(err);
    });
    return;
  }
  router.push(pathCompile(path)).catch((err) => {
    console.warn(err);
  });
}

/**
 * 路由签名：完整反映当前导航状态（fullPath 含 query，matched 反映动态路由 addRoute 后的链）
 * 同时监听两者：动态路由 addRoute + replace 场景下 path 可能不变但 matched 会变
 */
const routeSignature = computed(() => {
  const titles = currentRoute.matched
    .map((m) => m.meta?.title ?? "")
    .filter(Boolean)
    .join("/");
  return `${currentRoute.fullPath}::${titles}`;
});

watch(
  routeSignature,
  () => {
    // /redirect/ 这类中转路由跳过
    if (currentRoute.path.startsWith("/redirect/")) {
      return;
    }
    getBreadcrumb();
  },
  { immediate: true }
);

// DOM 创建前再兜底一次，确保 el-breadcrumb 首次渲染时 children 已正确
onBeforeMount(() => {
  getBreadcrumb();
});
</script>

<style lang="scss" scoped>
// 覆盖 element-plus 的样式
.el-breadcrumb__inner,
.el-breadcrumb__inner a {
  font-weight: 400 !important;
}
</style>
