<script lang="ts" setup>
import {
  VCard,
  VButton,
  VEmpty,
  VLoading,
  VPagination,
  VSpace,
  VTag,
  VEntity,
  VEntityContainer,
  VEntityField,
  Dialog,
  Toast,
  IconRefreshLine,
} from "@halo-dev/components";
import { useQuery, useQueryClient, useMutation } from "@tanstack/vue-query";
import { computed, ref, watch } from "vue";
import { useRouteQuery } from "@vueuse/router";
import { linkSubmitApiClient, linkSubmitCoreApiClient } from "@/api";
import { type LinkSubmit, LinkSubmitSpecStatusEnum } from "@/api/generated";
import { axiosInstance } from "@halo-dev/api-client";
import type { LinkGroup, LinkGroupList } from "@/domain";
import { linkSubmitStatusOptions, linkSubmitTypeOptions } from "@/constant";
import CheckModal from "@/components/CheckModal.vue";
import ListFilterSelect from "@/components/ListFilterSelect.vue";
import { utils } from "@halo-dev/ui-shared";

const queryClient = useQueryClient();

const selectedLinkSubmits = ref<string[]>([]);
const checkedAll = ref(false);
const keyword = useRouteQuery<string>("keyword", "");
const selectedSort = useRouteQuery<string | undefined>("sort");
const selectedStatus = useRouteQuery<string | undefined>("status");
const selectedType = useRouteQuery<string | undefined>("type");

const page = ref(1);
const size = ref(20);
const total = ref(0);

watch(
  () => [selectedSort.value, keyword.value, selectedStatus.value, selectedType.value],
  () => {
    page.value = 1;
    selectedLinkSubmits.value = [];
    checkedAll.value = false;
  }
);

function handleClearFilters() {
  keyword.value = "";
  selectedSort.value = undefined;
  selectedStatus.value = undefined;
  selectedType.value = undefined;
}

const hasFilters = computed(() => {
  return Boolean(keyword.value.trim() || selectedSort.value || selectedStatus.value || selectedType.value);
});

const {
  data: linkSubmits,
  isLoading,
  isFetching,
  refetch,
} = useQuery({
  queryKey: ["link-submits", page, size, selectedSort, selectedStatus, selectedType, keyword],
  queryFn: async () => {
    const { data } = await linkSubmitApiClient.linkSubmit.listLinkSubmits({
      page: page.value,
      size: size.value,
      sort: [selectedSort.value].filter(Boolean) as string[],
      status: selectedStatus.value,
      type: selectedType.value,
      keyword: keyword.value,
    });
    total.value = data.total;
    return data.items;
  },
  refetchInterval: (data) => {
    const deleting = data?.filter((linkSubmit) => !!linkSubmit.metadata.deletionTimestamp);
    return deleting?.length ? 1000 : false;
  },
});

const { data: groups } = useQuery<LinkGroup[]>({
  queryKey: ["link-groups"],
  queryFn: async () => {
    const { data } = await axiosInstance.get<LinkGroupList>(
      `/apis/core.halo.run/v1alpha1/linkgroups`
    );
    return data.items
      .map((group) => {
        if (group.spec) {
          group.spec.priority = group.spec.priority || 0;
        }
        return group;
      })
      .sort((a, b) => {
        return (a.spec?.priority || 0) - (b.spec?.priority || 0);
      });
  },
  refetchOnWindowFocus: false,
  refetchInterval(data) {
    const hasDeletingData = data?.some((group) => {
      return !!group.metadata.deletionTimestamp;
    });
    return hasDeletingData ? 1000 : false;
  },
});

function getGroup(groupName: string) {
  const linkGroup = groups.value?.find((group) => group.metadata.name === groupName);
  return linkGroup?.spec?.displayName || "未分组";
}

function getStatusType(status: string) {
  switch (status) {
    case "review":
      return "success";
    case "pending":
      return "warning";
    case "refuse":
      return "danger";
    default:
      return "default";
  }
}

function getTypeType(type: string) {
  return type === "add" ? "primary" : "info";
}

function statusText(status: string) {
  const item = linkSubmitStatusOptions.find((option) => option.value === status);
  return item ? item.label : "未知";
}

function typeText(type: string) {
  const item = linkSubmitTypeOptions.find((option) => option.value === type);
  return item ? item.label : "未知";
}

const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  checkedAll.value = checked;
  if (checkedAll.value) {
    selectedLinkSubmits.value =
      linkSubmits.value?.map((linkSubmit) => linkSubmit.metadata.name) || [];
  } else {
    selectedLinkSubmits.value.length = 0;
  }
};

const deleteMutation = useMutation({
  mutationFn: (name: string) =>
    linkSubmitCoreApiClient.linkSubmit.deleteLinkSubmit({ name }),
  onSuccess: () => {
    Toast.success("删除成功");
  },
  onError: () => {
    Toast.error("删除失败");
  },
  onSettled: () => {
    queryClient.invalidateQueries({ queryKey: ["link-submits"] });
  },
});

const handleDeleteInBatch = () => {
  Dialog.warning({
    title: "是否确认删除所选的链接？",
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    onConfirm: async () => {
      for (const name of selectedLinkSubmits.value) {
        await deleteMutation.mutateAsync(name);
      }
      selectedLinkSubmits.value.length = 0;
      checkedAll.value = false;
    },
  });
};

const handleDelete = (linkSubmit: LinkSubmit) => {
  Dialog.warning({
    title: "确定删除吗？",
    description: "此操作不可逆，确定吗？",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: () => {
      deleteMutation.mutate(linkSubmit.metadata.name);
    },
  });
};

const linkSubmitCheckModal = ref(false);
const selectedLinkSubmit = ref<LinkSubmit>();
const handleOpenCheckModal = (linkSubmit?: LinkSubmit) => {
  selectedLinkSubmit.value = linkSubmit;
  linkSubmitCheckModal.value = true;
};
</script>
<template>
  <CheckModal
    v-if="linkSubmitCheckModal && selectedLinkSubmit"
    :link-submit="selectedLinkSubmit"
    @close="linkSubmitCheckModal = false"
  />
  <VCard :body-class="[':uno: !p-0']">
    <template #header>
      <div class=":uno: block w-full bg-gray-50 px-4 py-3">
        <div class=":uno: relative flex flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center">
          <div class=":uno: hidden items-center sm:flex" v-permission="['plugin:link:submit-next:manage']">
            <input
              v-model="checkedAll"
              type="checkbox"
              @change="handleCheckAllChange"
            />
          </div>
          <div class=":uno: flex w-full flex-1 items-center sm:w-auto">
            <VSpace v-if="selectedLinkSubmits.length" v-permission="['plugin:link:submit-next:manage']">
              <VButton type="danger" @click="handleDeleteInBatch">
                删除
              </VButton>
            </VSpace>
            <SearchInput
              v-else
              v-model="keyword"
            />
          </div>
          <VSpace spacing="lg" class=":uno: flex-wrap">
            <FilterCleanButton
              v-if="hasFilters"
              @click="handleClearFilters"
            />
            <ListFilterSelect
              v-model="selectedStatus"
              label="状态"
              :items="[
                {
                  label: '全部',
                  value: undefined,
                },
                ...linkSubmitStatusOptions
              ]"
            />
            <ListFilterSelect
              v-model="selectedType"
              label="类型"
              :items="[
                {
                  label: '全部',
                  value: undefined,
                },
                ...linkSubmitTypeOptions
              ]"
            />
            <div class=":uno: flex flex-row items-end gap-2">
              <VButton v-tooltip="'刷新'" size="sm" ghost @click="refetch()">
                <template #icon>
                <IconRefreshLine
                  :class="{ 'animate-spin': isFetching }"
                  class=":uno: h-4 w-4"
                />
                </template>
              </VButton>
            </div>
          </VSpace>
        </div>
      </div>
    </template>
    <VLoading v-if="isLoading" />

    <Transition v-else-if="!linkSubmits?.length" appear name="fade">
      <VEmpty
        message="请尝试刷新"
        title="当前没有待审核的链接"
      >
        <template #actions>
          <VSpace>
            <VButton @click="refetch()"> 刷新 </VButton>
          </VSpace>
        </template>
      </VEmpty>
    </Transition>

    <Transition v-else appear name="fade">
      <VEntityContainer>
        <VEntity
          v-for="linkSubmit in linkSubmits"
          :key="linkSubmit.metadata.name"
        >
          <template #start>
            <VEntityField v-permission="['plugin:link:submit-next:manage']">
              <input
                v-model="selectedLinkSubmits"
                :value="linkSubmit.metadata.name"
                type="checkbox"
                class=":uno: h-4 w-4 rounded border-gray-300 text-indigo-600"
              />
            </VEntityField>
            <VEntityField>
              <template #description>
                <img
                  v-if="linkSubmit?.spec.logo"
                  :src="linkSubmit.spec.logo"
                  :alt="linkSubmit?.spec.displayName"
                  class=":uno: h-10 w-10 rounded-full object-cover"
                />
                <span v-else class=":uno: flex h-10 w-10 items-center justify-center rounded-full bg-gray-100 text-sm text-gray-500">
                  {{ linkSubmit?.spec.displayName?.slice(0, 1) || '?' }}
                </span>
              </template>
            </VEntityField>
            <VEntityField
              :description="linkSubmit?.spec.displayName"
            />
          </template>

          <template #end>
            <VEntityField>
              <template #description>
                <a :href="linkSubmit?.spec.url" target="_blank" class=":uno: text-sm text-blue-600 hover:underline">
                  {{ linkSubmit?.spec.url }}
                </a>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <span class=":uno: text-sm text-gray-500">{{ linkSubmit?.spec.description }}</span>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <span class=":uno: text-sm text-gray-500">{{ linkSubmit?.spec.email }}</span>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <span class=":uno: text-sm text-gray-500">{{ getGroup(linkSubmit?.spec.groupName || '') }}</span>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <VTag :type="getStatusType(linkSubmit?.spec.status)" size="sm">
                  {{ statusText(linkSubmit?.spec.status) }}
                </VTag>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <VTag :type="getTypeType(linkSubmit?.spec.type)" size="sm">
                  {{ typeText(linkSubmit?.spec.type) }}
                </VTag>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <span class=":uno: text-sm text-gray-500">
                  {{ utils.date.format(linkSubmit?.metadata.creationTimestamp) }}
                </span>
              </template>
            </VEntityField>
            <VEntityField v-permission="['plugin:link:submit-next:manage']" class=":uno: min-w-[140px]">
              <template #description>
                <VSpace>
                  <VButton
                    v-if="linkSubmit.spec.status == LinkSubmitSpecStatusEnum.Pending"
                    v-tooltip="'审核此提交'"
                    type="secondary"
                    size="sm"
                    @click="handleOpenCheckModal(linkSubmit)"
                  >审核</VButton>
                  <VButton
                    v-tooltip="'删除此提交'"
                    type="danger"
                    size="sm"
                    ghost
                    @click="handleDelete(linkSubmit)"
                  >删除</VButton>
                </VSpace>
              </template>
            </VEntityField>
          </template>
        </VEntity>
      </VEntityContainer>
    </Transition>

    <template v-if="total > 0" #footer>
      <VPagination
        v-model:page="page"
        v-model:size="size"
        :total="total"
        :size-options="[20, 30, 50, 100]"
      />
    </template>
  </VCard>
</template>
