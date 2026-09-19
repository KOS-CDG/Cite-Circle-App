package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.PaperRepository
import com.example.data.SettingsRepository
import com.example.data.chat.ChatRepository

class MyApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: PaperRepository
    lateinit var settings: SettingsRepository
    lateinit var chatRepository: ChatRepository

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "folio_db")
            .addMigrations(
                AppDatabase.migration1To2(System.currentTimeMillis()),
                AppDatabase.migration2To3(),
                AppDatabase.migration3To4()
            )
            .build()
        repository = PaperRepository(database)
        settings = SettingsRepository(this)
        chatRepository = ChatRepository(database, this)
    }
}
