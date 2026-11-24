<template>
  <footer class="kc-footer">
    <div class="kc-footer-inner">
      <div class="footer-top">
        <div class="footer-brand">
          <div class="brand-row">
            <span class="brand-badge kc-mono">KC</span>
            <div>
              <div class="brand-name">咔嗒造物商城 · KlackCraft</div>
              <div class="brand-en kc-mono">PRECISION DESK &amp; CUSTOM PERIPHERALS</div>
            </div>
          </div>
          <p class="brand-manifesto">
            取自机械键盘每一次清脆精准的“咔嗒”触发声。专注机械键盘、轻量化鼠标、桌面声学与深度客制化定制，以极简工业设计构筑你的专属桌面秩序。
          </p>
        </div>

        <div class="footer-columns">
          <div class="footer-col">
            <h4>外设品类</h4>
            <router-link to="/products?keyword=键盘">客制化机械键盘</router-link>
            <router-link to="/products?keyword=鼠标">轻量化电竞鼠标</router-link>
            <router-link to="/products?keyword=耳机">桌面监听耳机</router-link>
            <router-link to="/products">全部造物图鉴</router-link>
          </div>

          <div class="footer-col">
            <h4>工坊频道</h4>
            <router-link to="/flash-sale">限时闪购</router-link>
            <router-link to="/coupon-center">礼券工坊</router-link>
            <router-link to="/notice">极客志</router-link>
            <router-link to="/cart">购物清单</router-link>
          </div>

          <div class="footer-col">
            <h4>会员与服务</h4>
            <router-link to="/user/order/list">订单中心</router-link>
            <router-link to="/user/service/after-sale">售后与质保</router-link>
            <router-link to="/user/account/address">收货地址管理</router-link>
            <router-link to="/user/account/profile">个人资料设置</router-link>
          </div>
        </div>
      </div>

      <div class="footer-specs">
        <div class="spec-pill">
          <span class="spec-index kc-mono">01</span>
          <span>CNC 阳极工艺 · 严选客制化套件</span>
        </div>
        <div class="spec-pill">
          <span class="spec-index kc-mono">02</span>
          <span>原厂轴体与微动 · 精准手感调校</span>
        </div>
        <div class="spec-pill">
          <span class="spec-index kc-mono">03</span>
          <span>订单全链路透明 · 顺丰快速履约</span>
        </div>
        <div class="spec-pill">
          <span class="spec-index kc-mono">04</span>
          <span>官方无忧质保 · 极客售后响应</span>
        </div>
      </div>

      <div class="footer-bottom kc-mono">
        <span>© 2026 KlackCraft · 咔嗒造物商城. ALL RIGHTS RESERVED.</span>
        <span>PRECISION IN EVERY CLICK // DESK ARCHITECTURE</span>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
defineOptions({
  name: 'SiteFooter'
});
</script>

<style scoped lang="scss">
.kc-footer {
  margin-top: auto;
  background: var(--kc-carbon);
  color: #b4b7c2;
  border-radius: 28px 28px 0 0;
}

.kc-footer-inner {
  width: min(var(--kc-container-width), calc(100% - 32px));
  margin: 0 auto;
  padding: 48px 0 32px;
}

.footer-top {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: 48px;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-badge {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: var(--kc-accent);
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  display: grid;
  place-items: center;
}

.brand-name {
  color: #ffffff;
  font-size: 16px;
  font-weight: 700;
}

.brand-en {
  margin-top: 3px;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: #7c808e;
}

.brand-manifesto {
  margin: 16px 0 0;
  max-width: 440px;
  font-size: 13px;
  line-height: 1.85;
  color: #8f93a0;
}

.footer-columns {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer-col h4 {
  margin: 0 0 4px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
}

.footer-col a {
  color: #8f93a0;
  text-decoration: none;
  font-size: 13px;
  transition: color 150ms ease;
}

@media (hover: hover) {
  .footer-col a:hover {
    color: var(--kc-accent);
  }
}

.footer-specs {
  margin-top: 32px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.spec-pill {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #c2c5cf;
}

.spec-index {
  color: var(--kc-accent);
  font-size: 11px;
  font-weight: 700;
}

.footer-bottom {
  margin-top: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 11px;
  color: #6b6f7c;
  letter-spacing: 0.05em;
}

@media (max-width: 960px) {
  .footer-top {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .footer-specs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .kc-footer {
    border-radius: 22px 22px 0 0;
  }

  .kc-footer-inner {
    width: calc(100% - 24px);
  }

  .footer-columns {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .footer-specs {
    grid-template-columns: 1fr;
  }

  .footer-bottom {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
