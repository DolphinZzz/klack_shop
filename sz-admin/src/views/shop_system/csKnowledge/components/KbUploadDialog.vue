<template>
  <el-dialog
    v-model="visible"
    title="上传知识库 PDF 文档"
    width="560px"
    destroy-on-close
    :close-on-click-modal="!submitting"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    @closed="resetForm"
  >
    <el-form label-width="96px">
      <el-form-item label="文档分类" required>
        <el-select v-model="form.category" class="full-width" placeholder="请选择文档分类">
          <el-option v-for="item in KB_DOCUMENT_CATEGORY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="文档标题">
        <el-input v-model="form.title" maxlength="256" show-word-limit placeholder="留空则默认使用 PDF 文件名" clearable />
      </el-form-item>

      <el-form-item label="PDF 文件" required>
        <el-upload
          ref="uploadRef"
          v-model:file-list="fileList"
          class="pdf-upload"
          drag
          action="#"
          accept=".pdf,application/pdf"
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :on-exceed="handleExceed"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将 PDF 文件拖到此处，或 <em>点击选择文件</em></div>
          <template #tip>
            <div class="el-upload__tip">仅支持单个 PDF 文件，大小不超过 50MB；系统将自动进行 SHA-256 去重校验</div>
          </template>
        </el-upload>
      </el-form-item>

      <el-alert v-if="errorDetail" :title="errorDetail" type="error" show-icon :closable="false" class="upload-error-alert" />
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">上传并开始解析</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { UploadFilled } from '@element-plus/icons-vue';
import { ElMessage, genFileId } from 'element-plus';
import type { UploadFile, UploadFiles, UploadInstance, UploadProps, UploadRawFile, UploadUserFile } from 'element-plus';
import { KB_DOCUMENT_CATEGORY_OPTIONS, extractApiErrorMessage, uploadKbDocumentApi } from '@/api/modules/shop_system/cs';
import type { KbDocumentCategory } from '@/api/types/shop_system/cs';

defineOptions({
  name: 'KbUploadDialog'
});

const emit = defineEmits<{
  success: [];
}>();

const MAX_PDF_BYTES = 50 * 1024 * 1024;

const visible = ref(false);
const submitting = ref(false);
const errorDetail = ref('');
const uploadRef = ref<UploadInstance>();
const fileList = ref<UploadUserFile[]>([]);
const selectedFile = ref<File | null>(null);
let dialogSessionSeq = 0;

const form = ref<{
  category: KbDocumentCategory;
  title: string;
}>({
  category: 'mall_policy',
  title: ''
});

const validatePdfFile = (rawFile: File): boolean => {
  const isPdfExt = rawFile.name.toLowerCase().endsWith('.pdf');
  const isPdfMime = rawFile.type === 'application/pdf';
  if (!isPdfExt && !isPdfMime) {
    ElMessage.warning('仅支持上传 PDF 格式文档');
    return false;
  }
  if (rawFile.size <= 0) {
    ElMessage.warning('上传文件不能为空');
    return false;
  }
  if (rawFile.size > MAX_PDF_BYTES) {
    ElMessage.warning('PDF 文件大小不能超过 50MB');
    return false;
  }
  return true;
};

const handleFileChange = (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  errorDetail.value = '';
  const raw = uploadFile.raw;
  if (!raw || !validatePdfFile(raw)) {
    uploadRef.value?.clearFiles();
    fileList.value = [];
    selectedFile.value = null;
    return;
  }
  selectedFile.value = raw;
  fileList.value = uploadFiles.slice(-1);
};

const handleFileRemove = () => {
  selectedFile.value = null;
  fileList.value = [];
  errorDetail.value = '';
};

const handleExceed: UploadProps['onExceed'] = files => {
  const nextFile = files[0] as UploadRawFile | undefined;
  if (!nextFile || !validatePdfFile(nextFile)) return;
  uploadRef.value?.clearFiles();
  nextFile.uid = genFileId();
  uploadRef.value?.handleStart(nextFile);
};

const resetForm = () => {
  dialogSessionSeq++;
  form.value = {
    category: 'mall_policy',
    title: ''
  };
  uploadRef.value?.clearFiles();
  selectedFile.value = null;
  fileList.value = [];
  errorDetail.value = '';
  submitting.value = false;
};

const open = () => {
  resetForm();
  visible.value = true;
};

const handleSubmit = async () => {
  if (submitting.value) return;
  if (!form.value.category) {
    ElMessage.warning('请选择文档分类');
    return;
  }
  if (!selectedFile.value) {
    ElMessage.warning('请选择要上传的 PDF 文件');
    return;
  }

  const sessionSeq = dialogSessionSeq;
  errorDetail.value = '';
  submitting.value = true;
  try {
    await uploadKbDocumentApi({
      file: selectedFile.value,
      category: form.value.category,
      title: form.value.title
    });
    if (sessionSeq !== dialogSessionSeq) return;
    ElMessage.success('上传成功，已进入后台解析入库队列');
    visible.value = false;
    emit('success');
  } catch (error) {
    if (sessionSeq !== dialogSessionSeq) return;
    errorDetail.value = extractApiErrorMessage(error, '上传失败，请检查文件后重试');
  } finally {
    if (sessionSeq === dialogSessionSeq) {
      submitting.value = false;
    }
  }
};

defineExpose({
  open
});
</script>

<style scoped lang="scss">
.full-width {
  width: 100%;
}

.pdf-upload {
  width: 100%;
}

:deep(.pdf-upload .el-upload),
:deep(.pdf-upload .el-upload-dragger) {
  width: 100%;
}

.upload-error-alert {
  margin-top: 8px;
}
</style>
