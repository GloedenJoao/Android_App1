package com.example.tutorial1.data

import java.time.LocalDate

data class Account(
    val id: Long = 0,
    val name: String,
    val type: String,
    val balance: Double
)

data class CreditCard(
    val id: Long = 0,
    val name: String,
    val dueDay: Int,
    val openAmount: Double
)

data class Salary(
    val id: Long = 0,
    val amount: Double,
    val payday: Int
)

data class ValeBalance(
    val id: Long = 0,
    val valeType: String,
    val balance: Double
)

data class Transaction(
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val date: LocalDate,
    val targetType: String,
    val accountId: Long?
)

data class Transfer(
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val date: LocalDate,
    val fromAccountId: Long,
    val toAccountId: Long
)

data class FutureEvent(
    val id: Long = 0,
    val date: LocalDate,
    val description: String,
    val amount: Double,
    val target: String,
    val source: String? = null
)

data class DailyProjection(
    val date: LocalDate,
    val accountBalances: Map<String, Double>,
    val valeBalances: Map<String, Double>,
    val creditCardAmount: Double,
    val totalAccounts: Double,
    val totalVales: Double
)
