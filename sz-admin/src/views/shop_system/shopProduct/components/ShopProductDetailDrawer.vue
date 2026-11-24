<template>
  <el-drawer v-model="visible" title="商品详情" size="60%" :with-header="true" destroy-on-close>
    <div v-loading="loading" class="detail-wrap">
      <!-- 顶部概览 -->
      <el-card shadow="never" class="overview-card">
        <div class="overview">
          <el-image class="cover" :src="coverUrl" fit="cover" :preview-src-list="previewList" preview-teleported>
            <template #error>
              <div class="img-fallback">暂无图片</div>
            </template>
          </el-image>

          <div class="meta">
            <div class="title-row">
              <div class="name">{{ detail?.productName || '-' }}</div>
              <el-tag v-if="detail?.isShow !== undefined" :type="detail?.isShow == 1 ? 'success' : 'info'">
                {{ detail?.isShow == 1 ? '上架' : '下架' }}
              </el-tag>
            </div>

            <el-descriptions :column="3" border class="desc">
              <el-descriptions-item label="商品售价">
                <span class="price">¥ {{ detail?.price ?? '-' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="销量">
                {{ detail?.sales ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="库存">
                {{ detail?.stock ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ detail?.createTime ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="分类">
                {{ detail?.categoryName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="单位">
                {{ detail?.unitName || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-card>

      <!-- Tabs -->
      <el-card shadow="never" class="tabs-card">
        <el-tabs v-model="activeTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="base">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="商品名称">{{ detail?.productName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="关键词">{{ detail?.keyword || '-' }}</el-descriptions-item>
              <el-descriptions-item label="简介">{{ detail?.desc || '-' }}</el-descriptions-item>
              <el-descriptions-item label="轮播图">
                <div v-if="!sliderFileList.length">-</div>
                <div v-else class="slider-box">
                  <UploadImgs
                    v-model:fileList="sliderFileList"
                    :disabled="true"
                    :limit="sliderFileList.length"
                    :width="'88px'"
                    :height="'88px'"
                    dir="product"
                  />
                </div>
              </el-descriptions-item>
            </el-descriptions>

            <div class="attr-block" v-if="detail?.attrList?.length">
              <div class="block-title">规格属性</div>
              <div class="attr-grid">
                <el-card v-for="attr in detail.attrList" :key="attr.id" shadow="never" class="attr-card">
                  <div class="attr-name">{{ attr.attrName }}</div>
                  <div class="attr-options">
                    <el-tag v-for="opt in attr.optionList || []" :key="opt.optionName" class="opt-tag" type="info">
                      {{ opt.optionName }}
                    </el-tag>
                  </div>
                </el-card>
              </div>
            </div>
          </el-tab-pane>

          <!-- 规格库存 -->
          <el-tab-pane label="规格库存" name="sku">
            <el-table :data="detail?.attrValueList || []" border style="width: 100%">
              <el-table-column prop="sku" label="规格组合" min-width="200" />
              <el-table-column prop="price" label="售价" width="120">
                <template #default="{ row }">¥ {{ row.price ?? '-' }}</template>
              </el-table-column>
              <el-table-column prop="stock" label="库存" width="100" />
              <el-table-column prop="barCode" label="条码" min-width="160" />
              <el-table-column prop="cost" label="成本" width="120">
                <template #default="{ row }">¥ {{ row.cost ?? '-' }}</template>
              </el-table-column>
              <el-table-column prop="otPrice" label="市场价" width="120">
                <template #default="{ row }">¥ {{ row.otPrice ?? '-' }}</template>
              </el-table-column>
              <el-table-column v-if="showVipPrice" label="会员价" width="120">
                <template #default="{ row }">¥ {{ row.vip_price ?? row.vipPrice ?? '-' }}</template>
              </el-table-column>
              <el-table-column prop="weight" label="重量" width="100" />
              <el-table-column prop="volume" label="体积" width="100" />
            </el-table>

            <el-alert
              v-if="!detail?.attrValueList?.length"
              title="暂无规格库存数据"
              type="info"
              :closable="false"
              class="mt-12"
            />
          </el-tab-pane>

          <!-- 商品详情 -->
          <el-tab-pane label="商品详情" name="detail">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="商品介绍">
                {{ detail?.productInfo || '-' }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="mt-12">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="图片地址">
                  <el-link v-if="detail?.image" :href="detail.image" target="_blank">{{ detail.image }}</el-link>
                  <span v-else>-</span>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage, type UploadUserFile } from 'element-plus';
import { getShopProductDetailApi } from '@/api/modules/shop_system/shopProduct';
import UploadImgs from '@/components/Upload/Imgs.vue';

type ProductDetail = any; // 你有 VO 类型的话可以换成更严格的类型

const visible = ref(false);
const loading = ref(false);
const activeTab = ref<'base' | 'sku' | 'detail'>('base');
const detail = ref<ProductDetail | null>(null);

const showVipPrice = computed(() => {
  const list = detail.value?.attrValueList || [];
  // 只要任意一条有 vip_price 或 vipPrice，就认为需要展示这一列
  return list.some((x: any) => x?.vip_price !== undefined || x?.vipPrice !== undefined);
});

const sliderFileList = computed<UploadUserFile[]>(() => {
  const s = detail.value?.sliderImage;
  if (!s) return [];
  // 如果后端就是 "url1,url2"
  const urls = String(s)
    .split(',')
    .map(x => x.trim())
    .filter(Boolean);

  return urls.map((url, idx) => ({
    name: `slider_${idx + 1}`,
    url
  }));
});

const coverUrl = computed(() => {
  const img = detail.value?.image;
  // 你项目里 image 有时是 string，有时可能是数组（上传组件回填）
  if (Array.isArray(img)) return img?.[0]?.url || img?.[0] || '';
  return img || '';
});

const previewList = computed(() => {
  const img = detail.value?.image;
  if (Array.isArray(img)) {
    return img.map((x: any) => (typeof x === 'string' ? x : x?.url)).filter(Boolean);
  }
  return coverUrl.value ? [coverUrl.value] : [];
});

// 对外暴露：打开抽屉并加载详情
const open = async (productId: number | string) => {
  visible.value = true;
  activeTab.value = 'base';
  loading.value = true;
  detail.value = null;

  try {
    // 你现在列表页用的是 getShopProductDetailApi({ id })
    // 这里沿用它：如果你后端是 /detail/{id}，那就把 API 封装改一下即可
    const res = await getShopProductDetailApi({ productId: Number(productId) });
    detail.value = res?.data || null;
  } catch (e) {
    ElMessage.error('获取商品详情失败');
    visible.value = false;
  } finally {
    loading.value = false;
  }
};

defineExpose({ open });
</script>

<style scoped lang="scss">
.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.overview-card,
.tabs-card {
  border-radius: 12px;
}

.overview {
  display: flex;
  gap: 16px;
}

.cover {
  width: 140px;
  height: 140px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--el-fill-color-light);
}

.img-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.meta {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.name {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  font-weight: 700;
  font-size: 16px;
}

.attr-block {
  margin-top: 12px;
}

.block-title {
  font-weight: 600;
  margin: 8px 0 10px;
}

.attr-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.attr-card {
  border-radius: 12px;
}

.attr-name {
  font-weight: 600;
  margin-bottom: 8px;
}

.attr-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.opt-tag {
  border-radius: 10px;
}

.mt-12 {
  margin-top: 12px;
}

.slider-box {
  padding: 6px 0;
}
</style>
