<script setup lang="ts">
  import { ref, onMounted, watch } from 'vue'
  import { useStackStore } from '@/store/stack'
  import { storeToRefs } from 'pinia'
  import { useI18n } from 'vue-i18n'
  import { StackServiceVO } from '@/api/stack/types.ts'

  const clusterInfo = defineModel<any>('clusterInfo')

  const { t } = useI18n()
  const stackStore = useStackStore()

  const selectedOption = ref<string[]>([])
  const serviceData = ref<StackServiceVO[]>([])
  const { stackOptions, stackServices } = storeToRefs(stackStore)

  watch(selectedOption, (val) => {
    clusterInfo.value.stackName = val[0]
    clusterInfo.value.stackVersion = val[1]
    clusterInfo.value.fullStackName = val[0] + '-' + val[1]

    serviceData.value = stackServices.value[clusterInfo.value.fullStackName]
  })

  const serviceColumns = [
    {
      title: t('common.name'),
      dataIndex: 'displayName',
      align: 'center',
      width: 150
    },
    {
      title: t('common.version'),
      dataIndex: 'serviceVersion',
      align: 'center',
      width: 150
    },
    {
      title: t('common.desc'),
      dataIndex: 'serviceDesc',
      align: 'center',
      ellipsis: true
    }
  ]

  onMounted(async () => {
    if (clusterInfo.value.fullStackName) {
      selectedOption.value = [
        clusterInfo.value.stackName,
        clusterInfo.value.stackVersion
      ]
    } else {
      selectedOption.value = [
        `${stackOptions.value?.[0]?.value}`,
        `${stackOptions.value?.[0]?.children?.[0]?.value}`
      ]
    }
  })

  const onNextStep = async () => {
    return Promise.resolve(true)
  }

  defineExpose({
    onNextStep
  })
</script>

<template>
  <div class="container">
    <div class="title">{{ $t('cluster.choose_stack') }}</div>
    <a-cascader
      v-model:value="selectedOption"
      :options="stackOptions"
      :placeholder="$t('common.select_tips')"
    />
    <div class="space" />
    <a-table
      :pagination="false"
      :scroll="{ y: 400 }"
      :columns="serviceColumns"
      :data-source="serviceData"
    />
  </div>
</template>

<style scoped lang="scss">
  .container {
    display: flex;
    flex-direction: column;
    justify-content: start;
    align-items: center;
    align-content: center;
    height: 100%;

    .title {
      font-size: 1.5rem;
      line-height: 2rem;
      margin-bottom: 1rem;
    }

    .space {
      margin: 1rem 0;
    }
  }
</style>
