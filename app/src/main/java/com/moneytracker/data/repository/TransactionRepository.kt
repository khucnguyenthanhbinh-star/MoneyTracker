package com.moneytracker.data.repository

import android.content.Context
import com.moneytracker.data.local.AppDatabase
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByDateRange(start, end)

    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByType(type)

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getTransactionById(id)

    suspend fun insert(t: TransactionEntity): Long = transactionDao.insertTransaction(t)

    suspend fun update(t: TransactionEntity) = transactionDao.updateTransaction(t)

    suspend fun delete(t: TransactionEntity) = transactionDao.deleteTransaction(t)

    suspend fun deleteAll() = transactionDao.deleteAllTransactions()

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)
}
