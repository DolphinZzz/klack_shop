<template>
  <el-dialog v-model="visible" :title="`${paramsProps.title}`" :destroy-on-close="true" width="580px" draggable>
    <el-form
      ref="ruleFormRef"
      label-width="140px"
      label-suffix=" :"
      :rules="rules"
      :model="paramsProps.row"
      @submit.enter.prevent="handleSubmit"
    >
      <el-form-item label="主分类" prop="pid">
        <el-tree-select
          v-model="paramsProps.row.pid"
          :data="categoryOptions"
          :props="{ label: 'cateName', value: 'id', children: 'children' }"
          placeholder="请选择主分类"
          clearable
          check-strictly
          :render-after-expand="false"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="分类名称" prop="cateName">
        <el-input v-model="paramsProps.row.cateName" placeholder="请填写分类名称" clearable></el-input>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input v-model="paramsProps.row.sort" placeholder="请填写排序" clearable></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false"> 取消</el-button>
      <el-button type="primary" @click="handleSubmit"> 确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue';
import { type ElForm, ElMessage } from 'element-plus';
import { getShopCategoryAllListApi } from '@/api/modules/shop_system/shopCategory';
import type { ShopCategoryRow } from '@/api/types/shop_system/shopCategory';

defineOptions({
  name: 'ShopCategoryForm'
});

const rules = reactive({
  cateName: [{ required: true, message: '请填写分类名称' }]
});

const visible = ref(false);
const paramsProps = ref<View.DefaultParams>({
  title: '',
  row: {},
  api: undefined,
  getTableList: undefined
});

const categoryOptions = ref<ShopCategoryRow[]>([]);
const isCategoryLoaded = ref(false);

// 获取分类列表
const getCategoryList = async () => {
  if (isCategoryLoaded.value) return; // 如果已经加载过，直接返回

  try {
    const response = await getShopCategoryAllListApi({});
    categoryOptions.value = response.data || [];
    console.log('分类数据:', categoryOptions.value); // 调试日志

    isCategoryLoaded.value = true; // 标记已加载
  } catch (error) {
    console.error('获取分类列表失败:', error);
    ElMessage.error('获取分类列表失败');
  }
};

// 接收父组件传过来的参数
const acceptParams = async (params: View.DefaultParams) => {
  console.log('acceptParams 接收到的数据:', params.row); // 调试日志

  // 确保分类数据已加载
  if (!isCategoryLoaded.value) {
    await getCategoryList();
  }

  // 处理主分类的情况：如果是主分类（pid为0），设置为自己的id，这样会显示自己的名称
  const rowData = { ...params.row };
  if (rowData.pid === 0 || !rowData.pid) {
    rowData.pid = rowData.id; // 主分类显示自己的名称
  }

  paramsProps.value = { ...params, row: rowData };

  // 调试信息
  console.log('处理的 pid:', paramsProps.value.row.pid);
  console.log('categoryOptions.value:', categoryOptions.value);

  visible.value = true;

  // 等待DOM更新后再处理
  await nextTick();
};

// 页面加载时获取分类数据
onMounted(() => {
  getCategoryList();
});

// 提交数据（新增/编辑）
const ruleFormRef = ref<InstanceType<typeof ElForm>>();
const handleSubmit = () => {
  ruleFormRef.value!.validate(async valid => {
    if (!valid) return;
    try {
      // 提交前处理数据
      const submitData = { ...paramsProps.value.row };

      // 如果选择的pid等于自己的id，说明是主分类，设置pid为0
      if (submitData.pid === submitData.id) {
        submitData.pid = 0;
      }

      console.log('提交的数据:', submitData);
      await paramsProps.value.api!(submitData);
      ElMessage.success({ message: `${paramsProps.value.title}成功！` });
      paramsProps.value.getTableList!();
      visible.value = false;
    } catch (error) {
      console.log(error);
    }
  });
};

defineExpose({
  acceptParams
});
</script>

<style scoped lang="scss"></style>
