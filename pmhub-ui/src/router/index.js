import Vue from "vue"
import Router from "vue-router"

Vue.use(Router)

/* Layout */
import Layout from "@/layout"

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

/**
 * 公共路由配置
 * 这些路由不需要权限验证，所有用户都可以访问
 * 包括：登录页、404、401、首页等
 */
export const constantRoutes = [
  {
    path: "/redirect",
    component: Layout,
    hidden: true, // 不在侧边栏显示
    children: [
      {
        path: "/redirect/:path(.*)", // 重定向路由，支持动态路径
        component: () => import("@/views/redirect"),
      },
    ],
  },
  {
    path: "/login",
    component: () => import("@/views/login"), // 登录页面
    hidden: true, // 不在侧边栏显示
  },
  // {
  //   path: '/register',
  //   component: () => import('@/views/register'),
  //   hidden: true
  // },
  {
    path: "/404",
    component: () => import("@/views/error/404"), // 404错误页面
    hidden: true,
  },
  {
    path: "/401",
    component: () => import("@/views/error/401"), // 401未授权页面
    hidden: true,
  },
  {
    path: "",
    component: Layout,
    redirect: "index", // 根路径重定向到首页
    children: [
      {
        path: "index",
        component: () => import("@/views/dashboard/index"), // 首页/工作台
        name: "Index",
        meta: { 
          title: "首页", 
          icon: "dashboard", 
          affix: true // 固定在标签页，不可关闭
        },
      },
    ],
  },
  {
    path: "/tool",
    component: Layout,
    hidden: true, // 工具类路由，不在侧边栏显示
    children: [
      {
        path: "build/index",
        component: () => import("@/views/tool/build/index"), // 表单设计器
        name: "FormBuild",
        meta: { title: "表单设计", icon: "" },
      },
    ],
  },
  {
    path: "/user",
    component: Layout,
    hidden: true,
    redirect: "noredirect", // 不重定向
    children: [
      {
        path: "profile",
        component: () => import("@/views/system/user/profile/index"), // 个人中心
        name: "Profile",
        meta: { title: "个人中心", icon: "user" },
      },
    ],
  },
]

/**
 * 动态路由配置
 * 这些路由需要根据用户权限动态加载
 * 包括：系统管理、项目管理、工作流等模块的路由
 * 路由的显示和访问受 permissions 字段控制
 */
export const dynamicRoutes = [
  {
    path: "/system/user-auth",
    component: Layout,
    hidden: true,
    permissions: ["system:user:edit"],
    children: [
      {
        path: "role/:userId(\\d+)",
        component: () => import("@/views/system/user/authRole"),
        name: "AuthRole",
        meta: { title: "分配角色", activeMenu: "/system/user" },
      },
    ],
  },
  {
    path: "/system/role-auth",
    component: Layout,
    hidden: true,
    permissions: ["system:role:edit"],
    children: [
      {
        path: "user/:roleId(\\d+)",
        component: () => import("@/views/system/role/authUser"),
        name: "AuthUser",
        meta: { title: "分配用户", activeMenu: "/system/role" },
      },
    ],
  },
  {
    path: "/system/dict-data",
    component: Layout,
    hidden: true,
    permissions: ["system:dict:list"],
    children: [
      {
        path: "index/:dictId(\\d+)",
        component: () => import("@/views/system/dict/data"),
        name: "Data",
        meta: { title: "字典数据", activeMenu: "/system/dict" },
      },
    ],
  },
  {
    path: "/monitor/job-log",
    component: Layout,
    hidden: true,
    permissions: ["monitor:job:list"],
    children: [
      {
        path: "index/:jobId(\\d+)",
        component: () => import("@/views/monitor/job/log"),
        name: "JobLog",
        meta: { title: "调度日志", activeMenu: "/monitor/job" },
      },
    ],
  },
  {
    path: "/tool/gen-edit",
    component: Layout,
    hidden: true,
    permissions: ["tool:gen:edit"],
    children: [
      {
        path: "index/:tableId(\\d+)",
        component: () => import("@/views/tool/gen/editTable"),
        name: "GenEdit",
        meta: { title: "修改生成配置", activeMenu: "/tool/gen" },
      },
    ],
  },
  /**
   * 项目管理模块路由
   */
  {
    path: "/pmhub-project/my-project/info",
    component: Layout,
    hidden: true, // 详情页不在侧边栏显示
    permissions: ["pmhub-project:my-project:info"], // 需要项目详情权限
    children: [
      {
        path: "",
        component: () => import("@/views/pmhub-project/my-project/info"), // 项目详情页
        name: "MyProjectInfo",
        meta: { 
          title: "项目详情", 
          activeMenu: "/pmhub-project/my-project", // 激活的菜单项
          noCache: true // 不缓存页面，每次进入都重新加载
        },
      },
    ],
  },
  {
    path: "/pmhub-project/my-task/info",
    component: Layout,
    hidden: true,
    permissions: ["pmhub-project:my-task:info"],
    children: [
      {
        path: "",
        component: () => import("@/views/pmhub-project/my-task/info"),
        name: "MyTaskInfo",
        meta: { title: "任务详情", activeMenu: "/pmhub-project/my-task", noCache: true },
      },
    ],
  },
  {
    path: "/pmhub-project/my-task/child/info",
    component: Layout,
    hidden: true,
    permissions: ["pmhub-project:my-task:info"],
    children: [
      {
        path: "",
        component: () => import("@/views/pmhub-project/my-task/info"),
        name: "MyTaskChildInfo",
        meta: { title: "子任务详情", activeMenu: "/pmhub-project/my-task", noCache: true },
      },
    ],
  },
  /**
   * 工作流/审批流程模块路由
   */
  {
    path: "/workflow/process",
    component: Layout,
    hidden: true,
    permissions: ["workflow:process:query"], // 需要流程查询权限
    children: [
      {
        path: "detail/:procInsId([\\w|\\-]+)", // 流程详情页，支持流程实例ID参数
        component: () => import("@/views/workflow/work/detail"),
        name: "WorkDetail",
        meta: { title: "流程详情", activeMenu: "/work/own" },
      },
    ],
  },
  {
    path: "/workflow/work",
    component: Layout,
    hidden: true,
    permissions: ["workflow:process:approval"],
    children: [
      {
        path: "simplified-approval",
        component: () => import("@/views/workflow/work/simplified-approval"),
        name: "SimplifiedApproval",
        meta: { title: "简化审批", activeMenu: "/workflow/work/todo" },
      },
    ],
  },
]

/**
 * 路由导航守卫：防止连续点击多次路由报错
 * 当用户快速连续点击路由时，可能会触发重复导航错误
 * 通过捕获错误来避免控制台报错
 */
let routerPush = Router.prototype.push
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch((err) => err)
}

/**
 * 创建并导出Vue Router实例
 * @type {Router}
 */
export default new Router({
  mode: "history", // 使用HTML5 History模式，去掉URL中的#号
  scrollBehavior: () => ({ y: 0 }), // 路由切换时滚动到页面顶部
  routes: constantRoutes, // 初始路由配置（动态路由会在权限验证后添加）
})
