/**
 * HTTP请求工具模块
 * 基于axios封装，提供统一的请求拦截、响应处理和错误处理
 * 支持token认证、重复提交防护、文件下载等功能
 */

import axios from 'axios'
import { Notification, MessageBox, Message, Loading } from 'element-ui'
import store from '@/store' // Vuex状态管理
import { getToken } from '@/utils/auth' // 获取认证token
import errorCode from '@/utils/errorCode' // 错误码映射
import { tansParams, blobValidate } from "@/utils/ruoyi"; // 参数转换和blob验证工具
import cache from '@/plugins/cache' // 缓存工具
import { saveAs } from 'file-saver' // 文件保存工具

// 下载加载实例，用于显示下载进度
let downloadLoadingInstance;

// 是否显示重新登录弹窗的标志，防止重复弹出
export let isRelogin = { show: false };

// 设置axios默认请求头
axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

/**
 * 创建axios实例
 * 配置基础URL和超时时间
 */
const service = axios.create({
  // 请求URL公共部分，从环境变量中读取
  baseURL: process.env.VUE_APP_BASE_API,
  // 请求超时时间：10秒
  timeout: 10000
})

/**
 * 请求拦截器
 * 在发送请求之前对请求配置进行处理
 * 主要功能：
 * 1. 添加认证token
 * 2. GET请求参数处理
 * 3. 防止重复提交
 */
service.interceptors.request.use(config => {
  // 检查是否需要设置token（某些接口可能不需要token）
  const isToken = (config.headers || {}).isToken === false
  // 检查是否需要防止数据重复提交（某些接口可能需要允许重复提交）
  const isRepeatSubmit = (config.headers || {}).repeatSubmit === false
  
  // 如果存在token且需要设置token，则在请求头中添加Authorization
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken() // 让每个请求携带自定义token 请根据实际情况自行修改
  }
  
  // GET请求参数处理：将params转换为URL查询字符串
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params);
    url = url.slice(0, -1); // 移除末尾的&或?
    config.params = {}; // 清空params，避免重复
    config.url = url;
  }
  
  // POST和PUT请求的重复提交防护
  if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
    // 构建当前请求对象
    const requestObj = {
      url: config.url, // 请求地址
      data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data, // 请求数据（转为字符串便于比较）
      time: new Date().getTime() // 请求时间戳
    }
    
    // 从sessionStorage获取上一次请求信息
    const sessionObj = cache.session.getJSON('sessionObj')
    
    if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
      // 首次请求，直接保存
      cache.session.setJSON('sessionObj', requestObj)
    } else {
      const s_url = sessionObj.url;                  // 上一次请求地址
      const s_data = sessionObj.data;                // 上一次请求数据
      const s_time = sessionObj.time;                // 上一次请求时间
      const interval = 500;                         // 间隔时间(ms)，小于此时间视为重复提交
      
      // 判断是否为重复提交：相同URL、相同数据、时间间隔小于500ms
      if (s_data === requestObj.data && requestObj.time - s_time < interval && s_url === requestObj.url) {
        const message = '数据正在处理，请勿重复提交';
        console.warn(`[${s_url}]: ` + message)
        return Promise.reject(new Error(message))
      } else {
        // 不是重复提交，更新sessionStorage中的请求信息
        cache.session.setJSON('sessionObj', requestObj)
      }
    }
  }
  return config
}, error => {
    // 请求配置错误处理
    console.log(error)
    Promise.reject(error)
})

/**
 * 响应拦截器
 * 对服务器返回的数据进行统一处理
 * 主要功能：
 * 1. 统一错误处理
 * 2. token过期处理
 * 3. 文件下载处理
 * 4. 业务错误提示
 */
let cd = false // 403错误防抖标志，避免频繁弹出错误提示
let fileNameByApi = "download.txt" // 默认下载文件名

service.interceptors.response.use(res => {
    // 获取响应状态码，未设置则默认200（成功）
    const code = res.data.code || 200;
    // 获取错误信息：优先使用错误码映射，其次使用响应消息，最后使用默认错误信息
    const msg = errorCode[code] || res.data.msg || errorCode['default']
    
    // 二进制数据（文件下载）处理：直接返回数据，不进行业务逻辑处理
    if(res.request.responseType ===  'blob' || res.request.responseType ===  'arraybuffer'){
      // 从响应头获取文件名
      fileNameByApi = decodeURIComponent(res.headers['download-filename'])
      return res.data
    }
    // 获取请求URL，用于特殊错误处理
    const requestUrl = res.config && res.config.url ? res.config.url : ""
    
    // 401错误：未授权/登录过期
    if (code === 401) {
      // 防止重复弹出登录提示
      if (!isRelogin.show) {
        isRelogin.show = true;
        // 弹出确认对话框，提示用户重新登录
        MessageBox.confirm('登录状态已过期，您可以继续留在该页面，或者重新登录', '系统提示', { 
          confirmButtonText: '重新登录', 
          cancelButtonText: '取消', 
          type: 'warning' 
        }).then(() => {
          // 用户确认重新登录
          isRelogin.show = false;
          store.dispatch('LogOut').then(() => {
            location.href = '/index'; // 跳转到登录页
          })
        }).catch(() => {
          // 用户取消，关闭提示
          isRelogin.show = false;
        });
      }
      return Promise.reject('无效的会话，或者会话已过期，请重新登录。')
    } 
    // 500错误：服务器内部错误
    else if (code === 500) {
      let friendlyMsg = msg
      // 特殊处理：审批服务错误，提供更友好的提示信息
      if (requestUrl.includes("/project/task/updateApprovalSet") && msg === "远程调用审批服务失败") {
        friendlyMsg = "设置失败：该审批已发起，当前待审批完成前不能再次设置审批人"
      }
      Message({ message: friendlyMsg, type: 'error' })
      return Promise.reject(new Error(friendlyMsg))
    } 
    // 601错误：业务警告
    else if (code === 601) {
      Message({ message: msg, type: 'warning' })
      return Promise.reject('error')
    } 
    // 403错误：无权限访问
    else if (code === 403) {
      // 使用防抖机制，避免频繁弹出错误提示
      if (cd) {
        // 防抖期间，不处理
      } else {
        Notification.error({ title: msg, duration: 1000 })
        cd = true;
        // 1秒后重置防抖标志
        var timerId = setTimeout(() => {
          cd = false
        }, 1000)
        return Promise.reject('error')
      }
    } 
    // 其他非200状态码：业务错误
    else if (code !== 200) {
      Notification.error({ title: msg })
      return Promise.reject('error')
    } 
    // 200成功：返回响应数据
    else {
      return res.data
    }
  },
  error => {
    // 网络请求错误处理
    console.log('err' + error)
    let { message } = error;
    
    // 根据错误类型提供友好的错误提示
    if (message == "Network Error") {
      message = "后端接口连接异常";
    } else if (message.includes("timeout")) {
      message = "系统接口请求超时";
    } else if (message.includes("Request failed with status code")) {
      // 提取HTTP状态码
      message = "系统接口" + message.substr(message.length - 3) + "异常";
    }
    
    // 显示错误提示，持续5秒
    Message({ message: message, type: 'error', duration: 5 * 1000 })
    return Promise.reject(error)
  }
)

/**
 * 通用文件下载方法
 * @param {string} url - 下载接口地址
 * @param {object} params - 请求参数
 * @param {string} filename - 下载文件名（可选，不传则使用服务器返回的文件名）
 * @param {object} config - 额外的axios配置（可选）
 * @returns {Promise} 下载Promise
 */
export function download(url, params, filename, config) {
  // 显示下载加载提示
  downloadLoadingInstance = Loading.service({ 
    text: "正在下载数据，请稍候", 
    spinner: "el-icon-loading", 
    background: "rgba(0, 0, 0, 0.7)" 
  })
  
  // 发送POST请求下载文件
  return service.post(url, params, {
    // 将参数转换为URL编码格式
    transformRequest: [(params) => { return tansParams(params) }],
    // 设置请求头为表单格式
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    // 响应类型为blob（二进制数据）
    responseType: 'blob',
    ...config // 合并额外配置
  }).then(async (data) => {
    // 验证返回数据是否为有效的blob
    const isBlob = blobValidate(data);
    if (isBlob) {
      // 创建Blob对象并保存文件
      const blob = new Blob([data])
      saveAs(blob, filename ?? fileNameByApi) // 使用传入的文件名或默认文件名
    } else {
      // 如果不是blob，说明下载失败，解析错误信息
      const resText = await data.text();
      const rspObj = JSON.parse(resText);
      const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
      Message.error(errMsg);
    }
    // 关闭加载提示
    downloadLoadingInstance.close();
  }).catch((r) => {
    // 下载失败处理
    console.error(r)
    Message.error('下载文件出现错误，请联系管理员！')
    downloadLoadingInstance.close();
  })
}

export default service
