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
        // Destructive fallback is deliberate and safe here: every row in this database is seeded
        // sample data (see HomeViewModel.init), so there is no user-authored content to lose.
        // Without it, bumping the schema version throws
        // "A migration from 1 to 2 was required but not found" on first DB access.
        database = Room.databaseBuilder(this, AppDatabase::class.java, "folio_db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
        repository = PaperRepository(database.savedPaperDao())
    }
}
