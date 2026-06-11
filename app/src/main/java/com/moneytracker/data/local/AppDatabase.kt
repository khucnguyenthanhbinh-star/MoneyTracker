package com.moneytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneytracker.data.local.dao.CategoryDao
import com.moneytracker.data.local.dao.TransactionDao
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moneytracker.db"
                )
                    .addCallback(SEED_CALLBACK)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private val SEED_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedCategories(db)
            }
        }

        private fun seedCategories(db: SupportSQLiteDatabase) {
            val expenses = listOf(
                "Ăn uống|🍔|-1773131",
                "Di chuyển|🚌|-312065",
                "Mua sắm|🛒|-12451769",
                "Hóa đơn|💡|-16590296",
                "Giải trí|🎵|-10700955",
                "Sức khỏe|💊|-16749824",
                "Giáo dục|📚|-13091795",
                "Nhà cửa|🏠|-9488199",
                "Bảo hiểm|🛡️|-11126150",
                "Khác|📦|-8882548"
            )
            val incomes = listOf(
                "Lương|💰|-13386958",
                "Thưởng|🏆|-546587",
                "Đầu tư|📈|-15509504",
                "Freelance|💻|-9828710",
                "Khác|💵|-8882548"
            )

            try {
                expenses.forEach { item ->
                    val parts = item.split("|")
                    db.execSQL(
                        "INSERT INTO categories (name, icon, color, type) VALUES (?, ?, ?, 'expense')",
                        arrayOf(parts[0], parts[1], parts[2].toLongOrNull() ?: -1L)
                    )
                }
                incomes.forEach { item ->
                    val parts = item.split("|")
                    db.execSQL(
                        "INSERT INTO categories (name, icon, color, type) VALUES (?, ?, ?, 'income')",
                        arrayOf(parts[0], parts[1], parts[2].toLongOrNull() ?: -1L)
                    )
                }
            } catch (_: Throwable) {
                // Silently ignore seed errors
            }
        }
    }
}
