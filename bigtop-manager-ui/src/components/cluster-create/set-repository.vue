<script setup lang="ts">
  import { reactive, UnwrapRef } from 'vue'
  import { CheckOutlined, EditOutlined } from '@ant-design/icons-vue'
  import { useI18n } from 'vue-i18n'
  import { useStackStore } from '@/store/stack'
  import { storeToRefs } from 'pinia'
  import { StackRepoVO } from '@/api/stack/types.ts'
  import _ from 'lodash'

  const clusterInfo = defineModel<any>('clusterInfo')

  const { t } = useI18n()
  const stackStore = useStackStore()

  const editableData: UnwrapRef<Record<number, StackRepoVO>> = reactive({})
  const { stackRepos } = storeToRefs(stackStore)

  const repositoryData: StackRepoVO[] = _.cloneDeep(
    stackRepos.value[clusterInfo.value.fullStackName]
  )

  const repositoryColumns = [
    {
      title: t('common.os'),
      dataIndex: 'os',
      align: 'center',
      width: 150
    },
    {
      title: t('common.arch'),
      dataIndex: 'arch',
      align: 'center',
      width: 150
    },
    {
      title: t('common.base_url'),
      dataIndex: 'baseUrl',
      align: 'center',
      ellipsis: true
    }
  ]

  const edit = (index: number) => {
    editableData[index] = _.cloneDeep(repositoryData[index])
  }

  const save = (index: number) => {
    Object.assign(repositoryData[index], editableData[index])

    delete editableData[index]
  }

  const onNextStep = async () => {
    Object.assign(clusterInfo.value.repoInfoList, repositoryData)

    return Promise.resolve(true)
  }

  defineExpose({
    onNextStep
  })
</script>

<template>
  <div class="container">
    <div class="title">{{ $t('cluster.set_repository') }}</div>
    <a-table
      :pagination="false"
      :scroll="{ y: 400 }"
      :columns="repositoryColumns"
      :data-source="repositoryData"
    >
      <template #bodyCell="{ text, index, column }">
        <template v-if="column.dataIndex === 'baseUrl'">
          <div class="editable-cell">
            <div v-if="editableData[index]" class="editable-cell-input-wrapper">
              <a-input
                v-model:value="editableData[index].baseUrl"
                @press-enter="save(index)"
              />
              <check-outlined
                class="editable-cell-icon-check"
                @click="save(index)"
              />
            </div>
            <div v-else class="editable-cell-text-wrapper">
              {{ text || ' ' }}
              <edit-outlined class="editable-cell-icon" @click="edit(index)" />
            </div>
          </div>
        </template>
      </template>
    </a-table>
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

    .editable-cell {
      position: relative;

      .editable-cell-input-wrapper {
        padding-right: 24px;
        display: flex;
        justify-content: center;
        align-items: center;

        .editable-cell-icon-check {
          position: absolute;
          line-height: 28px;
          right: 0;
          width: 20px;
          cursor: pointer;

          &:hover {
            color: #108ee9;
          }
        }
      }

      .editable-cell-text-wrapper {
        display: flex;
        justify-content: center;
        align-items: center;
        padding: 5px 24px 5px 5px;

        .editable-cell-icon {
          display: none;
          position: absolute;
          margin-top: 4px;
          right: 0;
          width: 20px;
          cursor: pointer;

          &:hover {
            color: #108ee9;
          }
        }
      }

      &:hover {
        .editable-cell-icon {
          display: inline-block;
        }
      }
    }
  }
</style>
