package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.chat.ChatMessageDao
import com.example.data.chat.ChatMessageEntity
import com.example.data.chat.ConversationDao
import com.example.data.chat.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * A post in the feed: a researcher's commentary, an optional image, the structured metadata
 * of the paper it refers to, and its engagement state.
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

    // --- engagement -------------------------------------------------------
    val isEndorsed: Boolean = false,
    val endorsementCount: Int = 0,
    val commentCount: Int = 0,
    val repostCount: Int = 0,
    val isBookmarked: Boolean = false,

    /** Absolute path of an image copied into app storage by [ImageStore], or blank. */
    val imageUri: String = "",

    // --- quote/repost -----------------------------------------------------
    /**
     * When non-blank this post quotes another. The quoted author, title and commentary are
     * snapshotted rather than joined so a quote still renders after the original is deleted.
     */
    val quotedId: String = "",
    val quotedAuthorName: String = "",
    val quotedTitle: String = "",
    val quotedContent: String = "",

    // --- paper vault / pdf ------------------------------------------------
    val pdfUrl: String = "",
    val pdfLocalPath: String = "",
    val abstractText: String = "",
    val openAccess: Boolean = false
)

/**
 * An extension rather than a body property: Room ignores properties with no backing field,
 * but Firestore's reflective serializer would still pick the getter up and write a phantom
 * field to every synced document.
 */
val SavedPaper.isQuote: Boolean get() = quotedId.isNotBlank()

/** A reply on a post. */
@Entity(tableName = "comments", indices = [Index("paperId")])
data class Comment(
    @PrimaryKey val id: String,
    val paperId: String,
    val authorInitials: String,
    val authorName: String,
    val affiliation: String,
    val body: String,
    val createdAt: Long
)

/** One row of the Discover tab: a venue that actually appears in the library, and how often. */
data class VenueCount(val name: String, val count: Int)

@Dao
interface SavedPaperDao {
    @Query("SELECT * FROM saved_papers ORDER BY publishedAt DESC")
    fun getAllPapers(): Flow<List<SavedPaper>>

    @Query("SELECT * FROM saved_papers WHERE isBookmarked = 1 ORDER BY publishedAt DESC")
    fun getBookmarkedPapers(): Flow<List<SavedPaper>>

    /** Quote posts, newest first — one half of the activity feed. */
    @Query("SELECT * FROM saved_papers WHERE quotedId != '' ORDER BY publishedAt DESC LIMIT 50")
    fun getRecentQuotes(): Flow<List<SavedPaper>>

    @Query(
        "SELECT venue AS name, COUNT(*) AS count FROM saved_papers " +
            "WHERE venue != '' GROUP BY venue ORDER BY count DESC, name ASC"
    )
    fun getVenueCounts(): Flow<List<VenueCount>>

    @Query("SELECT * FROM saved_papers WHERE venue = :venue ORDER BY publishedAt DESC")
    fun getPapersInVenue(venue: String): Flow<List<SavedPaper>>

    @Query("SELECT * FROM saved_papers WHERE id = :id")
    fun getPaper(id: String): Flow<SavedPaper?>

    @Query("SELECT * FROM saved_papers WHERE id = :id")
    suspend fun findPaper(id: String): SavedPaper?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: SavedPaper)

    @Query("DELETE FROM saved_papers WHERE id = :id")
    suspend fun deletePaper(id: String)

    @Query(
        "UPDATE saved_papers SET isEndorsed = :endorsed, " +
            "endorsementCount = MAX(0, endorsementCount + :delta) WHERE id = :id"
    )
    suspend fun updateEndorsement(id: String, endorsed: Boolean, delta: Int)

    @Query("UPDATE saved_papers SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean)

    @Query("UPDATE saved_papers SET commentCount = :count WHERE id = :id")
    suspend fun setCommentCount(id: String, count: Int)

    @Query("UPDATE saved_papers SET repostCount = repostCount + 1 WHERE id = :id")
    suspend fun incrementRepostCount(id: String)

    @Query("SELECT * FROM saved_papers WHERE pdfLocalPath != '' OR pdfUrl != '' ORDER BY publishedAt DESC")
    fun getVaultPapers(): Flow<List<SavedPaper>>

    @Query("UPDATE saved_papers SET pdfLocalPath = :localPath WHERE id = :id")
    suspend fun updatePdfLocalPath(id: String, localPath: String)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE paperId = :paperId ORDER BY createdAt ASC")
    fun commentsFor(paperId: String): Flow<List<Comment>>

    @Query("SELECT * FROM comments WHERE paperId = :paperId ORDER BY createdAt ASC")
    suspend fun commentsForOnce(paperId: String): List<Comment>

    /** Replies across every post, newest first — the other half of the activity feed. */
    @Query("SELECT * FROM comments ORDER BY createdAt DESC LIMIT 50")
    fun recentComments(): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: Comment)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM comments WHERE paperId = :paperId")
    suspend fun deleteForPaper(paperId: String)

    @Query("SELECT COUNT(*) FROM comments WHERE paperId = :paperId")
    suspend fun countFor(paperId: String): Int
}

@Database(
    entities = [
        SavedPaper::class,
        Comment::class,
        ConversationEntity::class,
        ChatMessageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedPaperDao(): SavedPaperDao
    abstract fun commentDao(): CommentDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        /**
         * v1 stored a pre-rendered `citation` string and a frozen `timeAgo` label, and had no
         * notion of engagement. v2 replaces both with structured fields, adds the social
         * columns, and introduces the comments table. The papers table is recreated and the
         * old citation text preserved in `citationOverride`.
         *
         * No column carries a SQL DEFAULT: the entities declare Kotlin defaults only, and
         * Room's runtime schema validation compares default values literally. Every column is
         * therefore given an explicit value by the INSERT instead.
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
                        `endorsementCount` INTEGER NOT NULL,
                        `commentCount` INTEGER NOT NULL,
                        `repostCount` INTEGER NOT NULL,
                        `isBookmarked` INTEGER NOT NULL,
                        `imageUri` TEXT NOT NULL,
                        `quotedId` TEXT NOT NULL,
                        `quotedAuthorName` TEXT NOT NULL,
                        `quotedTitle` TEXT NOT NULL,
                        `quotedContent` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `saved_papers_new` (
                        `id`, `authorInitials`, `authorName`, `affiliation`, `content`,
                        `title`, `authors`, `year`, `venue`, `doi`, `url`,
                        `publishedAt`, `citationOverride`, `isEndorsed`,
                        `endorsementCount`, `commentCount`, `repostCount`, `isBookmarked`,
                        `imageUri`, `quotedId`, `quotedAuthorName`, `quotedTitle`,
                        `quotedContent`
                    )
                    SELECT `id`, `authorInitials`, `authorName`, `affiliation`, `content`,
                           '', '', '', '', '', '',
                           $nowMillis, `citation`, `isEndorsed`,
                           0, 0, 0, 0,
                           '', '', '', '', ''
                    FROM `saved_papers`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `saved_papers`")
                db.execSQL("ALTER TABLE `saved_papers_new` RENAME TO `saved_papers`")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `comments` (
                        `id` TEXT NOT NULL,
                        `paperId` TEXT NOT NULL,
                        `authorInitials` TEXT NOT NULL,
                        `authorName` TEXT NOT NULL,
                        `affiliation` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                // Room names an implicit index `index_<table>_<column>`; the migration has to
                // match that exactly or schema validation fails on the next launch.
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_comments_paperId` ON `comments` (`paperId`)"
                )
            }
        }

        fun migration2To3(): Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `conversations` (
                        `id` TEXT NOT NULL,
                        `participantName` TEXT NOT NULL,
                        `participantInitials` TEXT NOT NULL,
                        `participantAffiliation` TEXT NOT NULL,
                        `lastMessage` TEXT NOT NULL,
                        `lastMessageTimestamp` INTEGER NOT NULL,
                        `unreadCount` INTEGER NOT NULL,
                        `isOnline` INTEGER NOT NULL,
                        `attachedPaperId` TEXT NOT NULL,
                        `attachedPaperTitle` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` TEXT NOT NULL,
                        `conversationId` TEXT NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `senderInitials` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isOutgoing` INTEGER NOT NULL,
                        `quotedPaperId` TEXT NOT NULL,
                        `quotedPaperTitle` TEXT NOT NULL,
                        `quotedCitation` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_messages_conversationId` ON `chat_messages` (`conversationId`)"
                )
            }
        }

        fun migration3To4(): Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `saved_papers` ADD COLUMN `pdfUrl` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `saved_papers` ADD COLUMN `pdfLocalPath` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `saved_papers` ADD COLUMN `abstractText` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `saved_papers` ADD COLUMN `openAccess` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

/**
 * Single entry point for reads and writes.
 *
 * Writes that touch more than one row run inside [withTransaction] so denormalised counters
 * cannot drift away from the rows they summarise.
 */
class PaperRepository(private val database: AppDatabase) {

    private val dao = database.savedPaperDao()
    private val commentDao = database.commentDao()

    val allPapers: Flow<List<SavedPaper>> = dao.getAllPapers()
    val bookmarkedPapers: Flow<List<SavedPaper>> = dao.getBookmarkedPapers()
    val vaultPapers: Flow<List<SavedPaper>> = dao.getVaultPapers()
    val recentQuotes: Flow<List<SavedPaper>> = dao.getRecentQuotes()
    val recentComments: Flow<List<Comment>> = commentDao.recentComments()
    val venueCounts: Flow<List<VenueCount>> = dao.getVenueCounts()

    fun paper(id: String): Flow<SavedPaper?> = dao.getPaper(id)

    fun papersInVenue(venue: String): Flow<List<SavedPaper>> = dao.getPapersInVenue(venue)

    fun comments(paperId: String): Flow<List<Comment>> = commentDao.commentsFor(paperId)

    suspend fun savePaper(paper: SavedPaper) = dao.insertPaper(paper)

    suspend fun updatePdfLocalPath(id: String, localPath: String) = dao.updatePdfLocalPath(id, localPath)

    /** Removes a post and everything hanging off it. */
    suspend fun deletePaper(id: String) {
        database.withTransaction {
            commentDao.deleteForPaper(id)
            dao.deletePaper(id)
        }
    }

    /** A one-shot read of a post's comments, for capturing them before a delete cascades. */
    suspend fun commentsOnce(paperId: String): List<Comment> = commentDao.commentsForOnce(paperId)

    /** Puts a withdrawn post and its thread back exactly as they were. */
    suspend fun restorePaper(paper: SavedPaper, comments: List<Comment>) {
        database.withTransaction {
            dao.insertPaper(paper)
            comments.forEach { commentDao.insert(it) }
        }
    }

    suspend fun toggleEndorsement(id: String, currentStatus: Boolean) {
        dao.updateEndorsement(id, !currentStatus, if (currentStatus) -1 else 1)
    }

    suspend fun toggleBookmark(id: String, currentStatus: Boolean) {
        dao.updateBookmark(id, !currentStatus)
    }

    suspend fun addComment(comment: Comment) {
        database.withTransaction {
            commentDao.insert(comment)
            dao.setCommentCount(comment.paperId, commentDao.countFor(comment.paperId))
        }
    }

    suspend fun deleteComment(comment: Comment) {
        database.withTransaction {
            commentDao.delete(comment.id)
            dao.setCommentCount(comment.paperId, commentDao.countFor(comment.paperId))
        }
    }

    /** Publishes [quote] and credits the post it quotes. */
    suspend fun publishQuote(quote: SavedPaper) {
        database.withTransaction {
            dao.insertPaper(quote)
            if (quote.quotedId.isNotBlank()) dao.incrementRepostCount(quote.quotedId)
        }
    }

    suspend fun findPaper(id: String): SavedPaper? = dao.findPaper(id)
}
