import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';

import App from '@/App.vue';
import router from '@/router';
import pinia from '@/stores';
import '@/styles/index.scss';

const app = createApp(App);

Object.entries(ElementPlusIconsVue).forEach(([key, component]) => {
  app.component(key, component);
});

app.use(ElementPlus);
app.use(pinia);
app.use(router);
app.mount('#app');
