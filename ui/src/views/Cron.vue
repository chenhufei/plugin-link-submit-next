<script lang="ts" setup>
import { linkSubmitCoreApiClient } from "@/api";
import type { CronLinkSubmit } from "@/api/generated";
import type { LinkGroupList } from "@/domain";
import { axiosInstance } from "@halo-dev/api-client";
import { Toast, VAlert, VDescription, VDescriptionItem, VLoading } from "@halo-dev/components";
import { utils } from "@halo-dev/ui-shared";
import { useMutation, useQuery } from "@tanstack/vue-query";
import { computed, ref } from "vue";

const TASK_NAME = "cron-link-submit-default";

function createDefaultTask(): CronLinkSubmit {
  return {
    metadata: { name: TASK_NAME },
    spec: {
      cron: "@daily",
      suspend: true,
      cleanConfig: {
        type: "delete",
        withoutCheckGroupNames: [],
        moveGroupName: "",
      },
    },
    kind: "CronLinkSubmit",
    apiVersion: "link.submit.halo.run/v1alpha1",
  };
}

const formState = ref<CronLinkSubmit>(createDefaultTask());
const taskExists = computed(() => Boolean(formState.value.metadata.creationTimestamp));
const enabled = computed({
  get: () => !Boolean(formState.value.spec.suspend),
  set: (value: boolean) => {
    formState.value.spec.suspend = !value;
  },
});

const { isLoading } = useQuery({
  queryKey: ["cron-link-submit"],
  queryFn: async () => {
    const { data } = await linkSubmitCoreApiClient.cronLinkSubmit.listCronLinkSubmit(
      { page: 1, size: 100 },
      { mute: true }
    );
    return data.items.find((item) => item.metadata.name === TASK_NAME);
  },
  onSuccess(task) {
    formState.value = task || createDefaultTask();
  },
  retry: false,
});

const { mutate: save, isLoading: isSaving } = useMutation({
  mutationKey: ["cron-link-submit-save"],
  mutationFn: async () => {
    if (taskExists.value) {
      return linkSubmitCoreApiClient.cronLinkSubmit.updateCronLinkSubmit({
        name: TASK_NAME,
        cronLinkSubmit: formState.value,
      });
    }
    return linkSubmitCoreApiClient.cronLinkSubmit.createCronLinkSubmit({
      cronLinkSubmit: formState.value,
    });
  },
  onSuccess(response) {
    formState.value = response.data;
    Toast.success("定时任务已保存");
  },
  onError() {
    Toast.error("定时任务保存失败，请稍后重试");
  },
});

const handleSelectGroupRemote = {
  search: async ({ keyword, page, size }: { keyword: string; page: number; size: number }) => {
    const { data } = await axiosInstance.get<LinkGroupList>(
      "/apis/core.halo.run/v1alpha1/linkgroups",
      { params: { page, size, keyword } }
    );
    return {
      options: data.items.map((item) => ({
        label: item.spec?.displayName,
        value: item.metadata.name,
      })),
      total: data.total,
      page: data.page,
      size: data.size,
    };
  },
  findOptionsByValues: async (values: string[]) => {
    if (!values.length) return [];
    const { data } = await axiosInstance.get<LinkGroupList>(
      "/apis/core.halo.run/v1alpha1/linkgroups",
      { params: { page: 1, size: 1000 } }
    );
    const selectedValues = new Set(values);
    return data.items
      .filter((item) => selectedValues.has(item.metadata.name))
      .map((item) => ({
        label: item.spec?.displayName,
        value: item.metadata.name,
      }));
  },
};

const cronOptions = [
  { label: "每月（每月 1 日 0 点）", value: "@monthly" },
  { label: "每周（每周一 0 点）", value: "@weekly" },
  { label: "每天（每天 0 点）", value: "@daily" },
  { label: "每小时", value: "@hourly" },
];

function formatTime(value?: string) {
  return value ? utils.date.format(value) : "尚未执行";
}
</script>

<template>
  <div class=":uno: space-y-4 p-4">
    <VAlert
      type="info"
      title="定时检查正式友链"
      description="任务仅消费 Halo 官方链接插件的数据；默认关闭，启用后按设定周期检查无法访问的友链。"
      :closable="false"
    />

    <div v-if="isLoading" class=":uno: py-12">
      <VLoading />
    </div>

    <FormKit
      v-else
      id="cron-setting"
      type="form"
      :actions="false"
      @submit="save"
    >
      <FormKit v-model="enabled" label="启用定时任务" type="switch" name="enabled" />
      <FormKit
        v-model="formState.spec.cron"
        label="执行周期"
        type="select"
        name="cron"
        allow-create
        searchable
        help="可选择常用周期，也可填写 Spring Cron 表达式"
        validation="required"
        :options="cronOptions"
      />
      <FormKit v-model="formState.spec.cleanConfig" type="group" name="cleanConfig">
        <FormKit
          :options="[
            { label: '删除', value: 'delete' },
            { label: '移动到指定分组', value: 'move' },
          ]"
          name="type"
          type="radio"
          label="无效友链处理方式"
          validation="required"
        />
        <FormKit
          type="select"
          label="免检查分组"
          name="withoutCheckGroupNames"
          help="这些分组中的友链不会参与定时检查"
          searchable
          remote
          multiple
          :remote-option="handleSelectGroupRemote"
        />
        <FormKit
          v-if="formState.spec.cleanConfig?.type === 'move'"
          type="select"
          label="无效友链目标分组"
          name="moveGroupName"
          searchable
          remote
          :remote-option="handleSelectGroupRemote"
          validation="required"
        />
      </FormKit>

      <div v-permission="['plugin:link:submit-next:manage']" class=":uno: border-t border-gray-100 pt-4">
        <FormKit
          type="submit"
          :label="isSaving ? '保存中...' : '保存'"
          :disabled="isLoading || isSaving"
        />
      </div>
    </FormKit>

    <div v-if="formState.status" class=":uno: rounded border border-gray-100 p-4">
      <h3 class=":uno: mb-3 text-sm text-gray-900 font-medium">运行状态</h3>
      <VDescription>
        <VDescriptionItem label="上次执行">
          {{ formatTime(formState.status.lastScheduledTimestamp) }}
        </VDescriptionItem>
        <VDescriptionItem label="下次执行">
          {{ formState.status.nextSchedulingTimestamp ? utils.date.format(formState.status.nextSchedulingTimestamp) : "未计划" }}
        </VDescriptionItem>
      </VDescription>
    </div>
  </div>
</template>
