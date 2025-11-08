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
            <p v-if="info.title"><strong>单据标题：</strong>{{ info.title }}</p>
            <p v-if="info.id"><strong>单据ID：</strong>{{ info.id }}</p>
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
let INFO_PATH = ""
let INFO_URL = ""

export default {
  name: "Start",
  props: {
    visible: {
      type: Boolean,
      required: true,
    },
    id: {
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
    // 接口
    startProcessApi: {
      type: Function,
      required: true,
    },
    // 当前单据详情数据
    info: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      approvalInfoText: "",
    }
  },
  created() {
    INFO_PATH = `${window.location.pathname}/info`
    INFO_URL = `${window.location.origin}${INFO_PATH}`
  },
  methods: {
    handleClose() {
      this.$emit("update:visible", false)
    },
    handleOpen() {
      this.initData()
    },
    initData() {
      // 验证审批人设置是否存在
      const approvalInfo = this.workFlowable?.approvalInfo
      if (!approvalInfo) {
        this.$modal.msgError("审批人设置不完整，请先设置审批人")
        this.handleClose()
        return
      }
      // 解析审批人信息并验证
      try {
        const approvalInfoObj = JSON.parse(approvalInfo)
        // 检查是否有有效的审批人信息
        const hasValidApprover = approvalInfoObj.assignee || 
                                 approvalInfoObj.candidateUsers || 
                                 approvalInfoObj.candidateGroups ||
                                 approvalInfoObj.dataType === "INITIATOR"
        if (!hasValidApprover) {
          this.$modal.msgError("审批人设置不完整，请先设置审批人")
          this.handleClose()
          return
        }
        // 生成审批人显示文本
        this.approvalInfoText = this.formatApprovalInfo(approvalInfoObj)
      } catch (e) {
        this.$modal.msgError("审批人设置不完整，请先设置审批人")
        this.handleClose()
        return
      }
    },
    // 格式化审批人信息用于显示
    formatApprovalInfo(approvalInfoObj) {
      if (approvalInfoObj.dataType === "INITIATOR") {
        return "发起人的直属上级"
      }
      if (approvalInfoObj.assignee) {
        return approvalInfoObj.assignee
      }
      if (approvalInfoObj.candidateUsers && approvalInfoObj.candidateUsers.length > 0) {
        return approvalInfoObj.candidateUsers.join(", ")
      }
      if (approvalInfoObj.candidateGroups && approvalInfoObj.candidateGroups.length > 0) {
        return approvalInfoObj.candidateGroups.join(", ")
      }
      return "已设置"
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
      const url = encodeURIComponent(INFO_URL)
      this.startProcessApi(this.id, definitionId, url, JSON.stringify(variables)).then((res) => {
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
