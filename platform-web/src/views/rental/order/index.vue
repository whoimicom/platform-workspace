<template>
  <div class="page-container">
    <el-card class="page-search" shadow="never">
      <el-form ref="queryFormRef" :model="params" :inline="true">
        <el-form-item label="租户主体：HHHZ: 好货惠租；HHX: 换换星" prop="tenant">
          <el-input
            v-model="params.tenant"
            placeholder="请输入租户主体：HHHZ: 好货惠租；HHX: 换换星"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="渠道类型" prop="channelType">
          <el-input
            v-model="params.channelType"
            placeholder="请输入渠道类型"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="订单状态" prop="status">
          <el-input
            v-model="params.status"
            placeholder="请输入订单状态"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="收获方式：0-自提；1-邮寄" prop="addressType">
          <el-input
            v-model="params.addressType"
            placeholder="请输入收获方式：0-自提；1-邮寄"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="1: 到期买断 / 归还2. 租满即送" prop="productContractType">
          <el-input
            v-model="params.productContractType"
            placeholder="请输入1: 到期买断 / 归还2. 租满即送"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="page-content" shadow="never">
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-button v-hasPerm="'rental:order:add'" type="primary" @click="handleAdd">新增</el-button>
        </div>
        <div class="page-toolbar__right">
          <el-tooltip content="刷新" placement="top">
            <el-button class="page-icon-btn" @click="handleQuery">
              <el-icon><Refresh /></el-icon>
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
          <el-table-column key="memberOrderId" label="个人申请编号" prop="memberOrderId" min-width="120" />
          <el-table-column key="memberApplyStatus" label="状态：00-录入，01-提交签约,11-待审核，26-审核ok，44-拒绝，41-退货" prop="memberApplyStatus" min-width="120" />
          <el-table-column key="memberChannelType" label="渠道类型：zfb,wx" prop="memberChannelType" min-width="120" />
          <el-table-column key="memberZlType" label="类型：cash-我要现金,zuji-我要租机" prop="memberZlType" min-width="120" />
          <el-table-column key="memberCreSn" label="证件号码 作为先上传附件时唯一关联用" prop="memberCreSn" min-width="120" />
          <el-table-column key="memberMobile" label="手机号" prop="memberMobile" min-width="120" />
          <el-table-column key="memberProductContractType" label="类型: 租完即送" prop="memberProductContractType" min-width="120" />
          <el-table-column key="memberProductSkuId" label="选择的商品id" prop="memberProductSkuId" min-width="120" />
          <el-table-column key="memberProductOldNew" label="产品类型：1-新机；2-二手" prop="memberProductOldNew" min-width="120" />
          <el-table-column key="memberRealName" label="真实姓名" prop="memberRealName" min-width="120" />
          <el-table-column key="memberRepaymentAccount" label="还款帐号" prop="memberRepaymentAccount" min-width="120" />
          <el-table-column key="memberRepaymentBankName" label="银行名称" prop="memberRepaymentBankName" min-width="120" />
          <el-table-column key="memberRepaymentBank" label="还款开户行编号" prop="memberRepaymentBank" min-width="120" />
          <el-table-column key="memberBankBindType" label="绑定类型：3-三要素；4-四要素" prop="memberBankBindType" min-width="120" />
          <el-table-column key="memberPdfSignType" label="签约方式：face-人脸。smsSign-短信签约" prop="memberPdfSignType" min-width="120" />
          <el-table-column key="memberPdfSmsSignPhone" label="短信签约手机号" prop="memberPdfSmsSignPhone" min-width="120" />
          <el-table-column key="memberUserLevel" label="等级" prop="memberUserLevel" min-width="120" />
          <el-table-column key="memberRisk" label="风险等级" prop="memberRisk" min-width="120" />
          <el-table-column key="memberRecommendQuota" label="推荐额度" prop="memberRecommendQuota" min-width="120" />
          <el-table-column key="memberAccessMsg" label="准入信息" prop="memberAccessMsg" min-width="120" />
          <el-table-column key="memberLoanProduct" label="产品id" prop="memberLoanProduct" min-width="120" />
          <el-table-column key="memberGoodsTotal" label="商品总金额" prop="memberGoodsTotal" min-width="120" />
          <el-table-column key="memberPeriods" label="分期期数" prop="memberPeriods" min-width="120" />
          <el-table-column key="memberDownMoney" label="首次还款额" prop="memberDownMoney" min-width="120" />
          <el-table-column key="memberDownDate" label="首次还款日" prop="memberDownDate" min-width="120" />
          <el-table-column key="memberPerMonthMoney" label="每月还款额" prop="memberPerMonthMoney" min-width="120" />
          <el-table-column key="memberPerMonthDate" label="每月还款日" prop="memberPerMonthDate" min-width="120" />
          <el-table-column key="memberMemory" label="内存" prop="memberMemory" min-width="120" />
          <el-table-column key="memberColor" label="颜色" prop="memberColor" min-width="120" />
          <el-table-column key="memberProductName" label="产品" prop="memberProductName" min-width="120" />
          <el-table-column key="memberPwAnswer" label="品牌" prop="memberPwAnswer" min-width="120" />
          <el-table-column key="memberBorrowReason" label="当为商品时替换为商品类型" prop="memberBorrowReason" min-width="120" />
          <el-table-column key="memberWechatCode" label="公众号简称" prop="memberWechatCode" min-width="120" />
          <el-table-column key="memberWechatName" label="公众号名称" prop="memberWechatName" min-width="120" />
          <el-table-column key="memberWechatAppid" label="公众号appid" prop="memberWechatAppid" min-width="120" />
          <el-table-column key="memberWeixin" label="微信" prop="memberWeixin" min-width="120" />
          <el-table-column key="memberQq" label="qq" prop="memberQq" min-width="120" />
          <el-table-column key="memberZhiMaFen" label="芝麻分" prop="memberZhiMaFen" min-width="120" />
          <el-table-column key="memberNationality" label="民族" prop="memberNationality" min-width="120" />
          <el-table-column key="memberAge" label="年龄" prop="memberAge" min-width="120" />
          <el-table-column key="memberGender" label="性别：0-男，1-女" prop="memberGender" min-width="120" />
          <el-table-column key="memberBirthday" label="出生日期" prop="memberBirthday" min-width="120" />
          <el-table-column key="memberCreValidStart" label="身份证有效期(yyyymmdd)" prop="memberCreValidStart" min-width="120" />
          <el-table-column key="memberCreValid" label="身份证有效期(yyyymmdd)" prop="memberCreValid" min-width="120" />
          <el-table-column key="memberBodies" label="发证机构" prop="memberBodies" min-width="120" />
          <el-table-column key="memberCensusProvince" label="户籍地址：省/直辖市" prop="memberCensusProvince" min-width="120" />
          <el-table-column key="memberCensusCity" label="户籍地址：城市" prop="memberCensusCity" min-width="120" />
          <el-table-column key="memberCensusDistrict" label="户籍地址：区/县/镇" prop="memberCensusDistrict" min-width="120" />
          <el-table-column key="memberCunsusZip" label="户籍地址邮编" prop="memberCunsusZip" min-width="120" />
          <el-table-column key="memberCensusAddr" label="户籍地址：除去省市区后的" prop="memberCensusAddr" min-width="120" />
          <el-table-column key="memberCensusAddrDetail" label="户籍详细地址：省/直辖市，城市，区/县，镇，街/村，居民区，楼号，层号，房间号" prop="memberCensusAddrDetail" min-width="120" />
          <el-table-column key="memberLiveProvince" label="现居住地址:省/直辖市" prop="memberLiveProvince" min-width="120" />
          <el-table-column key="memberLiveCity" label="现居住地址:城市" prop="memberLiveCity" min-width="120" />
          <el-table-column key="memberLiveDistrict" label="现居住地址:区/县/镇" prop="memberLiveDistrict" min-width="120" />
          <el-table-column key="memberLiveZip" label="现居住地址:邮编" prop="memberLiveZip" min-width="120" />
          <el-table-column key="memberLiveAddr" label="现居住地址:除去省市区后的" prop="memberLiveAddr" min-width="120" />
          <el-table-column key="memberLiveAddrDetail" label="完整现居住地址" prop="memberLiveAddrDetail" min-width="120" />
          <el-table-column key="memberLiveKuandai" label="家庭宽带" prop="memberLiveKuandai" min-width="120" />
          <el-table-column key="memberCompanyName" label="单位名称" prop="memberCompanyName" min-width="120" />
          <el-table-column key="memberCompanyProvince" label="单位地址：省/直辖市" prop="memberCompanyProvince" min-width="120" />
          <el-table-column key="memberCompanyCity" label="单位地址：城市" prop="memberCompanyCity" min-width="120" />
          <el-table-column key="memberCompanyDistrict" label="单位地址：区/县/镇" prop="memberCompanyDistrict" min-width="120" />
          <el-table-column key="memberCompanyZip" label="单位地址：邮编" prop="memberCompanyZip" min-width="120" />
          <el-table-column key="memberCompanyAddr" label="单位地址:除去省市区后的" prop="memberCompanyAddr" min-width="120" />
          <el-table-column key="memberCompanyAddrDetail" label="完整单位地址" prop="memberCompanyAddrDetail" min-width="120" />
          <el-table-column key="memberCompanyTel" label="单位电话" prop="memberCompanyTel" min-width="120" />
          <el-table-column key="memberWorkYears" label="工作年限" prop="memberWorkYears" min-width="120" />
          <el-table-column key="memberWorkMoneys" label="工资收入" prop="memberWorkMoneys" min-width="120" />
          <el-table-column key="memberWorkMoneyType" label="收入方式" prop="memberWorkMoneyType" min-width="120" />
          <el-table-column key="memberWedlock" label="婚姻状况:0-未婚、1-已婚有子女、2-已婚无子女、3-离异、4-丧偶" prop="memberWedlock" min-width="120" />
          <el-table-column key="memberSpouseName" label="配偶姓名" prop="memberSpouseName" min-width="120" />
          <el-table-column key="memberSpouseTel" label="配偶联系电话" prop="memberSpouseTel" min-width="120" />
          <el-table-column key="memberParentsName" label="父母1姓名" prop="memberParentsName" min-width="120" />
          <el-table-column key="memberParentsRel" label="申请人关系" prop="memberParentsRel" min-width="120" />
          <el-table-column key="memberParentsTel" label="父母1电话" prop="memberParentsTel" min-width="120" />
          <el-table-column key="memberParents2Name" label="父母2姓名" prop="memberParents2Name" min-width="120" />
          <el-table-column key="memberParents2Rel" label="申请人关系" prop="memberParents2Rel" min-width="120" />
          <el-table-column key="memberParents2Tel" label="父母2电话" prop="memberParents2Tel" min-width="120" />
          <el-table-column key="memberFamily1Name" label="家人姓名" prop="memberFamily1Name" min-width="120" />
          <el-table-column key="memberFamily1Rel" label="家人关系" prop="memberFamily1Rel" min-width="120" />
          <el-table-column key="memberFamily1Tel" label="家人电话" prop="memberFamily1Tel" min-width="120" />
          <el-table-column key="memberContactName" label="紧急联系人姓名" prop="memberContactName" min-width="120" />
          <el-table-column key="memberContactRel" label="关系" prop="memberContactRel" min-width="120" />
          <el-table-column key="memberContactTel" label="紧急联系人电话" prop="memberContactTel" min-width="120" />
          <el-table-column key="memberGpsLng" label="gps经度" prop="memberGpsLng" min-width="120" />
          <el-table-column key="memberGpsLat" label="gps纬度" prop="memberGpsLat" min-width="120" />
          <el-table-column key="memberTencentLng" label="腾讯经度" prop="memberTencentLng" min-width="120" />
          <el-table-column key="memberTencentLat" label="腾讯纬度" prop="memberTencentLat" min-width="120" />
          <el-table-column key="memberLongitude" label="longitude" prop="memberLongitude" min-width="120" />
          <el-table-column key="memberLatitude" label="latitude" prop="memberLatitude" min-width="120" />
          <el-table-column key="memberRecordAddress" label="record_address" prop="memberRecordAddress" min-width="120" />
          <el-table-column key="memberAppType" label="app类型" prop="memberAppType" min-width="120" />
          <el-table-column key="memberPhoneTime" label="手机号时长" prop="memberPhoneTime" min-width="120" />
          <el-table-column key="memberValidateData" label="手机号姓名身份证校验" prop="memberValidateData" min-width="120" />
          <el-table-column key="memberIsXhzw" label="是否携号" prop="memberIsXhzw" min-width="120" />
          <el-table-column key="memberOperator" label="运营商" prop="memberOperator" min-width="120" />
          <el-table-column key="memberOperatorYuan" label="原运营商" prop="memberOperatorYuan" min-width="120" />
          <el-table-column key="memberPtSmsIspId" label="手机号时长id" prop="memberPtSmsIspId" min-width="120" />
          <el-table-column key="memberVdSmsIspId" label="实名验证id" prop="memberVdSmsIspId" min-width="120" />
          <el-table-column key="memberSignShanghu" label="签章商户" prop="memberSignShanghu" min-width="120" />
          <el-table-column key="memberOtherTemplateType" label="额外需要的合同类型" prop="memberOtherTemplateType" min-width="120" />
          <el-table-column key="memberCreatTime" label="创建时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.memberCreatTime) }}</template>
          </el-table-column>
          <el-table-column key="memberUpdateTime" label="update_time" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.memberUpdateTime) }}</template>
          </el-table-column>
          <el-table-column key="memberSerialNumber" label="人脸认证序列号" prop="memberSerialNumber" min-width="120" />
          <el-table-column key="memberRevokeFlag" label="撤销黑名单首付限制(N:取消)" prop="memberRevokeFlag" min-width="120" />
          <el-table-column key="memberRevokeUser" label="撤销人" prop="memberRevokeUser" min-width="120" />
          <el-table-column key="memberDeliveryStatus" label="物流状态" prop="memberDeliveryStatus" min-width="120" />
          <el-table-column key="orderId" label="订单号" prop="orderId" min-width="120" />
          <el-table-column key="tenant" label="租户主体：HHHZ: 好货惠租；HHX: 换换星" prop="tenant" min-width="120" />
          <el-table-column key="channelType" label="渠道类型" prop="channelType" min-width="120" />
          <el-table-column key="creSn" label="身份证号" prop="creSn" min-width="120" />
          <el-table-column key="status" label="订单状态" prop="status" min-width="120" />
          <el-table-column key="productId" label="产品Id" prop="productId" min-width="120" />
          <el-table-column key="addressType" label="收获方式：0-自提；1-邮寄" prop="addressType" min-width="120" />
          <el-table-column key="productContractType" label="1: 到期买断 / 归还2. 租满即送" prop="productContractType" min-width="120" />
          <el-table-column key="borrowDays" label="借款天数" prop="borrowDays" min-width="120" />
          <el-table-column key="tenor" label="期数" prop="tenor" min-width="120" />
          <el-table-column key="number" label="租用数量" prop="number" min-width="120" />
          <el-table-column key="payType" label="支付方式：1-指定还款;2-预收多期;4:次月为首期" prop="payType" min-width="120" />
          <el-table-column key="amount" label="订单金额" prop="amount" min-width="120" />
          <el-table-column key="monthAmount" label="月租金" prop="monthAmount" min-width="120" />
          <el-table-column key="deposit" label="押金" prop="deposit" min-width="120" />
          <el-table-column key="buyOutPrice" label="买断价" prop="buyOutPrice" min-width="120" />
          <el-table-column key="updateTime" label="修改时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.updateTime) }}</template>
          </el-table-column>
          <el-table-column key="createUser" label="创建人" prop="createUser" min-width="120" />
          <el-table-column key="createTime" label="创建时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column key="settleType" label="商户结算方式(1:一次性结算 2:分次结算)" prop="settleType" min-width="120" />
          <el-table-column key="merchantId" label="商户ID" prop="merchantId" min-width="120" />
          <el-table-column key="merchantAmt" label="商户结算总金额" prop="merchantAmt" min-width="120" />
          <el-table-column key="merchantAfterAmt" label="商户延期结算金额" prop="merchantAfterAmt" min-width="120" />
          <el-table-column key="salesId" label="业代ID" prop="salesId" min-width="120" />
          <el-table-column key="payTenor" label="支付期数" prop="payTenor" min-width="120" />
          <el-table-column key="payFirstAmount" label="首付金额" prop="payFirstAmount" min-width="120" />
          <el-table-column key="buyOutFlag" label="买断标识(是否需要支付)" prop="buyOutFlag" min-width="120" />
          <el-table-column key="brokenFee" label="碎屏险" prop="brokenFee" min-width="120" />
          <el-table-column key="modifyFee" label="维修费" prop="modifyFee" min-width="120" />
          <el-table-column key="lockFee" label="锁机费" prop="lockFee" min-width="120" />
          <el-table-column key="remark" label="备注" prop="remark" min-width="120" />
          <el-table-column key="takeDate" label="提货日期" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.takeDate) }}</template>
          </el-table-column>
          <el-table-column key="addressDeliver" label="交付地址" prop="addressDeliver" min-width="120" />
          <el-table-column key="color" label="颜色" prop="color" min-width="120" />
          <el-table-column key="refuseCode" label="拒绝码" prop="refuseCode" min-width="120" />
          <el-table-column key="accountDate" label="非首期账单日" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.accountDate) }}</template>
          </el-table-column>
          <el-table-column key="approveUser" label="审核人" prop="approveUser" min-width="120" />
          <el-table-column key="approveTime" label="审核时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.approveTime) }}</template>
          </el-table-column>
          <el-table-column key="finishTime" label="订单完成时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.finishTime) }}</template>
          </el-table-column>
          <el-table-column key="isNotarial" label="是否公证(0:否 1:是)" prop="isNotarial" min-width="120" />
          <el-table-column key="notarialResult" label="公证结果(-1:审核拒绝 0:未公证 2:公证办理中 3:已发证 4:撤销公证)" prop="notarialResult" min-width="120" />
          <el-table-column key="passCode" label="审核通过代码" prop="passCode" min-width="120" />
          <el-table-column key="version" label="数据锁" prop="version" min-width="120" />
          <el-table-column key="backDate" label="退货时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.backDate) }}</template>
          </el-table-column>
          <el-table-column key="backUser" label="退货人" prop="backUser" min-width="120" />
          <el-table-column key="officialPrice" label="官网价格" prop="officialPrice" min-width="120" />
          <el-table-column key="keepEntry" label="提前归还保持入账标识(0:否 1:是)" prop="keepEntry" min-width="120" />
          <el-table-column key="commonId" label="公共账户ID" prop="commonId" min-width="120" />
          <el-table-column key="memberFee" label="会员费" prop="memberFee" min-width="120" />
          <el-table-column key="claimUser" label="claim_user" prop="claimUser" min-width="120" />
          <el-table-column key="claimTime" label="claim_time" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.claimTime) }}</template>
          </el-table-column>
          <el-table-column key="channel" label="扣款渠道(SNXY:苏宁 ZJXY:中金)" prop="channel" min-width="120" />
          <el-table-column key="merchantNo" label="扣款商户" prop="merchantNo" min-width="120" />
          <el-table-column key="jobType" label="职业类型" prop="jobType" min-width="120" />
          <el-table-column key="jobDetail" label="职业" prop="jobDetail" min-width="120" />
          <el-table-column key="moreFirstAmount" label="首付增加金额" prop="moreFirstAmount" min-width="120" />
          <el-table-column key="returnStatus" label="产品状态" prop="returnStatus" min-width="120" />
          <el-table-column key="usePhone" label="当前使用手机" prop="usePhone" min-width="120" />
          <el-table-column key="guarantee" label="是否担保" prop="guarantee" min-width="120" />
          <el-table-column key="subsidyAmount" label="补贴金额" prop="subsidyAmount" min-width="120" />
          <el-table-column key="firstPayType" label="首付支付方式" prop="firstPayType" min-width="120" />
          <el-table-column key="sensitiveMsg" label="审核敏感词" prop="sensitiveMsg" min-width="120" />
          <el-table-column key="firstPaySource" label="首付来源" prop="firstPaySource" min-width="120" />
          <el-table-column key="followUser" label="跟进人" prop="followUser" min-width="120" />
          <el-table-column key="followTime" label="跟进时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.followTime) }}</template>
          </el-table-column>
          <el-table-column key="superviseLockId" label="id" prop="superviseLockId" min-width="120" />
          <el-table-column key="superviseLockOrderId" label="订单号" prop="superviseLockOrderId" min-width="120" />
          <el-table-column key="superviseLockSuperviseId" label="监管ID" prop="superviseLockSuperviseId" min-width="120" />
          <el-table-column key="superviseLockSuperviseName" label="监管名称" prop="superviseLockSuperviseName" min-width="120" />
          <el-table-column key="superviseLockSerialNumber" label="序列号" prop="superviseLockSerialNumber" min-width="120" />
          <el-table-column key="superviseLockStatus" label="是否上锁(1:已锁 0:正常)" prop="superviseLockStatus" min-width="120" />
          <el-table-column key="superviseLockIsInterface" label="是否支持接口(0:否 1:是)" prop="superviseLockIsInterface" min-width="120" />
          <el-table-column key="superviseLockNormalWallpaper" label="正常壁纸" prop="superviseLockNormalWallpaper" min-width="120" />
          <el-table-column key="superviseLockActivationLockStatus" label="activationLockStatus 激活锁状态" prop="superviseLockActivationLockStatus" min-width="120" />
          <el-table-column key="superviseLockSupervise" label="是否处于监管" prop="superviseLockSupervise" min-width="120" />
          <el-table-column key="superviseLockOnlineStatus" label="在线状态" prop="superviseLockOnlineStatus" min-width="120" />
          <el-table-column key="superviseLockAbmStatus" label="ABM在库状态" prop="superviseLockAbmStatus" min-width="120" />
          <el-table-column key="superviseLockRemark" label="备注" prop="superviseLockRemark" min-width="120" />
          <el-table-column key="superviseLockUpdateUser" label="修改人" prop="superviseLockUpdateUser" min-width="120" />
          <el-table-column key="superviseLockCreateUser" label="创建人" prop="superviseLockCreateUser" min-width="120" />
          <el-table-column key="superviseLockUpdateTime" label="修改时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.superviseLockUpdateTime) }}</template>
          </el-table-column>
          <el-table-column key="superviseLockCreateTime" label="创建时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.superviseLockCreateTime) }}</template>
          </el-table-column>
          <el-table-column key="superviseLockSysType" label="系统类型" prop="superviseLockSysType" min-width="120" />
          <el-table-column key="superviseLockBrandId" label="品牌" prop="superviseLockBrandId" min-width="120" />
          <el-table-column key="superviseLockSerialCode" label="串码" prop="superviseLockSerialCode" min-width="120" />
          <el-table-column key="superviseLockModel" label="手机型号(vivo锁)" prop="superviseLockModel" min-width="120" />
          <el-table-column key="superviseLockSuperviseCode" label="监管码(vivo锁)" prop="superviseLockSuperviseCode" min-width="120" />
          <el-table-column key="superviseLockSerialCode2" label="监管码2(vivo锁)" prop="superviseLockSerialCode2" min-width="120" />
          <el-table-column key="superviseLockTenant" label="租户主体" prop="superviseLockTenant" min-width="120" />
          <el-table-column key="superviseLockRestriction" label="APP限制(0:否 1:是)" prop="superviseLockRestriction" min-width="120" />
          <el-table-column key="superviseLockLastActiveTime" label="最后活动时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.superviseLockLastActiveTime) }}</template>
          </el-table-column>
          <el-table-column key="superviseLockDealDate" label="处理日期" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.superviseLockDealDate) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="scope">
              <el-button
                v-hasPerm="'rental:order:edit'"
                link
                type="primary"
                @click="handleEdit(scope.row)"
              >
                编辑
              </el-button>
              <el-button
                v-hasPerm="'rental:order:remove'"
                link
                type="danger"
                @click="handleDelete(scope.row)"
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
      v-model="dialog.visible"
      :title="dialog.title"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="订单号" prop="orderId">
          <el-input
            v-model="form.orderId"
            placeholder="请输入订单号"
            clearable
            :disabled="dialog.id !== undefined"
          />
        </el-form-item>
        <el-form-item label="租户主体：HHHZ: 好货惠租；HHX: 换换星" prop="tenant">
          <el-input
            v-model="form.tenant"
            placeholder="请输入租户主体：HHHZ: 好货惠租；HHX: 换换星"
            clearable
          />
        </el-form-item>
        <el-form-item label="渠道类型" prop="channelType">
          <el-input
            v-model="form.channelType"
            placeholder="请输入渠道类型"
            clearable
          />
        </el-form-item>
        <el-form-item label="身份证号" prop="creSn">
          <el-input
            v-model="form.creSn"
            placeholder="请输入身份证号"
            clearable
          />
        </el-form-item>
        <el-form-item label="订单状态" prop="status">
          <el-input
            v-model="form.status"
            placeholder="请输入订单状态"
            clearable
          />
        </el-form-item>
        <el-form-item label="产品Id" prop="productId">
          <el-input-number v-model="form.productId" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收获方式：0-自提；1-邮寄" prop="addressType">
          <el-input
            v-model="form.addressType"
            placeholder="请输入收获方式：0-自提；1-邮寄"
            clearable
          />
        </el-form-item>
        <el-form-item label="地址" prop="addressJson">
          <el-input
            v-model="form.addressJson"
            placeholder="请输入地址"
            clearable
          />
        </el-form-item>
        <el-form-item label="产品sku" prop="skuJson">
          <el-input
            v-model="form.skuJson"
            placeholder="请输入产品sku"
            clearable
          />
        </el-form-item>
        <el-form-item label="1: 到期买断 / 归还2. 租满即送" prop="productContractType">
          <el-input
            v-model="form.productContractType"
            placeholder="请输入1: 到期买断 / 归还2. 租满即送"
            clearable
          />
        </el-form-item>
        <el-form-item label="借款天数" prop="borrowDays">
          <el-input-number v-model="form.borrowDays" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="期数" prop="tenor">
          <el-input-number v-model="form.tenor" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="租用数量" prop="number">
          <el-input-number v-model="form.number" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="支付方式：1-指定还款;2-预收多期;4:次月为首期" prop="payType">
          <el-input
            v-model="form.payType"
            placeholder="请输入支付方式：1-指定还款;2-预收多期;4:次月为首期"
            clearable
          />
        </el-form-item>
        <el-form-item label="订单金额" prop="amount">
          <el-input-number v-model="form.amount" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="月租金" prop="monthAmount">
          <el-input-number v-model="form.monthAmount" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="押金" prop="deposit">
          <el-input-number v-model="form.deposit" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="买断价" prop="buyOutPrice">
          <el-input-number v-model="form.buyOutPrice" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="修改时间" prop="updateTime">
          <el-date-picker
            v-model="form.updateTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择修改时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="创建人" prop="createUser">
          <el-input
            v-model="form.createUser"
            placeholder="请输入创建人"
            clearable
          />
        </el-form-item>
        <el-form-item label="创建时间" prop="createTime">
          <el-date-picker
            v-model="form.createTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择创建时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="商户结算方式(1:一次性结算 2:分次结算)" prop="settleType">
          <el-input
            v-model="form.settleType"
            placeholder="请输入商户结算方式(1:一次性结算 2:分次结算)"
            clearable
          />
        </el-form-item>
        <el-form-item label="商户ID" prop="merchantId">
          <el-input-number v-model="form.merchantId" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="商户结算总金额" prop="merchantAmt">
          <el-input-number v-model="form.merchantAmt" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="商户延期结算金额" prop="merchantAfterAmt">
          <el-input-number v-model="form.merchantAfterAmt" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="业代ID" prop="salesId">
          <el-input-number v-model="form.salesId" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="支付期数" prop="payTenor">
          <el-input-number v-model="form.payTenor" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="首付金额" prop="payFirstAmount">
          <el-input-number v-model="form.payFirstAmount" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="买断标识(是否需要支付)" prop="buyOutFlag">
          <el-input
            v-model="form.buyOutFlag"
            placeholder="请输入买断标识(是否需要支付)"
            clearable
          />
        </el-form-item>
        <el-form-item label="碎屏险" prop="brokenFee">
          <el-input-number v-model="form.brokenFee" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="维修费" prop="modifyFee">
          <el-input-number v-model="form.modifyFee" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="锁机费" prop="lockFee">
          <el-input-number v-model="form.lockFee" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            placeholder="请输入备注"
            clearable
          />
        </el-form-item>
        <el-form-item label="提货日期" prop="takeDate">
          <el-date-picker
            v-model="form.takeDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择提货日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="交付地址" prop="addressDeliver">
          <el-input
            v-model="form.addressDeliver"
            placeholder="请输入交付地址"
            clearable
          />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-input
            v-model="form.color"
            placeholder="请输入颜色"
            clearable
          />
        </el-form-item>
        <el-form-item label="拒绝码" prop="refuseCode">
          <el-input
            v-model="form.refuseCode"
            placeholder="请输入拒绝码"
            clearable
          />
        </el-form-item>
        <el-form-item label="非首期账单日" prop="accountDate">
          <el-date-picker
            v-model="form.accountDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择非首期账单日"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="审核人" prop="approveUser">
          <el-input
            v-model="form.approveUser"
            placeholder="请输入审核人"
            clearable
          />
        </el-form-item>
        <el-form-item label="审核时间" prop="approveTime">
          <el-date-picker
            v-model="form.approveTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择审核时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="订单完成时间" prop="finishTime">
          <el-date-picker
            v-model="form.finishTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择订单完成时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="是否公证(0:否 1:是)" prop="isNotarial">
          <el-input
            v-model="form.isNotarial"
            placeholder="请输入是否公证(0:否 1:是)"
            clearable
          />
        </el-form-item>
        <el-form-item label="公证结果(-1:审核拒绝 0:未公证 2:公证办理中 3:已发证 4:撤销公证)" prop="notarialResult">
          <el-input
            v-model="form.notarialResult"
            placeholder="请输入公证结果(-1:审核拒绝 0:未公证 2:公证办理中 3:已发证 4:撤销公证)"
            clearable
          />
        </el-form-item>
        <el-form-item label="审核通过代码" prop="passCode">
          <el-input
            v-model="form.passCode"
            placeholder="请输入审核通过代码"
            clearable
          />
        </el-form-item>
        <el-form-item label="数据锁" prop="version">
          <el-input-number v-model="form.version" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退货时间" prop="backDate">
          <el-date-picker
            v-model="form.backDate"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择退货时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="退货人" prop="backUser">
          <el-input
            v-model="form.backUser"
            placeholder="请输入退货人"
            clearable
          />
        </el-form-item>
        <el-form-item label="官网价格" prop="officialPrice">
          <el-input-number v-model="form.officialPrice" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="提前归还保持入账标识(0:否 1:是)" prop="keepEntry">
          <el-input
            v-model="form.keepEntry"
            placeholder="请输入提前归还保持入账标识(0:否 1:是)"
            clearable
          />
        </el-form-item>
        <el-form-item label="公共账户ID" prop="commonId">
          <el-input-number v-model="form.commonId" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="会员费" prop="memberFee">
          <el-input-number v-model="form.memberFee" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="claim_user" prop="claimUser">
          <el-input
            v-model="form.claimUser"
            placeholder="请输入claim_user"
            clearable
          />
        </el-form-item>
        <el-form-item label="claim_time" prop="claimTime">
          <el-date-picker
            v-model="form.claimTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择claim_time"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="扣款渠道(SNXY:苏宁 ZJXY:中金)" prop="channel">
          <el-input
            v-model="form.channel"
            placeholder="请输入扣款渠道(SNXY:苏宁 ZJXY:中金)"
            clearable
          />
        </el-form-item>
        <el-form-item label="扣款商户" prop="merchantNo">
          <el-input
            v-model="form.merchantNo"
            placeholder="请输入扣款商户"
            clearable
          />
        </el-form-item>
        <el-form-item label="职业类型" prop="jobType">
          <el-input
            v-model="form.jobType"
            placeholder="请输入职业类型"
            clearable
          />
        </el-form-item>
        <el-form-item label="职业" prop="jobDetail">
          <el-input
            v-model="form.jobDetail"
            placeholder="请输入职业"
            clearable
          />
        </el-form-item>
        <el-form-item label="首付增加金额" prop="moreFirstAmount">
          <el-input-number v-model="form.moreFirstAmount" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="产品状态" prop="returnStatus">
          <el-input
            v-model="form.returnStatus"
            placeholder="请输入产品状态"
            clearable
          />
        </el-form-item>
        <el-form-item label="当前使用手机" prop="usePhone">
          <el-input
            v-model="form.usePhone"
            placeholder="请输入当前使用手机"
            clearable
          />
        </el-form-item>
        <el-form-item label="是否担保" prop="guarantee">
          <el-input
            v-model="form.guarantee"
            placeholder="请输入是否担保"
            clearable
          />
        </el-form-item>
        <el-form-item label="通过支撑点" prop="passPoint">
          <el-input
            v-model="form.passPoint"
            placeholder="请输入通过支撑点"
            clearable
          />
        </el-form-item>
        <el-form-item label="风险点" prop="riskPoint">
          <el-input
            v-model="form.riskPoint"
            placeholder="请输入风险点"
            clearable
          />
        </el-form-item>
        <el-form-item label="保单信息" prop="reportMsg">
          <el-input
            v-model="form.reportMsg"
            placeholder="请输入保单信息"
            clearable
          />
        </el-form-item>
        <el-form-item label="补贴金额" prop="subsidyAmount">
          <el-input-number v-model="form.subsidyAmount" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="首付支付方式" prop="firstPayType">
          <el-input
            v-model="form.firstPayType"
            placeholder="请输入首付支付方式"
            clearable
          />
        </el-form-item>
        <el-form-item label="审核敏感词" prop="sensitiveMsg">
          <el-input
            v-model="form.sensitiveMsg"
            placeholder="请输入审核敏感词"
            clearable
          />
        </el-form-item>
        <el-form-item label="首付来源" prop="firstPaySource">
          <el-input
            v-model="form.firstPaySource"
            placeholder="请输入首付来源"
            clearable
          />
        </el-form-item>
        <el-form-item label="跟进人" prop="followUser">
          <el-input
            v-model="form.followUser"
            placeholder="请输入跟进人"
            clearable
          />
        </el-form-item>
        <el-form-item label="跟进时间" prop="followTime">
          <el-date-picker
            v-model="form.followTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择跟进时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";

import OrderAPI from "@/api/rental/order";
import type {
  OrderItem,
  OrderQuery,
  OrderSaveForm,
} from "@/api/rental/order";
import { usePageTable } from "@/composables";

defineOptions({
  name: "Order",
  inheritAttrs: false,
});

const formatDateTime = (value?: string) => (value ? value.replace("T", " ") : "");

const queryFormRef = ref<FormInstance>();

const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<
  OrderItem,
  OrderQuery
>({
  initialParams: {
    pageNum: 1,
    pageSize: 10,
    tenant: "",
    channelType: "",
    status: "",
    addressType: "",
    productContractType: "",
  },
  request: OrderAPI.getPage,
  onBeforeReset: () => queryFormRef.value?.resetFields(),
});

const formRef = ref<FormInstance>();
const dialog = reactive({
  visible: false,
  title: "",
  saving: false,
  id: undefined as string | undefined,
});

const initialForm = (): OrderSaveForm => ({
  orderId: undefined,
  tenant: undefined,
  channelType: undefined,
  creSn: undefined,
  status: undefined,
  productId: undefined,
  addressType: undefined,
  addressJson: undefined,
  skuJson: undefined,
  productContractType: undefined,
  borrowDays: undefined,
  tenor: undefined,
  number: undefined,
  payType: undefined,
  amount: undefined,
  monthAmount: undefined,
  deposit: undefined,
  buyOutPrice: undefined,
  updateTime: undefined,
  createUser: undefined,
  createTime: undefined,
  settleType: undefined,
  merchantId: undefined,
  merchantAmt: undefined,
  merchantAfterAmt: undefined,
  salesId: undefined,
  payTenor: undefined,
  payFirstAmount: undefined,
  buyOutFlag: undefined,
  brokenFee: undefined,
  modifyFee: undefined,
  lockFee: undefined,
  remark: undefined,
  takeDate: undefined,
  addressDeliver: undefined,
  color: undefined,
  refuseCode: undefined,
  accountDate: undefined,
  approveUser: undefined,
  approveTime: undefined,
  finishTime: undefined,
  isNotarial: undefined,
  notarialResult: undefined,
  passCode: undefined,
  version: undefined,
  backDate: undefined,
  backUser: undefined,
  officialPrice: undefined,
  keepEntry: undefined,
  commonId: undefined,
  memberFee: undefined,
  claimUser: undefined,
  claimTime: undefined,
  channel: undefined,
  merchantNo: undefined,
  jobType: undefined,
  jobDetail: undefined,
  moreFirstAmount: undefined,
  returnStatus: undefined,
  usePhone: undefined,
  guarantee: undefined,
  passPoint: undefined,
  riskPoint: undefined,
  reportMsg: undefined,
  subsidyAmount: undefined,
  firstPayType: undefined,
  sensitiveMsg: undefined,
  firstPaySource: undefined,
  followUser: undefined,
  followTime: undefined,
});

const form = reactive<OrderSaveForm>(initialForm());

const rules: FormRules = {};

const resetForm = () => Object.assign(form, initialForm());

const handleAdd = () => {
  resetForm();
  dialog.id = undefined;
  dialog.title = "新增订单表";
  dialog.visible = true;
};

const handleEdit = async (row: OrderItem) => {
  const detail = await OrderAPI.getDetail(row.orderId as string);
  resetForm();
  Object.assign(form, detail);
  dialog.id = detail.orderId;
  dialog.title = "编辑订单表";
  dialog.visible = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  dialog.saving = true;
  try {
    if (dialog.id === undefined) {
      await OrderAPI.create(form);
    } else {
      await OrderAPI.update(dialog.id, form);
    }
    ElMessage.success("保存成功");
    dialog.visible = false;
    fetchData();
  } finally {
    dialog.saving = false;
  }
};

const handleDelete = async (row: OrderItem) => {
  await ElMessageBox.confirm("确认删除该记录吗？", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  });
  await OrderAPI.remove(row.orderId as string);
  ElMessage.success("删除成功");
  fetchData();
};

onMounted(() => {
  handleQuery();
});
</script>
