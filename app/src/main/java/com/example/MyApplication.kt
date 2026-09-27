package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.PaperRepository
import com.example.data.SettingsRepository
import com.example.data.auth.UserSessionManager
import com.example.data.chat.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: PaperRepository
    lateinit var settings: SettingsRepository
    lateinit var chatRepository: ChatRepository
    lateinit var sessionManager: UserSessionManager

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "folio_db")
            .addMigrations(
                AppDatabase.migration1To2(System.currentTimeMillis()),
                AppDatabase.migration2To3(),
                AppDatabase.migration3To4(),
                AppDatabase.migration4To5(),
                AppDatabase.migration5To6(),
                AppDatabase.migration6To7(),
                AppDatabase.migration7To8(),
                AppDatabase.migration8To9(),
                AppDatabase.migration9To10()
            )
            .build()
        repository = PaperRepository(database)
        settings = SettingsRepository(this)
        chatRepository = ChatRepository(database, this)
        sessionManager = UserSessionManager(this, database)
        CoroutineScope(Dispatchers.IO).launch {
            sessionManager.purgePlaceholderData()
        }
    }
}
