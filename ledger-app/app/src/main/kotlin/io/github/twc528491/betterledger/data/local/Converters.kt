package io.github.twc528491.betterledger.data.local

import androidx.room.TypeConverter
import io.github.twc528491.betterledger.domain.model.AccountType
import io.github.twc528491.betterledger.domain.model.CategoryType
import io.github.twc528491.betterledger.domain.model.TransactionType

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
