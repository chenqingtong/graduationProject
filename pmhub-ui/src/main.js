/**
 * 项目主入口文件
 * 负责初始化Vue应用、配置全局组件、方法和插件
 */

import Vue from 'vue'

// Cookie工具库，用于存储和读取用户偏好设置
import Cookies from 'js-cookie'

// Element UI组件库
import Element from 'element-ui'
import './assets/styles/element-variables.scss'

// 全局样式文件
import '@/assets/styles/index.scss' // global css
import '@/assets/styles/ruoyi.scss' // ruoyi css

// 应用核心组件和配置
import App from './App'
import store from './store' // Vuex状态管理
import router from './router' // Vue Router路由配置
import directive from './directive' // 自定义指令
import plugins from './plugins' // 自定义插件
import { download } from '@/utils/request' // 文件下载工具方法

import './assets/icons' // icon
import './permission' // permission control
// 系统API方法
import { getDicts } from "@/api/system/dict/data"; // 获取字典数据
import { getConfigKey } from "@/api/system/config"; // 获取系统配置

// 工具方法
import { parseTime, resetForm, addDateRange, selectDictLabel, selectDictLabels, handleTree } from "@/utils/ruoyi";

// 全局通用组件导入
import Pagination from "@/components/Pagination"; // 分页组件
import RightToolbar from "@/components/RightToolbar" // 自定义表格工具组件（显示/隐藏列、刷新等）
import Editor from "@/components/Editor" // 富文本编辑器组件
import FileUpload from "@/components/FileUpload" // 文件上传组件
import ImageUpload from "@/components/ImageUpload" // 图片上传组件
import ImagePreview from "@/components/ImagePreview" // 图片预览组件
import DictTag from '@/components/DictTag' // 字典标签组件，用于显示字典值对应的文本
import VueMeta from 'vue-meta' // 头部标签组件，用于管理页面meta信息
import DictData from '@/components/DictData' // 字典数据组件

/**
 * 将常用方法挂载到Vue原型上，方便在组件中通过this直接调用
 * 这些方法可以在任何Vue组件中通过 this.方法名() 的方式使用
 */
Vue.prototype.getDicts = getDicts // 获取字典数据
Vue.prototype.getConfigKey = getConfigKey // 获取系统配置键值
Vue.prototype.parseTime = parseTime // 时间格式化
Vue.prototype.resetForm = resetForm // 重置表单
Vue.prototype.addDateRange = addDateRange // 添加日期范围参数
Vue.prototype.selectDictLabel = selectDictLabel // 根据字典值获取标签
Vue.prototype.selectDictLabels = selectDictLabels // 根据字典值数组获取标签数组
Vue.prototype.download = download // 文件下载
Vue.prototype.handleTree = handleTree // 树形数据处理

/**
 * 注册全局组件，所有组件都可以直接使用，无需导入
 * 使用方式：<组件名 /> 或 <组件名></组件名>
 */
Vue.component('DictTag', DictTag) // 字典标签组件
Vue.component('Pagination', Pagination) // 分页组件
Vue.component('RightToolbar', RightToolbar) // 表格工具栏组件
Vue.component('Editor', Editor) // 富文本编辑器
Vue.component('FileUpload', FileUpload) // 文件上传
Vue.component('ImageUpload', ImageUpload) // 图片上传
Vue.component('ImagePreview', ImagePreview) // 图片预览

/**
 * 使用Vue插件
 */
Vue.use(directive) // 注册自定义指令
Vue.use(plugins) // 注册自定义插件
Vue.use(VueMeta) // 注册meta标签管理插件
DictData.install() // 安装字典数据组件

/**
 * Mock数据说明
 * If you don't want to use mock-server
 * you want to use MockJs for mock api
 * you can execute: mockXHR()
 *
 * Currently MockJs will be used in the production environment,
 * please remove it before going online! ! !
 */

// 配置Element UI对话框默认行为：点击遮罩层不关闭对话框
Element.Dialog.props.closeOnClickModal.default=false;

// 使用Element UI，设置默认尺寸（从Cookie读取用户偏好，默认为medium）
Vue.use(Element, {
  size: Cookies.get('size') || 'medium' // set element-ui default size
})

// 关闭生产环境提示
Vue.config.productionTip = false

/**
 * 创建Vue根实例并挂载到DOM
 * @param {string} el - 挂载的DOM元素选择器
 * @param {Router} router - Vue Router实例
 * @param {Store} store - Vuex Store实例
 * @param {Function} render - 渲染函数
 */
new Vue({
  el: '#app', // 挂载到id为app的DOM元素
  router, // 注入路由
  store, // 注入状态管理
  render: h => h(App) // 渲染App根组件
})
