package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Status of the academic relationship between the current researcher and another scholar.
 */
enum class ConnectionStatus {
    NOT_CONNECTED,
    PENDING_SENT,
    PENDING_RECEIVED,
    CONNECTED
}

/**
 * The academic context explaining how two scholars know each other or wish to connect.
 */
object ConnectionReasons {
    const val SAME_SCHOOL = "Same School or University"
    const val RESEARCH_COLLAB = "Research Collaboration"
    const val READ_AND_CITED = "Read & Cited Their Work"
    const val ACADEMIC_EVENT = "Academic Conference or Event"
    const val SHARED_INTEREST = "Shared Scientific Interest"

    val ALL = listOf(
        SAME_SCHOOL to "We attended, graduated from, or teach at the same institution",
        RESEARCH_COLLAB to "We worked together on a study, grant, or paper",
        READ_AND_CITED to "I read, cite, or follow their academic publications",
        ACADEMIC_EVENT to "We met or presented at a symposium or seminar",
        SHARED_INTEREST to "We share research focus in this scientific domain"
    )
}

/**
 * Room entity representing an academic peer or connection in Cite Circle.
 */
@Entity(tableName = "scholar_connections")
data class ScholarConnection(
    @PrimaryKey val id: String,
    val name: String,
    val initials: String,
    val avatarUri: String = "",
    val affiliation: String,
    val researchField: String,
    val degree: String = "",
    val status: String = ConnectionStatus.NOT_CONNECTED.name,
    val connectionReason: String = "",
    val personalNote: String = "",
    val mutualCount: Int = 0,
    val connectedAt: Long = 0L,
    val isSuggested: Boolean = true
)

@Dao
interface ScholarConnectionDao {
    @Query("SELECT * FROM scholar_connections ORDER BY connectedAt DESC, name ASC")
    fun getAllConnections(): Flow<List<ScholarConnection>>

    @Query("SELECT * FROM scholar_connections WHERE status = 'CONNECTED' ORDER BY connectedAt DESC")
    fun getConnectedScholars(): Flow<List<ScholarConnection>>

    @Query("SELECT * FROM scholar_connections WHERE status = 'PENDING_RECEIVED' ORDER BY connectedAt DESC")
    fun getPendingInvitations(): Flow<List<ScholarConnection>>

    @Query("SELECT * FROM scholar_connections WHERE status = 'PENDING_SENT' ORDER BY connectedAt DESC")
    fun getSentRequests(): Flow<List<ScholarConnection>>

    @Query("SELECT * FROM scholar_connections WHERE status IN ('NOT_CONNECTED', 'PENDING_SENT') ORDER BY isSuggested DESC, mutualCount DESC")
    fun getSuggestedScholars(): Flow<List<ScholarConnection>>

    @Query("SELECT COUNT(*) FROM scholar_connections WHERE status = 'CONNECTED'")
    fun countConnected(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scholar_connections WHERE status = 'PENDING_RECEIVED'")
    fun countPendingInvitations(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: ScholarConnection)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(connections: List<ScholarConnection>)

    @Query("UPDATE scholar_connections SET status = :status, connectionReason = :reason, personalNote = :note, connectedAt = :timestamp WHERE id = :id")
    suspend fun updateConnectionStatus(id: String, status: String, reason: String, note: String, timestamp: Long)

    @Query("DELETE FROM scholar_connections WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM scholar_connections")
    suspend fun countAll(): Int
}
