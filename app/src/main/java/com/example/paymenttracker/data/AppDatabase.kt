package com.example.paymenttracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PaymentEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payments ADD COLUMN bankName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE payments ADD COLUMN accountName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE payments ADD COLUMN accountNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE payments ADD COLUMN branchCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE payments ADD COLUMN paymentReference TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE payments ADD COLUMN isTaxDeductible INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payments ADD COLUMN frequency TEXT NOT NULL DEFAULT 'Once'")
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payments ADD COLUMN isTaxDeductible INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payments ADD COLUMN frequency TEXT NOT NULL DEFAULT 'Once'")
            }
        }

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS payments_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        beneficiary TEXT NOT NULL,
                        note TEXT NOT NULL,
                        bankName TEXT NOT NULL,
                        accountName TEXT NOT NULL,
                        accountNumber TEXT NOT NULL,
                        branchCode TEXT NOT NULL,
                        paymentReference TEXT NOT NULL,
                        isTaxDeductible INTEGER NOT NULL,
                        frequency TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO payments_new (
                        id, date, amountCents, beneficiary, note,
                        bankName, accountName, accountNumber, branchCode, paymentReference,
                        isTaxDeductible, frequency
                    )
                    SELECT
                        id,
                        CASE
                            WHEN date IS NOT NULL THEN date
                            ELSE '1970-01-01'
                        END as date,
                        amountCents,
                        beneficiary,
                        note,
                        bankName,
                        accountName,
                        accountNumber,
                        branchCode,
                        paymentReference,
                        0,
                        'Once'
                    FROM payments
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE payments")
                db.execSQL("ALTER TABLE payments_new RENAME TO payments")
            }
        }

        // NEW: add slip support
        private val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payments ADD COLUMN slipImageUri TEXT")
                db.execSQL("ALTER TABLE payments ADD COLUMN slipRawText TEXT")
            }
        }

        private val migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(payments)")
                val existingColumns = mutableSetOf<String>()

                cursor.use {
                    val nameIndex = it.getColumnIndex("name")
                    while (it.moveToNext()) {
                        existingColumns.add(it.getString(nameIndex))
                    }
                }

                if (!existingColumns.contains("slipImageUri")) {
                    db.execSQL("ALTER TABLE payments ADD COLUMN slipImageUri TEXT")
                }

                if (!existingColumns.contains("slipRawText")) {
                    db.execSQL("ALTER TABLE payments ADD COLUMN slipRawText TEXT")
                }
            }
        }

        // Add this migration inside the companion object, after migration5To6:
        private val migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE payments ADD COLUMN matchStatus TEXT NOT NULL DEFAULT 'unmatched'"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "payment_tracker.db"
                )
                    .addMigrations(
                        migration1To2,
                        migration2To3,
                        migration3To4,
                        migration4To5,
                        migration5To6,
                        migration6To7
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}