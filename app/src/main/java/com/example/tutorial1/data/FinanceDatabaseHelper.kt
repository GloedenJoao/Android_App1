package com.example.tutorial1.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DATABASE_NAME = "finance.db"
private const val DATABASE_VERSION = 1

class FinanceDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                balance REAL NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE credit_cards (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                due_day INTEGER NOT NULL,
                open_amount REAL NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE salary (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount REAL NOT NULL,
                payday INTEGER NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE vale_balances (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                vale_type TEXT NOT NULL,
                balance REAL NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                date TEXT NOT NULL,
                target_type TEXT NOT NULL,
                account_id INTEGER
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE transfers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                date TEXT NOT NULL,
                from_account_id INTEGER NOT NULL,
                to_account_id INTEGER NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE future_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                target TEXT NOT NULL,
                source TEXT
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS accounts")
            db.execSQL("DROP TABLE IF EXISTS credit_cards")
            db.execSQL("DROP TABLE IF EXISTS salary")
            db.execSQL("DROP TABLE IF EXISTS vale_balances")
            db.execSQL("DROP TABLE IF EXISTS transactions")
            db.execSQL("DROP TABLE IF EXISTS transfers")
            db.execSQL("DROP TABLE IF EXISTS future_events")
            onCreate(db)
        }
    }
}
