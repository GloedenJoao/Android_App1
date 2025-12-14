package com.example.tutorial1.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Account::class,
        CreditCard::class,
        Salary::class,
        ValeBalance::class,
        TransactionEntity::class,
        TransferEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getInstance(context: Context): FinanceDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): FinanceDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                FinanceDatabase::class.java,
                "finance.db"
            ).fallbackToDestructiveMigration().build()
    }
}
