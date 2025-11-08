<template>
  <div class="app-container">
    <el-card class="box-card" shadow="hover">
      <div slot="header" class="clearfix">
        <span>简化审批</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="goBack">返回</el-button>
      </div>
      <el-row>
        <el-col :span="20" :offset="2">
          <el-form ref="approvalForm" :model="approvalForm" :rules="rules" label-width="120px">
            <el-form-item label="审批任务标题">
              <span>{{ taskInfo.title || '任务审批' }}</span>
            </el-form-item>
            <el-form-item label="发起人">
              <span>{{ taskInfo.initiatorName || '-' }}</span>
            </el-form-item>
            <el-form-item label="审批意见" prop="comment">
              <el-input 
                type="textarea" 
                :rows="5" 
                v-model="approvalForm.comment" 
                placeholder="请输入审批意见" 
              />
            </el-form-item>
            <el-form-item label="抄送人">
              <el-tag
                :key="index"
                v-for="(item, index) in copyUser"
                closable
                :disable-transitions="false"
                @close="handleCloseCopyUser(item)"
                style="margin-right: 10px;"
              >
                {{ item.nickName }}
              </el-tag>
              <el-button
                class="button-new-tag"
                type="primary"
                icon="el-icon-plus"
                size="mini"
                circle
                @click="onSelectCopyUsers"
              />
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
      <el-row :gutter="10" type="flex" justify="center" style="margin-top: 20px;">
        <el-col :span="1.5">
          <el-button 
            icon="el-icon-circle-check" 
            type="success" 
            @click="handleApprove"
            :loading="loading"
          >通过</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button 
            icon="el-icon-circle-close" 
            type="danger" 
            @click="handleReject"
            :loading="loading"
          >拒绝</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 业务详情链接 -->
    <el-card class="box-card" shadow="hover" style="margin-top: 20px;" v-if="taskInfo.url">
      <div slot="header" class="clearfix">
        <span>业务详情</span>
      </div>
      <el-button type="primary" @click="viewBusinessDetail">查看业务详情</el-button>
    </el-card>

    <!-- 用户选择对话框 -->
    <el-dialog :title="userData.title" :visible.sync="userData.open" width="60%" append-to-body>
      <el-row type="flex" :gutter="20">
        <!--部门数据-->
        <el-col :span="5">
          <el-card shadow="never" style="height: 100%">
            <div slot="header">
              <span>部门列表</span>
            </div>
            <div class="head-container">
              <el-input
                v-model="deptName"
                placeholder="请输入部门名称"
                clearable
                size="small"
                prefix-icon="el-icon-search"
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
        <el-col :span="18">
          <el-table
            ref="userTable"
            height="500"
            v-loading="userLoading"
            :data="userList"
            highlight-current-row
            @selection-change="handleSelectionChange"
          >
            <el-table-column width="55" type="selection" />
            <el-table-column label="用户名" align="center" prop="nickName" />
            <el-table-column label="手机" align="center" prop="phonenumber" />
            <el-table-column label="部门" align="center" prop="dept.deptName" />
          </el-table>
          <pagination
            :total="total"
            :page.sync="queryParams.pageNum"
            :limit.sync="queryParams.pageSize"
            @pagination="getList"
          />
        </el-col>
      </el-row>
      <span slot="footer" class="dialog-footer">
        <el-button @click="userData.open = false">取 消</el-button>
        <el-button type="primary" @click="submitUserData">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { completeSimplifiedApproval } from "@/api/workflow/task"
import { selectUser, deptTreeSelect } from "@/api/system/user"

export default {
  name: "SimplifiedApproval",
  data() {
    return {
      loading: false,
      approvalTaskId: null,
      taskInfo: {
        title: '',
        initiatorName: '',
        url: ''
      },
      approvalForm: {
        comment: '',
        copyUserIds: ''
      },
      rules: {
        comment: [
          { required: true, message: '请输入审批意见', trigger: 'blur' }
        ]
      },
      // 抄送人相关
      copyUser: [],
      userMultipleSelection: [],
      userData: {
        title: "",
        type: "",
        open: false,
      },
      // 用户选择对话框相关
      deptName: undefined,
      deptOptions: undefined,
      userLoading: false,
      userList: null,
      deptProps: {
        children: "children",
        label: "label",
      },
      queryParams: {
        deptId: undefined,
        pageNum: 1,
        pageSize: 10,
      },
      total: 0,
    }
  },
  created() {
    // 从路由参数中获取审批任务ID
    this.approvalTaskId = this.$route.query.approvalTaskId
    if (!this.approvalTaskId) {
      this.$modal.msgError('缺少审批任务ID')
      this.goBack()
      return
    }
    
    // 从路由参数中获取任务信息
    if (this.$route.query.title) {
      this.taskInfo.title = decodeURIComponent(this.$route.query.title)
    }
    if (this.$route.query.initiatorName) {
      this.taskInfo.initiatorName = decodeURIComponent(this.$route.query.initiatorName)
    }
    if (this.$route.query.url) {
      this.taskInfo.url = decodeURIComponent(this.$route.query.url)
    }
    
    // TODO: 如果需要，可以从后端获取审批任务详情
    // this.getTaskInfo()
  },
  methods: {
    // 通过审批
    handleApprove() {
      this.$refs.approvalForm.validate((valid) => {
        if (valid) {
          this.submitApproval(true)
        }
      })
    },
    
    // 拒绝审批
    handleReject() {
      this.$refs.approvalForm.validate((valid) => {
        if (valid) {
          this.$modal.confirm('拒绝审批单流程会终止，是否继续？').then(() => {
            this.submitApproval(false)
          }).catch(() => {})
        }
      })
    },
    
    // 提交审批
    submitApproval(approved) {
      this.loading = true
      const data = {
        approvalTaskId: this.approvalTaskId,
        approved: approved,
        comment: this.approvalForm.comment,
        copyUserIds: this.approvalForm.copyUserIds
      }
      
      completeSimplifiedApproval(data).then(response => {
        this.$modal.msgSuccess(approved ? '审批通过' : '审批已拒绝')
        this.loading = false
        // 返回待办列表
        this.goBack()
      }).catch(error => {
        this.$modal.msgError(error.msg || (approved ? '审批通过失败' : '审批拒绝失败'))
        this.loading = false
      })
    },
    
    // 选择抄送人
    onSelectCopyUsers() {
      this.userMultipleSelection = this.copyUser
      this.userData.title = "添加抄送人"
      this.userData.type = "copy"
      this.getTreeSelect()
      this.getList()
      this.userData.open = true
    },
    
    // 关闭抄送人标签
    handleCloseCopyUser(tag) {
      const index = this.copyUser.findIndex(item => item.userId === tag.userId)
      if (index > -1) {
        this.copyUser.splice(index, 1)
        // 更新 copyUserIds
        if (this.copyUser && this.copyUser.length > 0) {
          const userIds = this.copyUser.map(item => item.userId)
          this.approvalForm.copyUserIds = userIds.join(",")
        } else {
          this.approvalForm.copyUserIds = ""
        }
      }
    },
    
    // 用户选择对话框相关方法
    getTreeSelect() {
      deptTreeSelect().then((response) => {
        this.deptOptions = response.data
      })
    },
    
    getList() {
      this.userLoading = true
      selectUser(this.queryParams).then((response) => {
        this.userList = response.rows
        this.total = response.total
        this.toggleSelection(this.userMultipleSelection)
        this.userLoading = false
      })
    },
    
    filterNode(value, data) {
      if (!value) return true
      return data.label.indexOf(value) !== -1
    },
    
    handleNodeClick(data) {
      this.queryParams.deptId = data.id
      this.getList()
    },
    
    handleSelectionChange(selection) {
      this.userMultipleSelection = selection
    },
    
    toggleSelection(selection) {
      if (selection && selection.length > 0) {
        this.$nextTick(() => {
          selection.forEach((item) => {
            let row = this.userList.find((k) => k.userId === item.userId)
            if (row) {
              this.$refs.userTable.toggleRowSelection(row)
            }
          })
        })
      } else {
        this.$nextTick(() => {
          this.$refs.userTable.clearSelection()
        })
      }
    },
    
    submitUserData() {
      if (!this.userMultipleSelection || this.userMultipleSelection.length <= 0) {
        this.$modal.msgError("请选择用户")
        return false
      }
      // 设置抄送人
      this.copyUser = this.userMultipleSelection
      const userIds = this.copyUser.map((item) => item.userId)
      this.approvalForm.copyUserIds = userIds.join(",")
      this.userData.open = false
    },
    
    // 查看业务详情
    viewBusinessDetail() {
      if (!this.taskInfo.url) {
        this.$modal.msgWarning('业务详情URL不存在')
        return
      }
      
      // 解析URL并跳转
      try {
        let routePath = this.taskInfo.url
        let routeQuery = {}
        
        // 如果是完整的HTTP/HTTPS URL，需要提取路径和查询参数
        if (this.taskInfo.url.startsWith('http://') || this.taskInfo.url.startsWith('https://')) {
          const urlObj = new URL(this.taskInfo.url)
          routePath = urlObj.pathname
          urlObj.searchParams.forEach((value, key) => {
            routeQuery[key] = value
          })
        } else if (this.taskInfo.url.includes('?')) {
          const [path, queryString] = this.taskInfo.url.split('?')
          routePath = path
          const params = new URLSearchParams(queryString)
          params.forEach((value, key) => {
            routeQuery[key] = value
          })
        }
        
        // 确保路径以 / 开头
        if (!routePath.startsWith('/')) {
          routePath = '/' + routePath
        }
        
        this.$router.push({
          path: routePath,
          query: routeQuery
        })
      } catch (err) {
        console.error('URL解析失败:', err)
        this.$modal.msgError('无法跳转到业务详情页')
      }
    },
    
    // 返回
    goBack() {
      // 关闭当前标签页并返回上一页
      this.$tab.closePage(this.$route)
      // 使用 router.back() 返回上一页，如果失败则跳转到首页
      this.$router.back()
    }
  }
}
</script>

<style lang="scss" scoped>
.box-card {
  margin-bottom: 20px;
}

.head-container {
  padding: 10px;
}

.button-new-tag {
  margin-left: 10px;
}
</style>

