<template>
  <div>
    <el-dialog title="任务设置" :visible.sync="visible" width="1200px" :before-close="handleClose" @open="handleOpen">
      <el-tabs tab-position="left" v-model="tabActiveName">
        <el-tab-pane label="交付物模板" name="交付物模板" class="deliverable-wrapper" v-if="taskId">
          <el-card shadow="never">
            <el-upload
              :action="uploadFileUrl"
              :headers="uploadFileHeaders"
              :data="uploadTemplateFileData"
              :show-file-list="false"
              :auto-upload="true"
              :before-upload="handleBeforeUpload"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
            >
              <el-button type="text">上传交付物模板</el-button>
            </el-upload>
          </el-card>
          <el-card shadow="never">
            <el-table :data="templateList">
              <el-table-column label="文件名" prop="fileName" align="center" show-overflow-tooltip />
              <el-table-column label="上传人" prop="nickName" align="center" show-overflow-tooltip />
              <el-table-column label="文件大小（KB）" prop="fileSize" align="center" show-overflow-tooltip />
              <el-table-column label="上传时间" prop="createdTime" align="center" show-overflow-tooltip />
              <el-table-column label="操作" align="center" width="100">
                <template slot-scope="scope">
                  <el-button type="text" @click="handleDownload(scope.row)">下载</el-button>
                  <el-button type="text" @click="handleDelete(scope.row)" style="color: #f56c6c">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>
        <el-tab-pane label="审批设置" name="审批设置" class="approval-wrapper">
          <el-card shadow="never">
            <el-radio-group v-model="isApproval">
              <el-radio :label="false">无需审批</el-radio>
              <el-radio :label="true">需要审批</el-radio>
            </el-radio-group>
          </el-card>
          <el-card shadow="never" v-show="isApproval">
            <h4><b>审批人设置</b></h4>
            <el-radio-group v-model="approvalDataType" @change="changeApprovalDataType" style="margin-bottom: 20px;">
              <el-radio label="USERS">指定用户</el-radio>
              <el-radio label="ROLES">角色</el-radio>
              <el-radio label="DEPTS">部门</el-radio>
              <el-radio label="INITIATOR">发起人</el-radio>
            </el-radio-group>
            
            <!-- 指定用户 -->
            <div v-if="approvalDataType === 'USERS'" style="margin-top: 15px;">
              <el-tag v-for="userText in selectedUser.text" :key="userText" effect="plain" style="margin-right: 10px; margin-bottom: 10px;">
                {{ userText }}
              </el-tag>
              <div style="margin-top: 10px;">
                <el-button size="small" type="primary" icon="el-icon-plus" @click="onSelectUsers()">添加用户</el-button>
              </div>
            </div>
            
            <!-- 角色 -->
            <div v-if="approvalDataType === 'ROLES'" style="margin-top: 15px;">
              <el-select v-model="selectedRoleIds" multiple size="small" placeholder="请选择角色" style="width: 100%;" @change="changeSelectRoles">
                  <el-option
                  v-for="item in roleOptions"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="`ROLE${item.roleId}`"
                  :disabled="item.status === 1"
                  >
                  </el-option>
                </el-select>
            </div>
            
            <!-- 部门 -->
            <div v-if="approvalDataType === 'DEPTS'" style="margin-top: 15px;">
              <tree-select
                :width="320"
                :height="400"
                size="small"
                :data="deptTreeData"
                :defaultProps="deptProps"
                multiple
                clearable
                checkStrictly
                nodeKey="id"
                :checkedKeys="selectedDeptIds"
                @change="checkedDeptChange"
              >
              </tree-select>
            </div>
            
            <!-- 发起人 -->
            <div v-if="approvalDataType === 'INITIATOR'" style="margin-top: 15px; color: #909399;">
              审批人将设置为流程发起人
            </div>

            <!-- 候选用户弹窗 -->
            <el-dialog title="候选用户" :visible.sync="userOpen" width="60%" append-to-body>
              <el-row type="flex" :gutter="20">
                <!--部门数据-->
                <el-col :span="7">
                  <el-card shadow="never" style="height: 100%">
                    <div class="head-container">
                      <el-input
                        @input="getUserList"
                        v-model.trim="userQueryParams.nickName"
                        placeholder="筛选用户"
                        clearable
                        size="small"
                        prefix-icon="el-icon-search"
                        style="margin-bottom: 20px"
                      />
                      <el-tree
                        :data="deptOptions"
                        :props="deptProps"
                        :expand-on-click-node="false"
                        :filter-node-method="filterNode"
                        ref="tree"
                        default-expand-all
                        @node-click="handleNodeClick"
                      />
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="17">
                  <el-table
                    ref="multipleTable"
                    height="600"
                    :data="userTableList"
                    border
                    @selection-change="handleSelectionChange"
                    @row-click="handleRowClick"
                    highlight-current-row
                  >
                    <el-table-column type="index" label="序号" align="center" width="50" />
                    <el-table-column label="用户名" align="center" prop="nickName" />
                    <el-table-column label="部门" align="center" prop="dept.deptName" />
            </el-table>
            <pagination
                    :total="userTotal"
                    :page.sync="userQueryParams.pageNum"
                    :limit.sync="userQueryParams.pageSize"
                    @pagination="getUserList"
                  />
                </el-col>
              </el-row>
              <div slot="footer" class="dialog-footer">
                <el-card shadow="never" style="margin-bottom: 20px; text-align: left">
                  当前已选用户（点击列表即可选择）：
                  <el-tag
                    v-for="(item, index) in selectedUserDate"
                    :key="index"
                    closable
                    @close="removeSelectedUserData(index)"
                    style="margin: 2px"
                  >
                    {{ item.nickName }}
                  </el-tag>
                </el-card>
                <el-button type="primary" @click="handleTaskUserComplete">确 定</el-button>
                <el-button @click="userOpen = false">取 消</el-button>
              </div>
            </el-dialog>
          </el-card>
        </el-tab-pane>
      </el-tabs>
      <template slot="footer">
        <el-button @click="handleClose">取 消</el-button>
        <el-button type="primary" @click="handleCreate">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getToken } from "@/utils/auth"
import { updateApprovalSetApi } from "@/api/pmhub-project/my-task"
import { getFileListApi, deleteFileApi } from "@/api/pmhub-project/my-project.js"
import { listUser, deptTreeSelect } from "@/api/system/user"
import { listRole } from "@/api/system/role"
import TreeSelect from "@/components/TreeSelect"

export default {
  name: "SetTaskDialog",
  components: {
    TreeSelect,
  },
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    // 没有 taskId 则代表是从 "新建任务" 调用该组件
    // 有 taskId 代表是从 "任务设置" 调用该组件
    taskId: {
      type: String,
      default: "",
    },
    // 后端返回的审批设置数据（有 taskId 时才会用到 workFlowable）
    workFlowable: {
      type: Object,
    },
    // 重新获取任务列表数据
    getTableData: {
      type: Function,
    },
  },
  watch: {
    taskId: {
      handler(val) {
        this.tabActiveName = val ? "交付物模板" : "审批设置"
      },
      immediate: true,
    },
  },
  data() {
    return {
      tabActiveName: "",

      /** 交付物模板模块 */
      uploadFileUrl: process.env.VUE_APP_BASE_API + "/project/file/upload",
      uploadFileHeaders: { Authorization: "Bearer " + getToken() },
      uploadTemplateFileData: {
        id: "",
        type: "template",
      },
      templateList: [],

      /** 审批设置模块 */
      isApproval: false, // 是否需要审批
      // 审批人设置
      approvalDataType: "USERS", // 审批人类型：USERS, ROLES, DEPTS, INITIATOR
      selectedUser: {
        ids: [],
        text: [],
      },
      selectedRoleIds: [],
      selectedDeptIds: [],
      roleOptions: [],
      deptTreeData: [],
      deptProps: {
        children: "children",
        label: "label",
      },
      // 用户选择相关
      userOpen: false,
      deptOptions: [],
      userTableList: [],
      userTotal: 0,
      selectedUserDate: [],
      userQueryParams: {
        pageNum: 1,
        pageSize: 10,
        nickName: undefined,
        deptId: undefined,
      },
    }
  },
  methods: {
    handleCreate() {
      if (this.isApproval) {
        // 验证审批人设置
        if (this.approvalDataType === "USERS" && (!this.selectedUser.ids || this.selectedUser.ids.length === 0)) {
          this.$modal.msgWarning("请选择审批人")
          return
        }
        if (this.approvalDataType === "ROLES" && (!this.selectedRoleIds || this.selectedRoleIds.length === 0)) {
          this.$modal.msgWarning("请选择角色")
          return
        }
        if (this.approvalDataType === "DEPTS" && (!this.selectedDeptIds || this.selectedDeptIds.length === 0)) {
          this.$modal.msgWarning("请选择部门")
        return
        }
      }
      if (this.taskId) {
        // 构建请求参数
        const requestData = {
          approved: this.isApproval ? "0" : "1",
          definitionId: "", // 不再使用流程定义ID
          deploymentId: "", // 不再使用部署ID
          taskId: this.taskId,
        }
        
        // 只有需要审批时才构建审批人信息
        if (this.isApproval) {
          const approvalInfo = {
            dataType: this.approvalDataType,
            assignee: "",
            candidateUsers: "",
            candidateGroups: "",
            text: "",
          }
          if (this.approvalDataType === "USERS") {
            if (this.selectedUser.ids.length === 1) {
              approvalInfo.assignee = this.selectedUser.ids[0]
              approvalInfo.text = this.selectedUser.text[0]
            } else {
              approvalInfo.candidateUsers = this.selectedUser.ids.join(",")
              approvalInfo.text = this.selectedUser.text.join(",")
            }
          } else if (this.approvalDataType === "ROLES") {
            approvalInfo.candidateGroups = this.selectedRoleIds.join(",")
            const roleNames = this.roleOptions
              .filter((k) => this.selectedRoleIds.indexOf(`ROLE${k.roleId}`) >= 0)
              .map((k) => k.roleName)
            approvalInfo.text = roleNames.join(",")
          } else if (this.approvalDataType === "DEPTS") {
            approvalInfo.candidateGroups = this.selectedDeptIds.join(",")
            // 获取部门名称
            const deptNames = this.getDeptNames(this.selectedDeptIds)
            approvalInfo.text = deptNames.join(",")
          } else if (this.approvalDataType === "INITIATOR") {
            approvalInfo.assignee = "${initiator}"
            approvalInfo.text = "流程发起人"
          }
          requestData.approvalInfo = JSON.stringify(approvalInfo)
        } else {
          // 无需审批时，不传递 approvalInfo 或传递 null
          requestData.approvalInfo = null
        }

        updateApprovalSetApi(requestData).then(() => {
          this.$modal.msgSuccess("设置成功")
          this.getTableData()
          this.handleClose()
        }).catch((error) => {
          this.$modal.msgError(error.msg || "设置失败，请重试")
        })
      } else {
        // 新建任务时的处理
        const approvalInfo = {
          dataType: this.approvalDataType,
          assignee: "",
          candidateUsers: "",
          candidateGroups: "",
          text: "",
        }
        if (this.approvalDataType === "USERS") {
          if (this.selectedUser.ids.length === 1) {
            approvalInfo.assignee = this.selectedUser.ids[0]
            approvalInfo.text = this.selectedUser.text[0]
          } else {
            approvalInfo.candidateUsers = this.selectedUser.ids.join(",")
            approvalInfo.text = this.selectedUser.text.join(",")
          }
        } else if (this.approvalDataType === "ROLES") {
          approvalInfo.candidateGroups = this.selectedRoleIds.join(",")
          const roleNames = this.roleOptions
            .filter((k) => this.selectedRoleIds.indexOf(`ROLE${k.roleId}`) >= 0)
            .map((k) => k.roleName)
          approvalInfo.text = roleNames.join(",")
        } else if (this.approvalDataType === "DEPTS") {
          approvalInfo.candidateGroups = this.selectedDeptIds.join(",")
          const deptNames = this.getDeptNames(this.selectedDeptIds)
          approvalInfo.text = deptNames.join(",")
        } else if (this.approvalDataType === "INITIATOR") {
          approvalInfo.assignee = "${initiator}"
          approvalInfo.text = "流程发起人"
        }

        this.$emit("change", {
          isApproval: this.isApproval,
          approvalInfo: approvalInfo,
        })
      }
      this.handleClose()
    },
    handleClose() {
      this.userQueryParams.pageNum = 1
      this.$emit("update:visible", false)
    },
    handleOpen() {
      // 初始化审批人设置相关的数据
      this.getRoleOptions()
      this.getDeptTreeData()
      // 如果是从任务设置进入
      if (this.taskId) {
        // 交付物模板列表
        this.getTemplateList()
        // 赋值附件上传所需的id
        this.uploadTemplateFileData.id = this.taskId
        // 回显以前的审批设置
        this.isApproval = this.workFlowable?.approved === "0" ? true : false
        // 如果有保存的审批人信息，回显
        if (this.workFlowable?.approvalInfo) {
          try {
            const approvalInfo = JSON.parse(this.workFlowable.approvalInfo)
            this.approvalDataType = approvalInfo.dataType || "USERS"
            if (approvalInfo.dataType === "USERS") {
              if (approvalInfo.assignee) {
                this.selectedUser.ids = [approvalInfo.assignee]
                this.selectedUser.text = [approvalInfo.text]
              } else if (approvalInfo.candidateUsers) {
                this.selectedUser.ids = approvalInfo.candidateUsers.split(",")
                this.selectedUser.text = approvalInfo.text ? approvalInfo.text.split(",") : []
              }
            } else if (approvalInfo.dataType === "ROLES") {
              this.selectedRoleIds = approvalInfo.candidateGroups ? approvalInfo.candidateGroups.split(",") : []
            } else if (approvalInfo.dataType === "DEPTS") {
              this.selectedDeptIds = approvalInfo.candidateGroups ? approvalInfo.candidateGroups.split(",") : []
            }
          } catch (e) {
            console.error("解析审批人信息失败", e)
          }
        }
      }
    },

    /** 交付物模板模块 */
    handleBeforeUpload(file) {
      // 上传文件之前触发
      this.$modal.loading("上传文件中...")
      const isLt50M = file.size / 1024 / 1024 < 50
      if (!isLt50M) {
        this.$message.error("上传文件大小不能超过 50MB!")
        this.$modal.closeLoading()
      }
      return isLt50M
    },
    handleUploadError() {
      // 上传失败时触发
      this.$modal.closeLoading()
      this.$modal.msgError("上传文件失败")
    },
    handleUploadSuccess(res) {
      // 上传成功时触发
      this.$modal.closeLoading()
      if (res.code === 200) {
        this.$modal.msgSuccess("上传文件成功")
        this.getTemplateList()
      } else {
        this.$modal.msgError(res.msg || "上传文件异常")
      }
    },
    getTemplateList() {
      getFileListApi({
        pageNum: 1,
        pageSize: 1,
        fileName: undefined,
        id: this.taskId,
        type: "template",
      })
        .then((res) => {
          this.templateList = res.data.list
        })
        .catch(() => {
          this.templateList = []
        })
    },
    handleDownload(row) {
      this.download(
        "/project/file/download",
        {
          projectFileId: row.projectFileId,
          fileUrl: row.fileUrl,
        },
        row.fileName
      )
    },
    handleDelete(row) {
      const data = {
        fileVOList: [
          {
            projectFileId: row.projectFileId,
            fileUrl: row.fileUrl,
          },
        ],
      }
      this.$modal
        .confirm(`是否确认删除文件：${row.fileName}？`)
        .then(() => {
          return deleteFileApi(data)
        })
        .then(() => {
          this.$modal.msgSuccess("删除成功")
          this.getTemplateList()
        })
        .catch(() => {})
    },

    /** 审批设置模块 */
    // 查询角色列表
    getRoleOptions() {
      if (!this.roleOptions || this.roleOptions.length <= 0) {
        listRole().then((response) => (this.roleOptions = response.rows))
      }
    },
    // 查询部门树结构
    getDeptTreeData() {
      function refactorTree(data) {
        return data.map((node) => {
          let treeData = { id: `DEPT${node.id}`, label: node.label, parentId: node.parentId, weight: node.weight }
          if (node.children && node.children.length > 0) {
            treeData.children = refactorTree(node.children)
          }
          return treeData
        })
      }
      return new Promise((resolve) => {
        if (!this.deptTreeData || this.deptTreeData.length <= 0) {
          deptTreeSelect()
            .then((response) => {
              const deptOptions = response.data
              this.deptTreeData = refactorTree(deptOptions)
              resolve()
            })
            .catch(() => {
              resolve()
            })
        } else {
          resolve()
        }
      })
    },
    // 获取部门名称
    getDeptNames(deptIds) {
      const names = []
      const findDeptName = (tree, id) => {
        for (let node of tree) {
          if (node.id === id) {
            return node.label
          }
          if (node.children && node.children.length > 0) {
            const found = findDeptName(node.children, id)
            if (found) return found
          }
        }
        return null
      }
      deptIds.forEach((id) => {
        const name = findDeptName(this.deptTreeData, id)
        if (name) names.push(name)
      })
      return names
    },
    // 改变审批人类型
    changeApprovalDataType(val) {
      // 切换类型时清空之前的选择
      if (val !== "USERS") {
        this.selectedUser.ids = []
        this.selectedUser.text = []
      }
      if (val !== "ROLES") {
        this.selectedRoleIds = []
      }
      if (val !== "DEPTS") {
        this.selectedDeptIds = []
      }
      if (val === "ROLES") {
        this.getRoleOptions()
      }
      if (val === "DEPTS") {
        this.getDeptTreeData()
      }
    },
    // 用户选择相关方法
    onSelectUsers() {
      this.selectedUserDate = []
      this.$refs.multipleTable?.clearSelection()
      this.getDeptOptions()
      this.getUserList()
      this.userOpen = true
    },
    getDeptOptions() {
      return new Promise((resolve) => {
        if (!this.deptOptions || this.deptOptions.length <= 0) {
          deptTreeSelect().then((response) => {
            this.deptOptions = response.data
            resolve()
          })
        } else {
          resolve()
        }
      })
    },
    getUserList() {
      if (this.userQueryParams.nickName) {
        this.userQueryParams.deptId = undefined
      }
      listUser(this.userQueryParams).then((response) => {
        this.userTableList = response.rows
        this.userTotal = response.total
      })
    },
    filterNode(value, data) {
      if (!value) return true
      return data.label.indexOf(value) !== -1
    },
    handleNodeClick(data) {
      this.userQueryParams.deptId = data.id
      this.getUserList()
    },
    handleSelectionChange(selection) {
      this.selectedUserDate = selection
    },
    handleRowClick(row) {
      this.selectedUserDate.push(row)
      this.selectedUserDate = this.noRepeat(this.selectedUserDate)
    },
    noRepeat(arr) {
      const newArr = JSON.parse(JSON.stringify(arr))
      for (let i = 0; i < newArr.length - 1; i++) {
        for (let j = i + 1; j < newArr.length; j++) {
          if (newArr[i].userId === newArr[j].userId) {
            newArr.splice(j, 1)
            j--
          }
        }
      }
      return newArr
    },
    removeSelectedUserData(index) {
      this.selectedUserDate.splice(index, 1)
    },
    handleTaskUserComplete() {
      if (!this.selectedUserDate || this.selectedUserDate.length <= 0) {
        this.$modal.msgError("请选择用户")
        return
      }
      this.selectedUser.text = this.selectedUserDate.map((k) => k.nickName) || []
      this.selectedUser.ids = this.selectedUserDate.map((k) => k.userId) || []
      this.userOpen = false
    },
    changeSelectRoles(val) {
      // 角色选择变化处理
    },
    checkedDeptChange(checkedIds) {
      this.selectedDeptIds = checkedIds
    },
  },
  mounted() {},
}
</script>

<style scoped lang="scss">
::v-deep .el-dialog {
  .el-tabs__content {
    height: 600px;
    overflow: auto;
  }
}

.el-card {
  margin-bottom: 10px;
}
</style>
