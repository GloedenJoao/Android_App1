package com.example.tutorial1.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun dateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun fromAccountType(value: String?): AccountType? = value?.let { AccountType.valueOf(it) }

    @TypeConverter
    fun accountTypeToString(value: AccountType?): String? = value?.name

    @TypeConverter
    fun fromDestinationType(value: String?): DestinationType? = value?.let { DestinationType.valueOf(it) }

    @TypeConverter
    fun destinationTypeToString(value: DestinationType?): String? = value?.name

    @TypeConverter
    fun fromValeType(value: String?): ValeType? = value?.let { ValeType.valueOf(it) }

    @TypeConverter
    fun valeTypeToString(value: ValeType?): String? = value?.name
}
