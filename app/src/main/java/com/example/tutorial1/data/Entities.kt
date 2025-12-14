package com.example.tutorial1.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: AccountType,
    val balance: Double
)

enum class AccountType { CORRENTE, CAIXINHA }

@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dueDay: Int,
    val openAmount: Double
)

@Entity(tableName = "salary")
data class Salary(
    @PrimaryKey val id: Int = 1,
    val amount: Double,
    val payDay: Int
)

@Entity(tableName = "vale_balances")
data class ValeBalance(
    @PrimaryKey val type: ValeType,
    val balance: Double
)

enum class ValeType { VALE_REFEICAO, VALE_ALIMENTACAO }

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Double,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val destinationType: DestinationType,
    val accountId: Int?
)

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Double,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val originAccountId: Int,
    val destinationAccountId: Int
)

enum class DestinationType {
    ACCOUNT, CREDIT_CARD, VALE_REFEICAO, VALE_ALIMENTACAO
}
