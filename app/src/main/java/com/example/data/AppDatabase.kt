package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * A post in the feed: a researcher's commentary plus the structured metadata of the
 * paper it refers to.
 *
 * The paper metadata is stored as discrete fields rather than a pre-rendered citation
 * string so that [CitationFormatter] can render it into any style on demand.
 * [citationOverride] is the escape hatch for a citation that was supplied verbatim and
 * cannot be re-derived — it is what rows migrated from schema v1 carry.
 */
@Entity(tableName = "saved_papers")
data class SavedPaper(
    @PrimaryKey val id: String,
    val authorInitials: String,
    val authorName: String,
    val affiliation: String,
    /** The poster's own commentary — the "social" half of the post. */
    val content: String,
    val title: String = "",
    /** Semicolon-separated, each entry "Family, Given" — e.g. "Doe, Jane; Smith, John". */
    val authors: String = "",
    val year: String = "",
    val venue: String = "",
    val doi: String = "",
    val url: String = "",
    /** Epoch millis. Relative "2h ago" labels are derived from this, never stored. */
    val publishedAt: Long = 0L,
    /** Verbatim citation used when [title] is blank and nothing can be generated. */
    val citationOverride: String = "",
    val isEndorsed: Boolean = false
)

@Dao
interface SavedPaperDao {
    @Query("SELECT * FROM saved_papers ORDER BY publishedAt DESC")
    fun getAllPapers(): Flow<List<SavedPaper>>

    @Query("SELECT * FROM saved_papers WHERE id = :id")
    fun getPaper(id: String): Flow<SavedPaper?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: SavedPaper)

    @Query("DELETE FROM saved_papers WHERE id = :id")
    suspend fun deletePaper(id: String)

    @Query("UPDATE saved_papers SET isEndorsed = :endorsed WHERE id = :id")
    suspend fun updateEndorsement(id: String, endorsed: Boolean)
}

@Database(entities = [SavedPaper::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedPaperDao(): SavedPaperDao

    companion object {
        /**
         * v1 stored a pre-rendered `citation` string and a frozen `timeAgo` label. v2
         * replaces both with structured fields, so the table is recreated and the old
         * citation text is preserved in `citationOverride`.
         *
         * No column carries a SQL DEFAULT: the entity declares Kotlin defaults only, and
         * Room's runtime schema validation compares default values literally. Every
         * column is therefore given an explicit value by the INSERT instead.
         */
        fun migration1To2(nowMillis: Long): Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_papers_new` (
                        `id` TEXT NOT NULL,
                        `authorInitials` TEXT NOT NULL,
                        `authorName` TEXT NOT NULL,
                        `affiliation` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `authors` TEXT NOT NULL,
                        `year` TEXT NOT NULL,
                        `venue` TEXT NOT NULL,
                        `doi` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `publishedAt` INTEGER NOT NULL,
                        `citationOverride` TEXT NOT NULL,
                        `isEndorsed` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `saved_papers_new` (
                        `id`, `authorInitials`, `authorName`, `affiliation`, `content`,
                        `title`, `authors`, `year`, `venue`, `doi`, `url`,
                        `publishedAt`, `citationOverride`, `isEndorsed`
                    )
                    SELECT `id`, `authorInitials`, `authorName`, `affiliation`, `content`,
                           '', '', '', '', '', '',
                           $nowMillis, `citation`, `isEndorsed`
                    FROM `saved_papers`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `saved_papers`")
                db.execSQL("ALTER TABLE `saved_papers_new` RENAME TO `saved_papers`")
            }
        }
    }
}

class PaperRepository(private val dao: SavedPaperDao) {
    val allPapers: Flow<List<SavedPaper>> = dao.getAllPapers()

    fun paper(id: String): Flow<SavedPaper?> = dao.getPaper(id)

    suspend fun savePaper(paper: SavedPaper) = dao.insertPaper(paper)

    suspend fun deletePaper(id: String) = dao.deletePaper(id)

    suspend fun toggleEndorsement(id: String, currentStatus: Boolean) {
        dao.updateEndorsement(id, !currentStatus)
    }
}
