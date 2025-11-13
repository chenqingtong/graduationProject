import request from '@/utils/request'

// 查询流程列表
export function listProcess(query) {
  return request({
    url: '/workflow/process/list',
    method: 'get',
    params: query
  })
}

// 获取流程图
export function getBpmnXml(processDefId) {
  return request({
    url: '/workflow/process/bpmnXml/' + processDefId,
    method: 'get'
  })
}

export function detailProcess(query) {
  return request({
    url: '/workflow/process/detail',
    method: 'get',
    params: query
  })
}

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
