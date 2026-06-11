package com.moneytracker

import android.app.Application
import com.moneytracker.data.repository.TransactionRepository

class MoneyTrackerApp : Application() {
    lateinit var repository: TransactionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = TransactionRepository(this)
    }

    companion object {
        lateinit var instance: MoneyTrackerApp
            private set
    }
}
