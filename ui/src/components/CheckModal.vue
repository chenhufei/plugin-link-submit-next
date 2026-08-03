<script lang="ts" setup>
import { linkSubmitApiClient } from "@/api";
import { type CheckLinkSubmitRequest, type LinkSubmit, LinkSubmitSpecStatusEnum, LinkSubmitSpecTypeEnum } from "@/api/generated";
import type { LinkList } from "@/domain";
import { axiosInstance } from "@halo-dev/api-client";
import { Toast, VButton, VModal, VSpace, VTag } from "@halo-dev/components";
import { utils } from "@halo-dev/ui-shared";
import { submitForm } from "@formkit/core";
import { useQueryClient } from "@tanstack/vue-query";
import { computed, ref, useTemplateRef } from "vue";

const props = withDefaults(
  defineProps<{
    linkSubmit: LinkSubmit;
    groupLabel?: string;
  }>(),
  { groupLabel: "未分组" }
);

const emit = defineEmits<{ (event: "close"): void }>();
const saving = ref(false);
const formState = ref<CheckLinkSubmitRequest>({ checkStatus: true, reason: "" });
const queryClient = useQueryClient();
const modal = useTemplateRef<InstanceType<typeof VModal> | null>("modal");
const isPending = computed(() => props.linkSubmit.spec.status === LinkSubmitSpecStatusEnum.Pending);
const reviewDescription = computed(
  () => props.linkSubmit.metadata.annotations?.["link.submit.halo.run/review-description"]
    || ""
);
const statusMeta = computed(() => {
  return {
    review: { label: "已通过", type: "success" },
    pending: { label: "待审核", type: "warning" },
    refuse: { label: "已拒绝", type: "danger" },
  }[props.linkSubmit.spec.status] || { label: "未知", type: "default" };
});

async function handleCheck() {
  if (formState.value.checkStatus && props.linkSubmit.spec.type === LinkSubmitSpecTypeEnum.Update && !formState.value.linkName) {
    Toast.error("请选择需要修改的正式友链");
    return;
  }
  try {
    saving.value = true;
    await linkSubmitApiClient.linkSubmit.check({
      name: props.linkSubmit.metadata.name,
      checkLinkSubmitRequest: formState.value,
    });
    Toast.success(formState.value.checkStatus ? "已通过申请" : "已拒绝申请");
    modal.value?.close();
  } finally {
    queryClient.invalidateQueries({ queryKey: ["link-submits"] });
    saving.value = false;
  }
}

const handleSelectLinkRemote = {
  search: async ({ keyword, page, size }: { keyword: string; page: number; size: number }) => {
    const { data } = await axiosInstance.get<LinkList>("/apis/api.link.halo.run/v1alpha1/links", {
      params: { page, size, keyword },
    });
    return {
      options: data.items.map((item) => ({ label: item.spec?.displayName, value: item.metadata.name })),
      total: data.total,
      page: data.page,
      size: data.size,
    };
  },
  findOptionsByValues: () => [],
};

function submitReviewForm() {
  submitForm("check-form");
}
</script>

<template>
  <VModal
    ref="modal"
    :centered="false"
    :mount-to-body="true"
    :title="isPending ? `审核申请 - ${linkSubmit.spec.displayName}` : `申请详情 - ${linkSubmit.spec.displayName}`"
    :width="640"
    @close="emit('close')"
  >
    <div class=":uno: space-y-4">
      <div class=":uno: flex flex-wrap items-center gap-3 border border-gray-200 rounded-lg bg-gray-50 px-4 py-3">
        <img
          v-if="linkSubmit.spec.logo"
          :src="linkSubmit.spec.logo"
          :alt="linkSubmit.spec.displayName"
          class=":uno: h-10 w-10 rounded object-cover"
        />
        <div v-else class=":uno: h-10 w-10 flex items-center justify-center rounded bg-gray-200 text-sm text-gray-600 font-semibold">
          {{ linkSubmit.spec.displayName.slice(0, 1) || "?" }}
        </div>
        <div class=":uno: min-w-0 flex-1">
          <div class=":uno: truncate text-base text-gray-900 font-semibold">{{ linkSubmit.spec.displayName }}</div>
          <div class=":uno: mt-1 truncate text-sm text-gray-500">{{ linkSubmit.spec.url }}</div>
        </div>
        <VTag :type="statusMeta.type">{{ statusMeta.label }}</VTag>
        <VTag type="info">{{ linkSubmit.spec.type === LinkSubmitSpecTypeEnum.Add ? "新增" : "修改" }}</VTag>
      </div>

      <dl class=":uno: overflow-hidden border border-gray-200 rounded-lg divide-y divide-gray-100">
        <div class=":uno: flex gap-3 px-3.5 py-2.5 text-sm"><dt class=":uno: w-22 shrink-0 text-gray-500">链接地址</dt><dd class=":uno: min-w-0 flex-1 break-all"><a :href="linkSubmit.spec.url" target="_blank" rel="noopener noreferrer" class=":uno: text-blue-600 hover:underline">{{ linkSubmit.spec.url }}</a></dd></div>
        <div v-if="linkSubmit.spec.oldUrl" class=":uno: flex gap-3 px-3.5 py-2.5 text-sm"><dt class=":uno: w-22 shrink-0 text-gray-500">原链接</dt><dd class=":uno: min-w-0 flex-1 break-all text-gray-900">{{ linkSubmit.spec.oldUrl }}</dd></div>
        <div class=":uno: flex gap-3 px-3.5 py-2.5 text-sm"><dt class=":uno: w-22 shrink-0 text-gray-500">分组</dt><dd class=":uno: min-w-0 flex-1 text-gray-900">{{ groupLabel }}</dd></div>
        <div v-if="linkSubmit.spec.email" class=":uno: flex gap-3 px-3.5 py-2.5 text-sm"><dt class=":uno: w-22 shrink-0 text-gray-500">邮箱</dt><dd class=":uno: min-w-0 flex-1 break-all"><a :href="`mailto:${linkSubmit.spec.email}`" class=":uno: text-blue-600 hover:underline">{{ linkSubmit.spec.email }}</a></dd></div>
        <div v-if="linkSubmit.spec.description" class=":uno: flex gap-3 px-3.5 py-2.5 text-sm"><dt class=":uno: w-22 shrink-0 text-gray-500">简介</dt><dd class=":uno: min-w-0 flex-1 whitespace-pre-wrap text-gray-900">{{ linkSubmit.spec.description }}</dd></div>
        <div v-if="linkSubmit.spec.rssUrl" class=":uno: flex gap-3 px-3.5 py-2.5 text-sm"><dt class=":uno: w-22 shrink-0 text-gray-500">RSS</dt><dd class=":uno: min-w-0 flex-1 break-all text-gray-900">{{ linkSubmit.spec.rssUrl }}</dd></div>
        <div v-if="reviewDescription" class=":uno: flex gap-3 px-3.5 py-2.5 text-sm"><dt class=":uno: w-22 shrink-0 text-gray-500">审核说明</dt><dd class=":uno: min-w-0 flex-1 whitespace-pre-wrap text-gray-900">{{ reviewDescription }}</dd></div>
        <div v-if="linkSubmit.metadata.creationTimestamp" class=":uno: flex gap-3 px-3.5 py-2.5 text-sm"><dt class=":uno: w-22 shrink-0 text-gray-500">提交时间</dt><dd class=":uno: min-w-0 flex-1 text-gray-900">{{ utils.date.format(linkSubmit.metadata.creationTimestamp) }}</dd></div>
      </dl>

      <FormKit
        v-if="isPending"
        id="check-form"
        name="check-form"
        type="form"
        :actions="false"
        :config="{ validationVisibility: 'submit' }"
        @submit="handleCheck"
      >
        <FormKit
          v-model="formState.checkStatus"
          :options="[
            { label: '通过', value: true },
            { label: '拒绝', value: false },
          ]"
          label="审核结果"
          name="checkStatus"
          type="select"
        />
        <FormKit
          v-if="formState.checkStatus && linkSubmit.spec.type === LinkSubmitSpecTypeEnum.Update"
          v-model="formState.linkName"
          type="select"
          label="需要修改的正式友链"
          name="linkName"
          searchable
          remote
          :remote-option="handleSelectLinkRemote"
          :auto-select="false"
          clearable
          validation="required"
        />
        <FormKit
          v-if="formState.checkStatus === false"
          v-model="formState.reason"
          type="textarea"
          name="reason"
          label="拒绝原因"
          placeholder="请说明拒绝原因"
          validation="required"
          rows="3"
        />
      </FormKit>
    </div>

    <template #footer>
      <VSpace>
        <VButton v-if="isPending" :loading="saving" type="secondary" @click="submitReviewForm">提交审核</VButton>
        <VButton @click="modal?.close()">{{ isPending ? "取消" : "关闭" }}</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
