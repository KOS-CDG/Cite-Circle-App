package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * A user-defined collection/folder for organizing research papers into
 * custom projects, research topics, or reading lists (similar to Zotero/Mendeley collections).
 */
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val colorHex: String = "#1A73E8",
    val iconName: String = "folder",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Join table mapping [SavedPaper] rows to [CollectionEntity] folders.
 * Supports many-to-many relationship: a paper can belong to multiple collections,
 * and a collection contains multiple papers.
 */
@Entity(
    tableName = "paper_collection_entries",
    primaryKeys = ["paperId", "collectionId"],
    indices = [
        Index("collectionId"),
        Index("paperId")
    ]
)
data class PaperCollectionEntry(
    val paperId: String,
    val collectionId: String,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Projection representing a collection along with the live count of papers assigned to it.
 */
data class CollectionWithCount(
    val id: String,
    val name: String,
    val description: String = "",
    val colorHex: String = "#1A73E8",
    val iconName: String = "folder",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val paperCount: Int = 0
)

@Dao
interface CollectionDao {
    @Query("""
        SELECT c.id, c.name, c.description, c.colorHex, c.iconName, c.createdAt, c.updatedAt,
               COUNT(e.paperId) AS paperCount
        FROM collections c
        LEFT JOIN paper_collection_entries e ON c.id = e.collectionId
        GROUP BY c.id
        ORDER BY c.updatedAt DESC
    """)
    fun getCollectionsWithCount(): Flow<List<CollectionWithCount>>

    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollectionById(id: String): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: String)

    @Query("DELETE FROM paper_collection_entries WHERE collectionId = :collectionId")
    suspend fun deleteEntriesForCollection(collectionId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPaperToCollection(entry: PaperCollectionEntry)

    @Query("DELETE FROM paper_collection_entries WHERE paperId = :paperId AND collectionId = :collectionId")
    suspend fun removePaperFromCollection(paperId: String, collectionId: String)

    @Query("DELETE FROM paper_collection_entries WHERE paperId = :paperId")
    suspend fun deleteEntriesForPaper(paperId: String)

    @Query("""
        SELECT p.* FROM saved_papers p
        INNER JOIN paper_collection_entries e ON p.id = e.paperId
        WHERE e.collectionId = :collectionId
        ORDER BY e.addedAt DESC
    """)
    fun getPapersInCollection(collectionId: String): Flow<List<SavedPaper>>

    @Query("SELECT collectionId FROM paper_collection_entries WHERE paperId = :paperId")
    fun getCollectionIdsForPaper(paperId: String): Flow<List<String>>

    @Query("SELECT collectionId FROM paper_collection_entries WHERE paperId = :paperId")
    suspend fun getCollectionIdsForPaperOnce(paperId: String): List<String>

    @Query("""
        SELECT c.* FROM collections c
        INNER JOIN paper_collection_entries e ON c.id = e.collectionId
        WHERE e.paperId = :paperId
        ORDER BY c.name ASC
    """)
    fun getCollectionsForPaper(paperId: String): Flow<List<CollectionEntity>>

    @Query("SELECT COUNT(*) FROM paper_collection_entries WHERE collectionId = :collectionId")
    suspend fun countPapersInCollection(collectionId: String): Int

    @Query("SELECT * FROM collections")
    suspend fun getAllCollectionsOnce(): List<CollectionEntity>

    @Query("SELECT * FROM paper_collection_entries")
    suspend fun getAllEntriesOnce(): List<PaperCollectionEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<CollectionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntries(entries: List<PaperCollectionEntry>)
}
