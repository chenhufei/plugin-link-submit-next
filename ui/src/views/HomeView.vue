<script setup lang="ts">
import {
  IconExternalLinkLine,
  IconSettings,
  VAlert,
  VButton,
  VCard,
  VDescription,
  VDescriptionItem,
  VPageHeader,
} from '@halo-dev/components'
import LinkVariantPlus from '~icons/mdi/link-variant-plus'

function openOfficialLinks() {
  window.location.assign('/console/links')
}

function openPluginSettings() {
  window.location.assign('/console/plugins/link-submit-next?tab=settings')
}

function openFrontendLinks() {
  window.open('/links', '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <VPageHeader title="友链申请增强">
    <template #icon>
      <LinkVariantPlus class=":uno: mr-2 self-center" />
    </template>
    <template #actions>
      <VButton size="sm" @click="openOfficialLinks"> 返回链接管理 </VButton>
      <VButton size="sm" type="secondary" @click="openFrontendLinks">
        <template #icon><IconExternalLinkLine /></template>
        前台友链页
      </VButton>
    </template>
  </VPageHeader>

  <div class=":uno: m-0 space-y-4 md:m-4">
    <VAlert
      type="info"
      title="申请与审核已统一到 Halo 官方链接插件"
      description="本插件不再保存新的申请记录，也不再执行审核、通知或定时清理。前台增强弹窗会直接调用 PluginLinks 2.3.0 的验证码与申请接口。"
      :closable="false"
    />

    <VCard title="职责边界">
      <VDescription>
        <VDescriptionItem label="本插件负责">
          前台申请弹窗、网站信息提取、主题触发和旧版记录兼容
        </VDescriptionItem>
        <VDescriptionItem label="官方插件负责">
          申请记录、验证码、限流、审核、通知、正式友链数据和清理
        </VDescriptionItem>
        <VDescriptionItem label="兼容要求"> PluginLinks 2.3.0 或更高版本 </VDescriptionItem>
      </VDescription>

      <div class=":uno: mt-5 flex flex-wrap gap-2 border-t border-gray-100 pt-4">
        <VButton type="primary" @click="openOfficialLinks">管理友链与申请</VButton>
        <VButton type="secondary" @click="openPluginSettings">
          <template #icon><IconSettings /></template>
          增强设置
        </VButton>
      </div>
    </VCard>
  </div>
</template>
