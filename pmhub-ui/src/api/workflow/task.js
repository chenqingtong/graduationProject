import request from "@/utils/request"

// 完成简化审批任务
export function completeSimplifiedApproval(data) {
  return request({
    url: "/workflow/process/completeSimplifiedApproval",
    method: "post",
    data: data,
  })
}
