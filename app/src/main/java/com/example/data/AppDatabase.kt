package com.example.data

import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Note the columns added in v2 all carry defaults. That is what lets Room generate the migration
 * automatically -- adding a NOT NULL column with a default is a purely additive change.
 */
@Entity(tableName = "saved_papers")
data class SavedPaper(
    @PrimaryKey val id: String,
    val authorInitials: String,
    val authorName: String,
    /**
     * Kept for the seeded rows, but no longer the source of truth for ordering. It is a literal
     * String ("2h ago") baked in at insert time, which is why the feed could not be sorted
     * chronologically. Display now derives from createdAt via TimeFormat.relative().
     */
    val timeAgo: String,
    val affiliation: String,
    val content: String,
    val citation: String,
    val isEndorsed: Boolean = false,

    // --- added in v2 ---
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(defaultValue = "''")
    val authorId: String = "",
    val imageUrl: String? = null,
    val fieldKey: String? = null,
    @ColumnInfo(defaultValue = "0")
    val insightfulCount: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val citeWorthyCount: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val commentCount: Int = 0,
    /** ENDORSE / INSIGHTFUL / CITE_WORTHY, or null. */
    val myReaction: String? = null,
)

@Entity(
    tableName = "comments",
    indices = [Index("postId")],
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorInitials: String,
    val body: String,
    val createdAt: Long,
)

@Dao
interface SavedPaperDao {
    @Query("SELECT * FROM saved_papers ORDER BY createdAt DESC")
    fun getAllPapers(): Flow<List<SavedPaper>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: SavedPaper)

    @Query("DELETE FROM saved_papers WHERE id = :id")
    suspend fun deletePaper(id: String)

    @Query("UPDATE saved_papers SET isEndorsed = :endorsed WHERE id = :id")
    suspend fun updateEndorsement(id: String, endorsed: Boolean)

    @Query("UPDATE saved_papers SET myReaction = :reaction WHERE id = :id")
    suspend fun updateReaction(id: String, reaction: String?)

    @Query("UPDATE saved_papers SET commentCount = commentCount + :delta WHERE id = :id")
    suspend fun bumpCommentCount(id: String, delta: Int)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun getComments(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity)
}

/**
 * v1 -> v2 is a generated migration, which is only possible because
 * app/schemas/com.example.data.AppDatabase/1.json was captured and committed while the database
 * was still at version 1. Every new column has a default and the new table is additive, so Room
 * writes the SQL itself -- no hand-written statements to keep in sync with the entity definitions,
 * and no schema-hash mismatch to debug at runtime.
 */
@Database(
    entities = [SavedPaper::class, CommentEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedPaperDao(): SavedPaperDao
    abstract fun commentDao(): CommentDao
}

class PaperRepository(
    private val dao: SavedPaperDao,
    private val commentDao: CommentDao,
) {
    val allPapers: Flow<List<SavedPaper>> = dao.getAllPapers()

    suspend fun savePaper(paper: SavedPaper) = dao.insertPaper(paper)

    suspend fun deletePaper(id: String) = dao.deletePaper(id)

    suspend fun toggleEndorsement(id: String, currentStatus: Boolean) {
        dao.updateEndorsement(id, !currentStatus)
    }

    suspend fun setReaction(id: String, reaction: String?) = dao.updateReaction(id, reaction)

    fun comments(postId: String): Flow<List<CommentEntity>> = commentDao.getComments(postId)

    suspend fun addComment(comment: CommentEntity) {
        commentDao.insert(comment)
        dao.bumpCommentCount(comment.postId, 1)
    }
}
