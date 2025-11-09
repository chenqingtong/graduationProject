<template>
  <div>
    <el-dialog title="发起审批" :visible.sync="visible" width="50%" :before-close="handleClose" @open="handleOpen">
      <div class="approval-confirm">
        <el-alert
          title="确认发起审批"
          type="info"
          :closable="false"
          show-icon>
          <div slot="title">
            <span>确认发起审批</span>
          </div>
          <div class="approval-info">
            <p><strong>任务名称：</strong>{{ info.taskName }}</p>
            <p><strong>所属项目：</strong>{{ info.projectName }}</p>
            <p><strong>任务状态：</strong>{{ info.statusName }}</p>
            <p><strong>优先级：</strong>{{ info.taskPriorityName }}</p>
            <p v-if="approvalInfoText"><strong>审批人：</strong>{{ approvalInfoText }}</p>
        </div>
        </el-alert>
        <div class="action-buttons">
          <el-button type="primary" @click="handleSubmit">确认发起审批</el-button>
          <el-button @click="handleClose">取消</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { startProcessApi } from "@/api/pmhub-project/my-task"
import { getUserProfile, getUser } from "@/api/system/user"

let INFO_PATH = ""
let INFO_URL = ""

export default {
  name: "Start",
  props: {
    visible: {
      type: Boolean,
      required: true,
    },
    taskId: {
      type: String,
      required: true,
    },
    // 后端返回的审批设置数据
    workFlowable: {
      type: Object,
      required: true,
    },
    // 重新获取任务列表数据
    getTableData: {
      type: Function,
    },
    // 当前单据详情数据
    info: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      user: {},
      approvalInfoText: "",
    }
  },
  created() {
    INFO_PATH = `${window.location.pathname}/info`
    INFO_URL = `${window.location.origin}${INFO_PATH}`
    getUserProfile().then((response) => {
      // 拿到当前登录的用户信息，审批流程表单需要填写用户昵称字段
      this.user = response.data
    })
  },
  methods: {
    handleClose() {
      this.$emit("update:visible", false)
    },
    handleOpen() {
      // 使用 $nextTick 确保在对话框完全打开后再进行验证
      this.$nextTick(() => {
        this.initData()
      })
    },
    initData() {
      console.log("Start组件 initData 被调用")
      console.log("Start组件接收到的 workFlowable:", this.workFlowable)
      // 验证审批人设置是否存在
      const approvalInfo = this.workFlowable?.approvalInfo
      console.log("Start组件中的 approvalInfo:", approvalInfo, "类型:", typeof approvalInfo)
      if (!approvalInfo) {
        console.log("Start组件: approvalInfo为空")
        this.$modal.msgError("审批人设置不完整，请先在任务设置页面设置审批人")
        // 延迟关闭，确保错误提示能显示
        this.$nextTick(() => {
          this.handleClose()
        })
        return
      }
      // 解析审批人信息并验证
      try {
        const approvalInfoObj = JSON.parse(approvalInfo)
        console.log("Start组件解析后的 approvalInfoObj:", approvalInfoObj)
        // 检查是否有有效的审批人信息（使用显式检查，避免空字符串被判断为false）
        const hasValidApprover = (approvalInfoObj.assignee && String(approvalInfoObj.assignee).trim() !== "") || 
                                 (approvalInfoObj.candidateUsers && String(approvalInfoObj.candidateUsers).trim() !== "") || 
                                 (approvalInfoObj.candidateGroups && String(approvalInfoObj.candidateGroups).trim() !== "") ||
                                 approvalInfoObj.dataType === "INITIATOR"
        console.log("Start组件 hasValidApprover:", hasValidApprover,
                    "assignee:", approvalInfoObj.assignee,
                    "candidateUsers:", approvalInfoObj.candidateUsers,
                    "candidateGroups:", approvalInfoObj.candidateGroups,
                    "dataType:", approvalInfoObj.dataType)
        if (!hasValidApprover) {
          console.log("Start组件: 审批人信息无效")
          this.$modal.msgError("审批人设置不完整，请先在任务设置页面设置审批人")
          // 延迟关闭，确保错误提示能显示
          this.$nextTick(() => {
            this.handleClose()
          })
          return
        }
        // 生成审批人显示文本
        this.approvalInfoText = this.formatApprovalInfo(approvalInfoObj)
        console.log("Start组件: 验证通过，审批人文本:", this.approvalInfoText)
        // 如果 text 字段不存在，根据用户ID查询用户信息
        if (!approvalInfoObj.text || String(approvalInfoObj.text).trim() === "") {
          this.fetchUserNames(approvalInfoObj).then(userNames => {
            if (userNames) {
              this.approvalInfoText = userNames
            }
          })
        }
      } catch (e) {
        console.error("Start组件: 解析approvalInfo失败:", e)
        this.$modal.msgError("审批人设置不完整，请先在任务设置页面设置审批人")
        // 延迟关闭，确保错误提示能显示
        this.$nextTick(() => {
          this.handleClose()
        })
        return
      }
    },
    // 格式化审批人信息用于显示
    formatApprovalInfo(approvalInfoObj) {
      if (approvalInfoObj.dataType === "INITIATOR") {
        return "发起人的直属上级"
      }
      // 优先使用 text 字段（保存的是用户名），如果没有再根据ID查询
      if (approvalInfoObj.text && String(approvalInfoObj.text).trim() !== "") {
        return approvalInfoObj.text
      }
      // 如果没有 text 字段，先返回临时文本，后续会通过 fetchUserNames 异步查询
      if (approvalInfoObj.assignee && String(approvalInfoObj.assignee).trim() !== "") {
        return "查询中..."
      }
      // candidateUsers 可能是字符串（逗号分隔）或数组
      if (approvalInfoObj.candidateUsers) {
        const candidateUsersStr = String(approvalInfoObj.candidateUsers).trim()
        if (candidateUsersStr !== "") {
          return "查询中..."
        }
      }
      // candidateGroups 可能是字符串（逗号分隔）或数组
      if (approvalInfoObj.candidateGroups) {
        const candidateGroupsStr = String(approvalInfoObj.candidateGroups).trim()
        if (candidateGroupsStr !== "") {
          // 如果是数组，使用 join；如果是字符串，直接返回
          if (Array.isArray(approvalInfoObj.candidateGroups)) {
            return candidateGroupsStr.join(", ")
          } else {
            return candidateGroupsStr
          }
        }
      }
      return "已设置"
    },
    // 根据用户ID查询用户名称
    async fetchUserNames(approvalInfoObj) {
      try {
        const userIds = []
        // 收集需要查询的用户ID
        if (approvalInfoObj.assignee && String(approvalInfoObj.assignee).trim() !== "") {
          userIds.push(String(approvalInfoObj.assignee).trim())
        }
        if (approvalInfoObj.candidateUsers) {
          const candidateUsersStr = String(approvalInfoObj.candidateUsers).trim()
          if (candidateUsersStr !== "") {
            // 如果是数组，直接使用；如果是字符串，按逗号分割
            if (Array.isArray(approvalInfoObj.candidateUsers)) {
              userIds.push(...approvalInfoObj.candidateUsers.map(id => String(id).trim()))
            } else {
              userIds.push(...candidateUsersStr.split(",").map(id => id.trim()).filter(id => id !== ""))
            }
          }
        }
        if (userIds.length === 0) {
          return null
        }
        // 去重
        const uniqueUserIds = [...new Set(userIds)]
        // 并行查询所有用户信息
        const userPromises = uniqueUserIds.map(userId => {
          return getUser(userId).then(res => {
            // 根据实际API返回结构调整
            const user = res.data || res
            return user.nickName || user.userName || userId
          }).catch(() => {
            // 查询失败时返回ID
            return userId
          })
        })
        const userNames = await Promise.all(userPromises)
        return userNames.join(", ")
      } catch (error) {
        console.error("查询用户信息失败:", error)
        return null
      }
    },
    // 提交审批
    handleSubmit() {
      // 构建提交数据（简化版，不包含表单数据）
      const variables = {}
      // 如果有info数据，将关键信息加入变量
      if (this.info) {
        Object.keys(this.info).forEach(key => {
          if (this.info[key] !== null && this.info[key] !== undefined) {
            variables[key] = this.info[key]
          }
        })
      }
      
      // processDefId 为可选，如果存在则传递，否则后端会从审批设置中获取
      const definitionId = this.workFlowable?.definitionId
        const url = encodeURIComponent(`${INFO_URL}?taskId=${this.taskId}`)
      startProcessApi(this.taskId, definitionId, url, JSON.stringify(variables)).then((res) => {
        this.$modal.msgSuccess(res.msg || "审批发起成功")
        if (this.getTableData) {
          this.getTableData()
        }
          this.handleClose()
      }).catch((error) => {
        this.$modal.msgError(error.msg || "发起审批失败")
        })
    },
  },
}
</script>

<style lang="scss" scoped>
.approval-confirm {
  .approval-info {
    margin-top: 15px;
    p {
      margin: 8px 0;
      line-height: 1.8;
    }
  }
  .action-buttons {
    margin-top: 20px;
    text-align: right;
    .el-button {
      margin-left: 10px;
    }
  }
}
</style>
