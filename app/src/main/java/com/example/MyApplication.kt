package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.PaperRepository

class MyApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: PaperRepository

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "folio_db")
            .addMigrations(AppDatabase.migration1To2(System.currentTimeMillis()))
            .build()
        repository = PaperRepository(database)
    }
}
