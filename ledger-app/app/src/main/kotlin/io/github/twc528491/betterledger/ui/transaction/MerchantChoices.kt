package io.github.twc528491.betterledger.ui.transaction

import io.github.twc528491.betterledger.data.repository.MerchantRepository
import io.github.twc528491.betterledger.domain.model.Merchant

internal object MerchantChoices {
    /**
     * 商家选择列表按名称去重：旧数据中可能存在多条同名商家记录，列表只显示一条。
     * 保留首次出现的位置；同名组内若有当前已选商家则显示它，保证选中态正确。
     */
    fun distinctByName(merchants: List<Merchant>, selectedId: String?): List<Merchant> {
        val groups = LinkedHashMap<String, Merchant>()
        merchants.forEach { merchant ->
            val key = MerchantRepository.nameKey(merchant.name)
            val existing = groups[key]
            if (existing == null || (merchant.id == selectedId && existing.id != selectedId)) {
                groups[key] = merchant
            }
        }
        return groups.values.toList()
    }
}
