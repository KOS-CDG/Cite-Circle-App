package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_papers")
data class SavedPaper(
    @PrimaryKey val id: String,
    val authorInitials: String,
    val authorName: String,
    val timeAgo: String,
    val affiliation: String,
    val content: String,
    val citation: String,
    val isEndorsed: Boolean = false
)

@Dao
interface SavedPaperDao {
    @Query("SELECT * FROM saved_papers")
    fun getAllPapers(): Flow<List<SavedPaper>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: SavedPaper)

    @Query("DELETE FROM saved_papers WHERE id = :id")
    suspend fun deletePaper(id: String)
    
    @Query("UPDATE saved_papers SET isEndorsed = :endorsed WHERE id = :id")
    suspend fun updateEndorsement(id: String, endorsed: Boolean)
}

/**
 * exportSchema is intentionally `true`, and the version is intentionally still 1.
 *
 * Room can only generate an `@AutoMigration(from = 1, to = 2)` if the exported JSON schema for
 * BOTH endpoints exists on disk. This project shipped with `exportSchema = false`, so no v1 JSON
 * was ever written -- and it cannot be produced retroactively once the version moves past 1.
 *
 * So: build once at version 1 with this flag on, and commit the generated
 * `app/schemas/com.example.data.AppDatabase/1.json`. Every later schema change is then a
 * one-line annotation instead of hand-written SQL that has to match Room's internal schema hash.
 */
@Database(entities = [SavedPaper::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedPaperDao(): SavedPaperDao
}

class PaperRepository(private val dao: SavedPaperDao) {
    val allPapers: Flow<List<SavedPaper>> = dao.getAllPapers()

    suspend fun savePaper(paper: SavedPaper) = dao.insertPaper(paper)
    
    suspend fun deletePaper(id: String) = dao.deletePaper(id)
    
    suspend fun toggleEndorsement(id: String, currentStatus: Boolean) {
        dao.updateEndorsement(id, !currentStatus)
    }
}
