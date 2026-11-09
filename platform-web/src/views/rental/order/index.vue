<template>
  <div class="page-container">
    <el-card class="page-search" shadow="never">
      <el-form ref="queryFormRef" :model="params" :inline="true">
        <el-form-item label="关键字" prop="keywords">
          <el-input
            v-model="params.keywords"
            placeholder="订单号 / 身份证号 / 姓名 / 手机号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="租户主体" prop="tenant">
          <el-select v-model="params.tenant" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="item in tenantOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="订单状态" prop="status">
          <el-input v-model="params.status" placeholder="请输入订单状态" clearable />
        </el-form-item>
        <el-form-item label="申请状态" prop="applyStatus">
          <el-select v-model="params.applyStatus" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="item in applyStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="租赁类型" prop="zlType">
          <el-select v-model="params.zlType" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="item in zlTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道类型" prop="channelType">
          <el-input v-model="params.channelType" placeholder="请输入渠道类型" clearable />
        </el-form-item>
        <el-form-item label="创建时间" prop="createTimeStart">
          <el-date-picker
            v-model="params.createTimeStart"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="开始时间"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="至" prop="createTimeEnd">
          <el-date-picker
            v-model="params.createTimeEnd"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="结束时间"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card ref="tableWrapperRef" class="page-content" shadow="never">
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-button
            v-hasPerm="['rental:order:create']"
            type="primary"
            :disabled="isSaving || deletingId !== undefined"
            @click="openDialog()"
          >
            新增
          </el-button>
        </div>
        <div class="page-toolbar__right">
          <el-tooltip content="刷新" placement="top">
            <el-button class="page-icon-btn" @click="handleQuery">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="全屏" placement="top">
            <el-button class="page-icon-btn" @click="toggleFullscreen">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table
          v-loading="loading"
          :data="list"
          class="page-table"
          height="100%"
          highlight-current-row
          border
        >
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column key="orderId" label="订单号" prop="orderId" min-width="180" />
          <el-table-column key="realName" label="客户姓名" prop="realName" min-width="100" />
          <el-table-column key="mobile" label="手机号" prop="mobile" min-width="120" />
          <el-table-column key="applyStatus" label="申请状态" min-width="110">
            <template #default="scope">{{ applyStatusLabel(scope.row.applyStatus) }}</template>
          </el-table-column>
          <el-table-column key="zlType" label="租赁类型" min-width="100">
            <template #default="scope">{{ zlTypeLabel(scope.row.zlType) }}</template>
          </el-table-column>
          <el-table-column key="productName" label="产品名称" prop="productName" min-width="140" />
          <el-table-column key="tenant" label="租户主体" min-width="100">
            <template #default="scope">{{ tenantLabel(scope.row.tenant) }}</template>
          </el-table-column>
          <el-table-column key="channelType" label="渠道类型" prop="channelType" min-width="110" />
          <el-table-column key="creSn" label="身份证号" prop="creSn" min-width="160" />
          <el-table-column key="status" label="订单状态" prop="status" min-width="110" />
          <el-table-column key="productId" label="产品 ID" prop="productId" min-width="90" />
          <el-table-column key="addressType" label="收货方式" min-width="90">
            <template #default="scope">{{ addressTypeLabel(scope.row.addressType) }}</template>
          </el-table-column>
          <el-table-column key="number" label="数量" prop="number" min-width="70" />
          <el-table-column key="amount" label="订单金额" prop="amount" min-width="100" />
          <el-table-column key="monthAmount" label="月租金" prop="monthAmount" min-width="100" />
          <el-table-column key="deposit" label="押金" prop="deposit" min-width="90" />
          <el-table-column key="merchantId" label="商户 ID" prop="merchantId" min-width="90" />
          <el-table-column key="usePhone" label="使用手机" prop="usePhone" min-width="120" />
          <el-table-column key="createUser" label="创建人" prop="createUser" min-width="100" />
          <el-table-column key="createTime" label="创建时间" prop="createTime" min-width="160" />
          <el-table-column fixed="right" label="操作" width="160">
            <template #default="scope">
              <el-button
                v-hasPerm="['rental:order:update']"
                type="primary"
                size="small"
                link
                :disabled="isSaving || deletingId !== undefined"
                @click="openDialog(scope.row.orderId)"
              >
                编辑
              </el-button>
              <el-button
                v-hasPerm="['rental:order:delete']"
                type="danger"
                size="small"
                link
                :loading="deletingId === scope.row.orderId"
                :disabled="isSaving || deletingId !== undefined"
                @click="handleDelete(scope.row.orderId)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <pagination
        v-if="total > 0"
        v-model:total="total"
        v-model:page="params.pageNum"
        v-model:limit="params.pageSize"
        @pagination="fetchData"
      />
    </el-card>

    <el-dialog
      v-model="dialogState.visible"
      :title="dialogState.title"
      width="900px"
      :close-on-click-modal="!isSaving"
      :close-on-press-escape="!isSaving"
      :show-close="!isSaving"
      @close="closeDialog"
    >
      <el-alert
        v-if="hasFormLoadError"
        title="订单详情加载失败，请重新打开。"
        type="error"
        :closable="false"
      />
      <el-form
        ref="dataFormRef"
        v-loading="isLoadingForm"
        :model="formData"
        :rules="rules"
        :disabled="isLoadingForm || hasFormLoadError || isSaving"
        label-suffix=":"
        label-width="120px"
      >
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基础信息" name="basic">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="订单号" prop="orderId">
                  <el-input
                    v-model="formData.orderId"
                    :disabled="isEditing"
                    placeholder="留空则自动生成"
                    :maxlength="64"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="租户主体" prop="tenant">
                  <el-select v-model="formData.tenant" placeholder="请选择租户主体" clearable>
                    <el-option
                      v-for="item in tenantOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="订单状态" prop="status">
                  <el-input
                    v-model="formData.status"
                    placeholder="请输入订单状态"
                    :maxlength="64"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="渠道类型" prop="channelType">
                  <el-input
                    v-model="formData.channelType"
                    placeholder="请输入渠道类型"
                    :maxlength="64"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="身份证号" prop="creSn">
                  <el-input v-model="formData.creSn" placeholder="请输入身份证号" :maxlength="64" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="使用手机" prop="usePhone">
                  <el-input
                    v-model="formData.usePhone"
                    placeholder="请输入当前使用手机"
                    :maxlength="32"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="产品 ID" prop="productId">
                  <el-input-number
                    v-model="formData.productId"
                    :controls="false"
                    :precision="0"
                    :min="0"
                    placeholder="请输入产品 ID"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="收货方式" prop="addressType">
                  <el-select v-model="formData.addressType" placeholder="请选择收货方式" clearable>
                    <el-option
                      v-for="item in addressTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="租用数量" prop="number">
                  <el-input-number
                    v-model="formData.number"
                    :controls="false"
                    :precision="0"
                    :min="0"
                    placeholder="请输入租用数量"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="颜色" prop="color">
                  <el-input v-model="formData.color" placeholder="请输入颜色" :maxlength="64" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="备注" prop="remark">
                  <el-input
                    v-model="formData.remark"
                    type="textarea"
                    :rows="2"
                    :maxlength="255"
                    show-word-limit
                    placeholder="请输入备注"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="租赁方案" name="plan">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="合同类型" prop="productContractType">
                  <el-select
                    v-model="formData.productContractType"
                    placeholder="请选择合同类型"
                    clearable
                  >
                    <el-option
                      v-for="item in contractTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="支付方式" prop="payType">
                  <el-select v-model="formData.payType" placeholder="请选择支付方式" clearable>
                    <el-option
                      v-for="item in payTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="借款天数" prop="borrowDays">
                  <el-input-number
                    v-model="formData.borrowDays"
                    :controls="false"
                    :precision="0"
                    :min="0"
                    placeholder="请输入借款天数"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="期数" prop="tenor">
                  <el-input-number
                    v-model="formData.tenor"
                    :controls="false"
                    :precision="0"
                    :min="0"
                    placeholder="请输入期数"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="支付期数" prop="payTenor">
                  <el-input-number
                    v-model="formData.payTenor"
                    :controls="false"
                    :precision="0"
                    :min="0"
                    placeholder="请输入支付期数"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="订单金额" prop="amount">
                  <el-input-number
                    v-model="formData.amount"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="月租金" prop="monthAmount">
                  <el-input-number
                    v-model="formData.monthAmount"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="押金" prop="deposit">
                  <el-input-number
                    v-model="formData.deposit"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="买断价" prop="buyOutPrice">
                  <el-input-number
                    v-model="formData.buyOutPrice"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="首付金额" prop="payFirstAmount">
                  <el-input-number
                    v-model="formData.payFirstAmount"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="首付增加金额" prop="moreFirstAmount">
                  <el-input-number
                    v-model="formData.moreFirstAmount"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="官网价格" prop="officialPrice">
                  <el-input-number
                    v-model="formData.officialPrice"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="补贴金额" prop="subsidyAmount">
                  <el-input-number
                    v-model="formData.subsidyAmount"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="会员费" prop="memberFee">
                  <el-input-number
                    v-model="formData.memberFee"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="碎屏险" prop="brokenFee">
                  <el-input-number
                    v-model="formData.brokenFee"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="维修费" prop="modifyFee">
                  <el-input-number
                    v-model="formData.modifyFee"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="锁机费" prop="lockFee">
                  <el-input-number
                    v-model="formData.lockFee"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="买断标识" prop="buyOutFlag">
                  <el-input
                    v-model="formData.buyOutFlag"
                    placeholder="请输入买断标识"
                    :maxlength="64"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="保持入账" prop="keepEntry">
                  <el-input
                    v-model="formData.keepEntry"
                    placeholder="请输入保持入账标识"
                    :maxlength="8"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="收货与产品" name="delivery">
            <el-row :gutter="16">
              <el-col :span="24">
                <el-form-item label="地址" prop="addressJson">
                  <el-input
                    v-model="formData.addressJson"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入 JSON 格式的地址信息"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="产品 SKU" prop="skuJson">
                  <el-input
                    v-model="formData.skuJson"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入 JSON 格式的产品 SKU"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="交付地址" prop="addressDeliver">
                  <el-input
                    v-model="formData.addressDeliver"
                    placeholder="请输入交付地址"
                    :maxlength="255"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="提货日期" prop="takeDate">
                  <el-date-picker
                    v-model="formData.takeDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="请选择提货日期"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="非首期账单日" prop="accountDate">
                  <el-date-picker
                    v-model="formData.accountDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="请选择账单日"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="职业类型" prop="jobType">
                  <el-input
                    v-model="formData.jobType"
                    placeholder="请输入职业类型"
                    :maxlength="16"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="职业" prop="jobDetail">
                  <el-input v-model="formData.jobDetail" placeholder="请输入职业" :maxlength="16" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否担保" prop="guarantee">
                  <el-input
                    v-model="formData.guarantee"
                    placeholder="请输入是否担保"
                    :maxlength="8"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="商户与结算" name="merchant">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="结算方式" prop="settleType">
                  <el-select v-model="formData.settleType" placeholder="请选择结算方式" clearable>
                    <el-option
                      v-for="item in settleTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="商户 ID" prop="merchantId">
                  <el-input-number
                    v-model="formData.merchantId"
                    :controls="false"
                    :precision="0"
                    :min="0"
                    placeholder="请输入商户 ID"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="结算总金额" prop="merchantAmt">
                  <el-input-number
                    v-model="formData.merchantAmt"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="延期结算金额" prop="merchantAfterAmt">
                  <el-input-number
                    v-model="formData.merchantAfterAmt"
                    :controls="false"
                    :precision="2"
                    :min="0"
                    :step="0.01"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="业代 ID" prop="salesId">
                  <el-input-number
                    v-model="formData.salesId"
                    :controls="false"
                    :precision="0"
                    :min="0"
                    placeholder="请输入业代 ID"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="公共账户 ID" prop="commonId">
                  <el-input-number
                    v-model="formData.commonId"
                    :controls="false"
                    :precision="0"
                    :min="0"
                    placeholder="请输入公共账户 ID"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="扣款渠道" prop="channel">
                  <el-input
                    v-model="formData.channel"
                    placeholder="如 SNXY / ZJXY"
                    :maxlength="255"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="扣款商户" prop="merchantNo">
                  <el-input
                    v-model="formData.merchantNo"
                    placeholder="请输入扣款商户"
                    :maxlength="255"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="首付支付方式" prop="firstPayType">
                  <el-input
                    v-model="formData.firstPayType"
                    placeholder="请输入首付支付方式"
                    :maxlength="128"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="首付来源" prop="firstPaySource">
                  <el-input
                    v-model="formData.firstPaySource"
                    placeholder="请输入首付来源"
                    :maxlength="128"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="审核敏感词" prop="sensitiveMsg">
                  <el-input
                    v-model="formData.sensitiveMsg"
                    placeholder="请输入审核敏感词"
                    :maxlength="128"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="审核与跟进" name="audit">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="审核人" prop="approveUser">
                  <el-input
                    v-model="formData.approveUser"
                    placeholder="请输入审核人"
                    :maxlength="32"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="审核时间" prop="approveTime">
                  <el-date-picker
                    v-model="formData.approveTime"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    placeholder="请选择审核时间"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="完成时间" prop="finishTime">
                  <el-date-picker
                    v-model="formData.finishTime"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    placeholder="请选择完成时间"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否公证" prop="isNotarial">
                  <el-select v-model="formData.isNotarial" placeholder="请选择" clearable>
                    <el-option
                      v-for="item in notarialOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="公证结果" prop="notarialResult">
                  <el-select v-model="formData.notarialResult" placeholder="请选择" clearable>
                    <el-option
                      v-for="item in notarialResultOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="审核通过代码" prop="passCode">
                  <el-input
                    v-model="formData.passCode"
                    placeholder="请输入通过代码"
                    :maxlength="2"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="拒绝码" prop="refuseCode">
                  <el-input
                    v-model="formData.refuseCode"
                    placeholder="请输入拒绝码"
                    :maxlength="64"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="产品状态" prop="returnStatus">
                  <el-input
                    v-model="formData.returnStatus"
                    placeholder="请输入产品状态"
                    :maxlength="8"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="退货时间" prop="backDate">
                  <el-date-picker
                    v-model="formData.backDate"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    placeholder="请选择退货时间"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="退货人" prop="backUser">
                  <el-input
                    v-model="formData.backUser"
                    placeholder="请输入退货人"
                    :maxlength="32"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="领取人" prop="claimUser">
                  <el-input
                    v-model="formData.claimUser"
                    placeholder="请输入领取人"
                    :maxlength="32"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="领取时间" prop="claimTime">
                  <el-date-picker
                    v-model="formData.claimTime"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    placeholder="请选择领取时间"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="跟进人" prop="followUser">
                  <el-input
                    v-model="formData.followUser"
                    placeholder="请输入跟进人"
                    :maxlength="32"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="跟进时间" prop="followTime">
                  <el-date-picker
                    v-model="formData.followTime"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    placeholder="请选择跟进时间"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="通过支撑点" prop="passPoint">
                  <el-input
                    v-model="formData.passPoint"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入通过支撑点"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="风险点" prop="riskPoint">
                  <el-input
                    v-model="formData.riskPoint"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入风险点"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="保单信息" prop="reportMsg">
                  <el-input
                    v-model="formData.reportMsg"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入保单信息"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane v-if="isEditing" label="审计信息" name="auditInfo">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="创建人" prop="createUser">
                  <el-input v-model="formData.createUser" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="创建时间" prop="createTime">
                  <el-input v-model="formData.createTime" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="修改时间" prop="updateTime">
                  <el-input v-model="formData.updateTime" disabled />
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button
            type="primary"
            :loading="isSaving"
            :disabled="isLoadingForm || hasFormLoadError"
            @click="handleSubmit"
          >
            确定
          </el-button>
          <el-button :disabled="isSaving" @click="closeDialog">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { Refresh, FullScreen } from "@element-plus/icons-vue";

import RentalOrderAPI from "@/api/rental/order";
import type { RentalOrderItem, RentalOrderForm, RentalOrderQuery } from "@/api/rental/order";
import { usePageTable } from "@/composables";

defineOptions({
  name: "RentalOrder",
  inheritAttrs: false,
});

const tenantOptions = [
  { value: "HHHZ", label: "好货惠租" },
  { value: "HHX", label: "换换星" },
];
const addressTypeOptions = [
  { value: "0", label: "自提" },
  { value: "1", label: "邮寄" },
];
const contractTypeOptions = [
  { value: "1", label: "到期买断/归还" },
  { value: "2", label: "租满即送" },
];
const payTypeOptions = [
  { value: "1", label: "指定还款" },
  { value: "2", label: "预收多期" },
  { value: "4", label: "次月为首期" },
];
const settleTypeOptions = [
  { value: "1", label: "一次性结算" },
  { value: "2", label: "分次结算" },
];
const notarialOptions = [
  { value: "0", label: "否" },
  { value: "1", label: "是" },
];
const notarialResultOptions = [
  { value: "-1", label: "审核拒绝" },
  { value: "0", label: "未公证" },
  { value: "2", label: "公证办理中" },
  { value: "3", label: "已发证" },
  { value: "4", label: "撤销公证" },
];
const applyStatusOptions = [
  { value: "00", label: "录入" },
  { value: "01", label: "提交签约" },
  { value: "11", label: "待审核" },
  { value: "26", label: "审核通过" },
  { value: "44", label: "拒绝" },
  { value: "41", label: "退货" },
];
const zlTypeOptions = [
  { value: "cash", label: "我要现金" },
  { value: "zuji", label: "我要租机" },
];

const tenantLabel = (value?: string) =>
  tenantOptions.find((item) => item.value === value)?.label ?? value ?? "";
const addressTypeLabel = (value?: string) =>
  addressTypeOptions.find((item) => item.value === value)?.label ?? value ?? "";
const applyStatusLabel = (value?: string) =>
  applyStatusOptions.find((item) => item.value === value)?.label ?? value ?? "";
const zlTypeLabel = (value?: string) =>
  zlTypeOptions.find((item) => item.value === value)?.label ?? value ?? "";

const tableWrapperRef = ref<HTMLElement | null>(null);
const { toggle: toggleFullscreen } = useFullscreen(tableWrapperRef);

const queryFormRef = ref<FormInstance>();
const dataFormRef = ref<FormInstance>();

// 分页表格数据管理
const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<
  RentalOrderItem,
  RentalOrderQuery
>({
  initialParams: {
    pageNum: 1,
    pageSize: 10,
    keywords: "",
    tenant: "",
    status: "",
    applyStatus: "",
    zlType: "",
    channelType: "",
    createTimeStart: "",
    createTimeEnd: "",
  },
  request: RentalOrderAPI.getPage,
  onBeforeReset: () => queryFormRef.value?.resetFields(),
});

const dialogState = reactive<{ title: string; visible: boolean; orderId?: string }>({
  title: "",
  visible: false,
  orderId: undefined,
});
const activeTab = ref("basic");
const isLoadingForm = ref(false);
const hasFormLoadError = ref(false);
const isSaving = ref(false);
const deletingId = ref<string>();
let formRevision = 0;

const isEditing = computed(() => dialogState.orderId !== undefined);

const initialFormData: RentalOrderForm = {
  orderId: undefined,
  tenant: "HHHZ",
  addressType: "1",
  productContractType: "1",
  payType: "1",
  settleType: "1",
  isNotarial: "0",
  notarialResult: "0",
  number: 1,
  tenor: 12,
  version: 0,
};
const formData = reactive<RentalOrderForm>({ ...initialFormData });

/**
 * JSON 字段校验：允许为空，非空时必须是合法 JSON。
 */
function validateJson(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  if (!value) {
    callback();
    return;
  }
  try {
    JSON.parse(value);
    callback();
  } catch {
    callback(new Error("请输入合法的 JSON"));
  }
}

const rules: FormRules = {
  addressJson: [{ validator: validateJson, trigger: "blur" }],
  skuJson: [{ validator: validateJson, trigger: "blur" }],
};

/**
 * 打开订单表单，编辑时按订单号回填最新详情。
 */
async function openDialog(orderId?: string): Promise<void> {
  if (isSaving.value || deletingId.value !== undefined) return;
  const revision = ++formRevision;
  Object.assign(formData, initialFormData, { orderId });
  activeTab.value = "basic";
  dataFormRef.value?.clearValidate();
  hasFormLoadError.value = false;
  isLoadingForm.value = orderId !== undefined;
  dialogState.orderId = orderId;
  dialogState.title = orderId === undefined ? "新增租赁订单" : "修改租赁订单";
  dialogState.visible = true;

  if (orderId === undefined) return;
  try {
    const data = await RentalOrderAPI.getFormData(orderId);
    if (revision === formRevision) Object.assign(formData, data, { orderId });
  } catch {
    if (revision === formRevision) hasFormLoadError.value = true;
  } finally {
    if (revision === formRevision) isLoadingForm.value = false;
  }
}

/**
 * 校验并提交订单表单
 */
async function handleSubmit(): Promise<void> {
  if (isSaving.value || isLoadingForm.value || hasFormLoadError.value || !dialogState.visible)
    return;
  const revision = formRevision;
  isSaving.value = true;
  try {
    const valid = await dataFormRef.value?.validate().catch(() => false);
    if (!valid || revision !== formRevision) return;

    const data = { ...formData };
    if (dialogState.orderId) {
      await RentalOrderAPI.update(dialogState.orderId, data);
      ElMessage.success("修改成功");
    } else {
      const createdOrderId = await RentalOrderAPI.create(data);
      ElMessage.success(createdOrderId ? `新增成功，订单号 ${createdOrderId}` : "新增成功");
    }
    closeDialog();
    await handleResetQuery().catch(() => undefined);
  } finally {
    isSaving.value = false;
  }
}

/**
 * 关闭弹窗并重置表单
 */
function closeDialog(): void {
  formRevision += 1;
  dialogState.visible = false;
  dialogState.orderId = undefined;
  isLoadingForm.value = false;
  hasFormLoadError.value = false;
  activeTab.value = "basic";
  Object.assign(formData, initialFormData);
  dataFormRef.value?.clearValidate();
}

/**
 * 删除租赁订单
 */
async function handleDelete(orderId: string): Promise<void> {
  if (isSaving.value || deletingId.value !== undefined) return;
  deletingId.value = orderId;
  try {
    const confirmed = await ElMessageBox.confirm(`确认删除订单 ${orderId}?`, "警告", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }).then(
      () => true,
      () => false
    );
    if (!confirmed) return;

    await RentalOrderAPI.deleteById(orderId);
    ElMessage.success("删除成功");
    await handleResetQuery().catch(() => undefined);
  } finally {
    deletingId.value = undefined;
  }
}

onScopeDispose(() => {
  formRevision += 1;
});

onMounted(() => {
  handleQuery();
});
</script>
