package com.example.tutorial1.data

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class FinanceRepository(context: Context) {
    private val dbHelper = FinanceDatabaseHelper(context)

    suspend fun getAccounts(): List<Account> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("accounts", null, null, null, null, null, null)
        val accounts = mutableListOf<Account>()
        cursor.use {
            while (it.moveToNext()) {
                accounts.add(
                    Account(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        type = it.getString(it.getColumnIndexOrThrow("type")),
                        balance = it.getDouble(it.getColumnIndexOrThrow("balance"))
                    )
                )
            }
        }
        accounts
    }

    suspend fun upsertAccount(account: Account) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", account.name)
            put("type", account.type)
            put("balance", account.balance)
        }

        if (account.id == 0L) {
            db.insert("accounts", null, values)
        } else {
            db.update("accounts", values, "id=?", arrayOf(account.id.toString()))
        }
    }

    suspend fun deleteAccount(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete("accounts", "id=?", arrayOf(id.toString()))
    }

    suspend fun getCreditCards(): List<CreditCard> = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.query("credit_cards", null, null, null, null, null, null)
        val cards = mutableListOf<CreditCard>()
        cursor.use {
            while (it.moveToNext()) {
                cards.add(
                    CreditCard(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        dueDay = it.getInt(it.getColumnIndexOrThrow("due_day")),
                        openAmount = it.getDouble(it.getColumnIndexOrThrow("open_amount"))
                    )
                )
            }
        }
        cards
    }

    suspend fun upsertCreditCard(card: CreditCard) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("name", card.name)
            put("due_day", card.dueDay)
            put("open_amount", card.openAmount)
        }
        val db = dbHelper.writableDatabase
        if (card.id == 0L) {
            db.insert("credit_cards", null, values)
        } else {
            db.update("credit_cards", values, "id=?", arrayOf(card.id.toString()))
        }
    }

    suspend fun saveSalary(salary: Salary) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("amount", salary.amount)
            put("payday", salary.payday)
        }
        val db = dbHelper.writableDatabase
        if (salary.id == 0L) {
            db.insert("salary", null, values)
        } else {
            db.update("salary", values, "id=?", arrayOf(salary.id.toString()))
        }
    }

    suspend fun getSalary(): Salary? = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.query("salary", null, null, null, null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                return@withContext Salary(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    amount = it.getDouble(it.getColumnIndexOrThrow("amount")),
                    payday = it.getInt(it.getColumnIndexOrThrow("payday"))
                )
            }
        }
        null
    }

    suspend fun saveValeBalance(balance: ValeBalance) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("vale_type", balance.valeType)
            put("balance", balance.balance)
        }
        val db = dbHelper.writableDatabase
        val existing = db.query(
            "vale_balances",
            arrayOf("id"),
            "vale_type=?",
            arrayOf(balance.valeType),
            null,
            null,
            null
        )
        existing.use {
            if (it.moveToFirst()) {
                val id = it.getLong(0)
                db.update("vale_balances", values, "id=?", arrayOf(id.toString()))
            } else {
                db.insert("vale_balances", null, values)
            }
        }
    }

    suspend fun getValeBalances(): List<ValeBalance> = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.query("vale_balances", null, null, null, null, null, null)
        val list = mutableListOf<ValeBalance>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ValeBalance(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        valeType = it.getString(it.getColumnIndexOrThrow("vale_type")),
                        balance = it.getDouble(it.getColumnIndexOrThrow("balance"))
                    )
                )
            }
        }
        list
    }

    suspend fun insertTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("description", transaction.description)
            put("amount", transaction.amount)
            put("date", transaction.date.toString())
            put("target_type", transaction.targetType)
            put("account_id", transaction.accountId)
        }
        dbHelper.writableDatabase.insert("transactions", null, values)
    }

    suspend fun getTransactions(): List<Transaction> = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.query("transactions", null, null, null, null, null, "date")
        val list = mutableListOf<Transaction>()
        cursor.use {
            while (it.moveToNext()) {
                val accountIdIndex = it.getColumnIndexOrThrow("account_id")
                val accountId = if (!it.isNull(accountIdIndex)) it.getLong(accountIdIndex) else null

                list.add(
                    Transaction(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        description = it.getString(it.getColumnIndexOrThrow("description")),
                        amount = it.getDouble(it.getColumnIndexOrThrow("amount")),
                        date = LocalDate.parse(it.getString(it.getColumnIndexOrThrow("date"))),
                        targetType = it.getString(it.getColumnIndexOrThrow("target_type")),
                        accountId = accountId
                    )
                )
            }
        }
        list
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete("transactions", "id=?", arrayOf(id.toString()))
    }

    suspend fun insertTransfer(transfer: Transfer) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("description", transfer.description)
            put("amount", transfer.amount)
            put("date", transfer.date.toString())
            put("from_account_id", transfer.fromAccountId)
            put("to_account_id", transfer.toAccountId)
        }
        dbHelper.writableDatabase.insert("transfers", null, values)
    }

    suspend fun getTransfers(): List<Transfer> = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.query("transfers", null, null, null, null, null, "date")
        val list = mutableListOf<Transfer>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Transfer(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        description = it.getString(it.getColumnIndexOrThrow("description")),
                        amount = it.getDouble(it.getColumnIndexOrThrow("amount")),
                        date = LocalDate.parse(it.getString(it.getColumnIndexOrThrow("date"))),
                        fromAccountId = it.getLong(it.getColumnIndexOrThrow("from_account_id")),
                        toAccountId = it.getLong(it.getColumnIndexOrThrow("to_account_id"))
                    )
                )
            }
        }
        list
    }

    suspend fun insertFutureEvent(event: FutureEvent) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("date", event.date.toString())
            put("description", event.description)
            put("amount", event.amount)
            put("target", event.target)
            put("source", event.source)
        }
        dbHelper.writableDatabase.insert("future_events", null, values)
    }

    suspend fun deleteFutureEvent(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete("future_events", "id=?", arrayOf(id.toString()))
    }

    suspend fun getFutureEvents(): List<FutureEvent> = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.query("future_events", null, null, null, null, null, "date")
        val list = mutableListOf<FutureEvent>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    FutureEvent(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        date = LocalDate.parse(it.getString(it.getColumnIndexOrThrow("date"))),
                        description = it.getString(it.getColumnIndexOrThrow("description")),
                        amount = it.getDouble(it.getColumnIndexOrThrow("amount")),
                        target = it.getString(it.getColumnIndexOrThrow("target")),
                        source = it.getString(it.getColumnIndexOrThrow("source"))
                    )
                )
            }
        }
        list
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("accounts", null, null)
        db.delete("credit_cards", null, null)
        db.delete("salary", null, null)
        db.delete("vale_balances", null, null)
        db.delete("transactions", null, null)
        db.delete("transfers", null, null)
        db.delete("future_events", null, null)
    }
}
