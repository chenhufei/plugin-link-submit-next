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
  VDropdownDivider,
  VDropdownItem,
  VStatusDot,
  Dialog,
  Toast,
  IconExternalLinkLine,
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

function stopEventPropagation(event?: MouseEvent) {
  event?.stopPropagation();
}

const hasFilters = computed(() => {
  return Boolean(keyword.value.trim() || selectedSort.value || selectedStatus.value || selectedType.value);
});

const sortOptions = [
  { label: "最新提交", value: "metadata.creationTimestamp,desc" },
  { label: "最早提交", value: "metadata.creationTimestamp,asc" },
  { label: "名称 A-Z", value: "spec.displayName,asc" },
  { label: "名称 Z-A", value: "spec.displayName,desc" },
];

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

function getStatusState(status: string) {
  return ({ review: "success", pending: "warning", refuse: "error" } as Record<string, "success" | "warning" | "error" | "default">)[status] || "default";
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
    :group-label="getGroup(selectedLinkSubmit.spec.groupName || '')"
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
              placeholder="搜索名称、地址或邮箱"
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
            <ListFilterSelect
              v-model="selectedSort"
              label="排序"
              :items="sortOptions"
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
          :is-selected="selectedLinkSubmits.includes(linkSubmit.metadata.name)"
        >
          <template #checkbox>
            <input
              v-model="selectedLinkSubmits"
              :value="linkSubmit.metadata.name"
              type="checkbox"
            />
          </template>
          <template #start>
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
              :title="linkSubmit?.spec.displayName"
              :description="linkSubmit?.spec.url"
              class=":uno: min-w-0 max-w-[32rem] cursor-pointer"
              @click="handleOpenCheckModal(linkSubmit)"
            >
              <template #extra>
                <a
                  :href="linkSubmit?.spec.url"
                  target="_blank"
                  rel="noopener noreferrer"
                  class=":uno: text-gray-500 opacity-0 transition-opacity hover:text-gray-900 group-hover:opacity-100"
                  @click="stopEventPropagation"
                >
                  <IconExternalLinkLine class=":uno: h-3.5 w-3.5" />
                </a>
              </template>
            </VEntityField>
          </template>

          <template #end>
            <VEntityField>
              <template #description>
                <VStatusDot :state="getStatusState(linkSubmit?.spec.status)" :text="statusText(linkSubmit?.spec.status)" />
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <VTag :type="getTypeType(linkSubmit?.spec.type)" size="sm">{{ typeText(linkSubmit?.spec.type) }}</VTag>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <span class=":uno: block max-w-[9rem] truncate text-sm text-gray-500">{{ getGroup(linkSubmit?.spec.groupName || '') }}</span>
              </template>
            </VEntityField>
            <VEntityField v-if="linkSubmit?.metadata.creationTimestamp">
              <template #description>
                <span
                  v-tooltip="utils.date.format(linkSubmit.metadata.creationTimestamp)"
                  class=":uno: whitespace-nowrap text-sm text-gray-500"
                >{{ utils.date.timeAgo(linkSubmit.metadata.creationTimestamp) }}</span>
              </template>
            </VEntityField>
          </template>
          <template #dropdownItems>
            <VDropdownItem @click="handleOpenCheckModal(linkSubmit)">
              {{ linkSubmit.spec.status == LinkSubmitSpecStatusEnum.Pending ? "审核" : "查看详情" }}
            </VDropdownItem>
            <VDropdownDivider />
            <VDropdownItem type="danger" @click="handleDelete(linkSubmit)">删除</VDropdownItem>
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
