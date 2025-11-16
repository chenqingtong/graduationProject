import request from '@/utils/request'

// 注意：listProcess、getBpmnXml 接口已删除（Flowable 相关功能已移除）
// detailProcess 接口保留，仅用于查询流转记录

// 我待办的流程
export function listTodoProcess(query) {
  return request({
    url: '/workflow/process/todoList',
    method: 'get',
    params: query
  })
}

// 我已办的流程
export function listFinishedProcess(query) {
  return request({
    url: '/workflow/process/finishedList',
    method: 'get',
    params: query
  })
}

// 查询流程抄送列表
export function listCopyProcess(query) {
  return request({
    url: '/workflow/process/copyList',
    method: 'get',
    params: query
  })
}

// 查询审批任务列表（用于显示审批进度）
export function getApprovalTaskList(extraId, type) {
  return request({
    url: '/workflow/process/getApprovalTaskList',
    method: 'get',
    params: {
      extraId,
      type
    }
  })
}

// 查询流程详情（仅用于获取流转记录，支持 Flowable 和简化审批流程）
export function detailProcess(query) {
  return request({
    url: '/workflow/process/detail',
    method: 'get',
    params: query
  })
}
