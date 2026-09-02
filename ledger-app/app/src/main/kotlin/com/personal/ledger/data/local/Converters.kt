package com.personal.ledger.data.local

import androidx.room.TypeConverter
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.TransactionType

class Converters {
    @TypeConverter
    fun transactionTypeToString(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun stringToTransactionType(value: String?): TransactionType? =
        value?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun accountTypeToString(value: AccountType?): String? = value?.name

    @TypeConverter
    fun stringToAccountType(value: String?): AccountType? =
        value?.let { AccountType.valueOf(it) }

    @TypeConverter
    fun categoryTypeToString(value: CategoryType?): String? = value?.name

    @TypeConverter
    fun stringToCategoryType(value: String?): CategoryType? =
        value?.let { CategoryType.valueOf(it) }
}
