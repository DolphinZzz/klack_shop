<template>
  <div class="table-box">
    <ProTable
      ref="proTableRef"
      title="客服知识库管理"
      :indent="20"
      :columns="columns"
      :search-columns="searchColumns"
      :request-api="getTableList"
      row-key="id"
    >
      <template #tableHeader>
        <div class="kb-header">
          <el-button v-auth="'cs.kb.upload'" type="primary" :icon="Upload" @click="openUploadDialog">
            上传 PDF 文档
          </el-button>
          <el-tag v-if="hasActiveParsing" type="warning" effect="light" class="polling-tag">
            存在待解析/解析中文档，每 5 秒自动刷新状态
          </el-tag>
        </div>
      </template>

      <template #operation="{ row }">
        <el-button
          v-if="row.status === 'FAILED'"
          v-auth="'cs.kb.upload'"
          type="primary"
          link
          :icon="RefreshRight"
          :loading="reindexingId === row.id"
          :disabled="reindexingId === row.id || deletingId === row.id"
          @click="handleReindex(row)"
        >
          重新解析入库
        </el-button>
        <el-button
          v-auth="'cs.kb.delete'"
          type="danger"
          link
          :icon="Delete"
          :loading="deletingId === row.id"
          :disabled="row.status === 'PARSING' || deletingId === row.id || reindexingId === row.id"
          @click="handleDelete(row)"
        >
          删除
        </el-button>
      </template>
    </ProTable>

    <KbUploadDialog ref="uploadDialogRef" @success="handleUploadSuccess" />
  </div>
</template>

<script setup lang="ts">
import { onActivated, onBeforeUnmount, onDeactivated, ref } from 'vue';
import { Delete, RefreshRight, Upload } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import ProTable from '@/components/ProTable/index.vue';
import {
  KB_DOCUMENT_CATEGORY_OPTIONS,
  KB_DOCUMENT_STATUS_OPTIONS,
  deleteKbDocumentApi,
  extractApiErrorMessage,
  formatCsDateTime,
  formatCsFileSize,
  getKbDocumentListApi,
  reindexKbDocumentApi
} from '@/api/modules/shop_system/cs';
import type { ColumnProps, ProTableInstance, SearchProps } from '@/components/ProTable/interface';
import type { KbDocumentQuery, KbDocumentRow } from '@/api/types/shop_system/cs';
import KbUploadDialog from '@/views/shop_system/csKnowledge/components/KbUploadDialog.vue';

defineOptions({
  name: 'ShopCsKnowledgeView'
});

const POLL_INTERVAL_MS = 5000;

const proTableRef = ref<ProTableInstance>();
const uploadDialogRef = ref<InstanceType<typeof KbUploadDialog>>();
const hasActiveParsing = ref(false);
const reindexingId = ref<string | null>(null);
const deletingId = ref<string | null>(null);

let isViewActive = true;
let pollTimer: ReturnType<typeof setTimeout> | null = null;
let fetchSeq = 0;

const clearPollTimer = () => {
  if (pollTimer !== null) {
    clearTimeout(pollTimer);
    pollTimer = null;
  }
};

const schedulePollTimer = () => {
  clearPollTimer();
  if (hasActiveParsing.value && isViewActive) {
    pollTimer = setTimeout(() => {
      pollTimer = null;
      proTableRef.value?.getTableList();
    }, POLL_INTERVAL_MS);
  }
};

const schedulePollIfNeeded = (rows: KbDocumentRow[]) => {
  hasActiveParsing.value = rows.some(row => row.status === 'PENDING' || row.status === 'PARSING');
  schedulePollTimer();
};

const enumFieldNames = {
  label: 'label',
  value: 'value',
  tagType: 'tagType'
};

const columns: ColumnProps<KbDocumentRow>[] = [
  { prop: 'title', label: '文档标题', minWidth: 200, showOverflowTooltip: true },
  {
    prop: 'category',
    label: '文档分类',
    minWidth: 130,
    tag: true,
    enum: KB_DOCUMENT_CATEGORY_OPTIONS,
    fieldNames: enumFieldNames
  },
  {
    prop: 'file_name',
    label: '文件名',
    minWidth: 180,
    showOverflowTooltip: true,
    render: ({ row }) => row.file_name || '-'
  },
  {
    prop: 'file_size',
    label: '文件大小',
    minWidth: 100,
    render: ({ row }) => formatCsFileSize(row.file_size)
  },
  {
    prop: 'mineru_mode',
    label: '解析模式',
    minWidth: 100,
    render: ({ row }) => row.mineru_mode || '-'
  },
  {
    prop: 'status',
    label: '入库状态',
    minWidth: 110,
    tag: true,
    enum: KB_DOCUMENT_STATUS_OPTIONS,
    fieldNames: enumFieldNames
  },
  { prop: 'chunk_count', label: '切片数', minWidth: 90 },
  {
    prop: 'error_msg',
    label: '失败原因',
    minWidth: 200,
    showOverflowTooltip: true,
    render: ({ row }) => row.error_msg || '-'
  },
  {
    prop: 'created_at',
    label: '上传时间',
    minWidth: 170,
    render: ({ row }) => formatCsDateTime(row.created_at)
  },
  { prop: 'operation', label: '操作', width: 200, fixed: 'right' }
];

const searchColumns: SearchProps[] = [
  {
    prop: 'category',
    label: '文档分类',
    el: 'select',
    enum: KB_DOCUMENT_CATEGORY_OPTIONS,
    fieldNames: enumFieldNames
  },
  {
    prop: 'status',
    label: '入库状态',
    el: 'select',
    enum: KB_DOCUMENT_STATUS_OPTIONS,
    fieldNames: enumFieldNames
  }
];

const getTableList = async (params: KbDocumentQuery) => {
  const seq = ++fetchSeq;
  clearPollTimer();
  let succeeded = false;
  try {
    const res = await getKbDocumentListApi(params);
    succeeded = true;
    if (seq === fetchSeq) {
      schedulePollIfNeeded(res.data?.rows || []);
    }
    return res;
  } finally {
    if (!succeeded && seq === fetchSeq && hasActiveParsing.value) {
      schedulePollTimer();
    }
  }
};

const openUploadDialog = () => {
  uploadDialogRef.value?.open();
};

const handleUploadSuccess = () => {
  proTableRef.value?.getTableList();
};

const handleReindex = async (row: KbDocumentRow) => {
  if (!row.id || reindexingId.value || deletingId.value) return;
  reindexingId.value = row.id;
  try {
    await reindexKbDocumentApi(row.id, { use_cached_markdown: false });
    ElMessage.success('已重新加入后台解析队列');
    proTableRef.value?.getTableList();
  } catch (error) {
    ElMessage.error(extractApiErrorMessage(error, '重新解析入库失败'));
  } finally {
    reindexingId.value = null;
  }
};

const handleDelete = async (row: KbDocumentRow) => {
  if (!row.id || deletingId.value || reindexingId.value) return;
  try {
    await ElMessageBox.confirm(
      `确认删除知识库文档「${row.title}」吗？删除后将同步清理向量索引与 PDF 文件，且不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
  } catch {
    return;
  }

  deletingId.value = row.id;
  try {
    await deleteKbDocumentApi(row.id);
    ElMessage.success('删除成功');
    proTableRef.value?.getTableList();
  } catch (error) {
    ElMessage.error(extractApiErrorMessage(error, '删除知识库文档失败'));
  } finally {
    deletingId.value = null;
  }
};

onActivated(() => {
  isViewActive = true;
  if (hasActiveParsing.value && pollTimer === null) {
    proTableRef.value?.getTableList();
  }
});

onDeactivated(() => {
  isViewActive = false;
  clearPollTimer();
});

onBeforeUnmount(() => {
  isViewActive = false;
  clearPollTimer();
});
</script>

<style scoped lang="scss">
.kb-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.polling-tag {
  font-size: 12px;
}
</style>
