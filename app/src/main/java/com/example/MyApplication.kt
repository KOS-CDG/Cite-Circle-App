package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.PaperRepository
import com.example.data.messenger.InMemoryMessengerRepository
import com.example.data.messenger.MessengerRepository
import com.example.data.people.InMemoryPeopleRepository
import com.example.data.people.PeopleRepository
import com.example.data.prefs.SettingsStore

class MyApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: PaperRepository
    lateinit var settingsStore: SettingsStore

    /**
     * Application-scoped on purpose. If the messenger repository were owned by a ViewModel,
     * every message sent would be discarded the moment the thread left the back stack.
     */
    val messengerRepository: MessengerRepository by lazy { InMemoryMessengerRepository() }

    /** Reads its user records from the messenger, so the two can never disagree about a person. */
    val peopleRepository: PeopleRepository by lazy {
        InMemoryPeopleRepository(messengerRepository)
    }

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        // Destructive fallback is deliberate and safe here: every row in this database is seeded
        // sample data (see HomeViewModel.init), so there is no user-authored content to lose.
        // Without it, bumping the schema version throws
        // "A migration from 1 to 2 was required but not found" on first DB access.
        database = Room.databaseBuilder(this, AppDatabase::class.java, "folio_db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
        repository = PaperRepository(database.savedPaperDao(), database.commentDao())
    }
}
