package com.personal.ledger.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.personal.ledger.domain.model.AccountType

/**
 * 账户（v5 起为全局实体，所有账本共享）。
 * 某账本可使用的账户为 `currency == Ledger.baseCurrency` 的账户；余额汇总全部账本中的交易。
 */
@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: AccountType,
    val currency: String,
    val initialBalance: Long,
    val initialBalanceTime: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
)
