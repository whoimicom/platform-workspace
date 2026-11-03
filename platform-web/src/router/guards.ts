import type { RouteRecordRaw } from "vue-router";
import NProgress from "@/plugins/nprogress";
import router from "@/router";
import { usePermissionStore, useUserStore } from "@/stores";

/**
 * 路由守卫
 *
 * 处理登录验证、动态路由生成、404 检测、页面标题与进度条
 */
export function setupPermissionGuard() {
  // 白名单支持前缀匹配：/login 命中 /login 及其子路径
  const whiteList = ["/login"];

  router.beforeEach(async (to, _from) => {
    NProgress.start();

    try {
      // 统一认证回调：认证中心携带 code 回跳应用根路径（hash 路由，参数在 search 上）
      const search = new URLSearchParams(window.location.search);
      const oidcError = search.get("error");
      const oidcCode = search.get("code");
      const oidcState = search.get("state");

      if (oidcCode && oidcState) {
        // 先清理地址栏，避免刷新时重复消费同一授权码
        window.history.replaceState(null, "", window.location.pathname + window.location.hash);
        NProgress.done();
        const redirect = await useUserStore().completeOidcLogin(oidcCode, oidcState);
        return redirect || "/";
      }

      if (oidcError) {
        console.error("统一认证失败:", oidcError, search.get("error_description"));
        window.history.replaceState(null, "", window.location.pathname + window.location.hash);
        NProgress.done();
        return "/login";
      }

      const isLoggedIn = useUserStore().isLoggedIn();

      // 未登录处理
      if (!isLoggedIn) {
        const isWhiteListed = whiteList.some(
          (path) => to.path === path || to.path.startsWith(`${path}/`)
        );
        if (isWhiteListed) {
          return;
        }
        NProgress.done();
        return `/login?redirect=${encodeURIComponent(to.fullPath)}`;
      }

      // 已登录访问登录页，重定向到首页
      if (to.path === "/login") {
        return { path: "/" };
      }

      const permissionStore = usePermissionStore();
      const userStore = useUserStore();

      // 动态路由生成
      if (!permissionStore.isRouteGenerated) {
        if (!userStore.userInfo?.roles?.length) {
          await userStore.fetchUserInfo();
        }

        const dynamicRoutes = await permissionStore.generateRoutes();
        dynamicRoutes.forEach((route: RouteRecordRaw) => {
          router.addRoute(route);
        });

        return { ...to, replace: true };
      }

      // 路由 404 检查
      if (to.matched.length === 0) {
        // 从登录页跳转且目标路径无效，回退首页（避免不同用户权限不同导致的 404）
        if (_from.path === "/login") {
          return { path: "/", replace: true };
        }
        return "/404";
      }

      // 动态标题
      const title = (to.params.title as string) || (to.query.title as string);
      if (title) {
        to.meta.title = title;
      }
    } catch (error) {
      console.error("Route guard error:", error);

      const userStore = useUserStore();
      // 会话失效已由请求拦截器统一清状态并跳登录页，这里不再重复导航
      if (userStore.isSessionExpired()) {
        NProgress.done();
        return false;
      }

      await userStore.resetAllState();
      NProgress.done();
      return "/login";
    }
  });

  router.afterEach(() => {
    NProgress.done();
  });
}
