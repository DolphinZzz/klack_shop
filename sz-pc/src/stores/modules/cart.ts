import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { getCartCountApi } from '@/api';
import { useAuthStore } from '@/stores/modules/auth';

export const useCartStore = defineStore('cart', () => {
  const count = ref(0);
  const loading = ref(false);

  const cartCount = computed(() => Math.max(0, Number(count.value || 0)));

  const setCount = (value: number) => {
    count.value = Math.max(0, Number(value || 0));
  };

  const clear = () => {
    count.value = 0;
  };

  const fetchCount = async () => {
    const authStore = useAuthStore();
    if (!authStore.isLogin) {
      clear();
      return 0;
    }
    loading.value = true;
    try {
      const res = await getCartCountApi();
      setCount(res.data || 0);
      return cartCount.value;
    } finally {
      loading.value = false;
    }
  };

  return {
    count,
    cartCount,
    loading,
    setCount,
    clear,
    fetchCount
  };
});
