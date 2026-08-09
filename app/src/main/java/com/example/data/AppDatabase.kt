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

@Database(entities = [SavedPaper::class], version = 1, exportSchema = false)
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
