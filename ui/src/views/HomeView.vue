<script setup lang="ts">
import Cron from "@/views/Cron.vue";
import { useRouteQuery } from "@vueuse/router";
import LinkVariantPlus from "~icons/mdi/link-variant-plus";

import {
  VCard,
  VPageHeader,
  VTabbar,
} from "@halo-dev/components";
import SubmitList from "@/views/SubmitList.vue";

const tabs = [
  {
    id: "submitList",
    label: "提交记录",
  },
  {
    id: "cron",
    label: "定时任务",
  }
];

const activeIndex = useRouteQuery<string>("tab", tabs[0].id);

</script>

<template>

  <VPageHeader title="友链自助提交管理">
    <template #icon>
      <LinkVariantPlus class=":uno: mr-2 self-center"/>
    </template>
  </VPageHeader>

  <div class=":uno: m-0 space-y-4 md:m-4">
    <div class=":uno: border-b border-gray-100 bg-white px-4 py-2">
        <VTabbar
          v-model:active-id="activeIndex"
          :items="tabs"
          class=":uno: w-full"
          type="outline"
        />
    </div>
    <SubmitList v-if="activeIndex === 'submitList'" />
    <VCard v-else-if="activeIndex === 'cron'" :body-class="[':uno: !p-0']">
      <Cron />
    </VCard>
  </div>


</template>
