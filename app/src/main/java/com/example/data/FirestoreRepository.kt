package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Scalable Firestore Repository engineered for 10k+ researchers.
 *
 * Implements:
 * 1. Multi-tenant public feed (`/posts`) with atomic `FieldValue.increment()` counters.
 * 2. Master open-access research archive (`/papers`) for deduplicated paper records.
 * 3. User bookmark & library synchronization (`/users/{uid}/saved_papers`).
 * 4. Fault-tolerant offline/demo graceful fallbacks.
 */
class FirestoreRepository {

    private val TAG = "FirestoreRepository"
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    /**
     * Synchronizes local saved library and published papers to both user cache
     * and the shared public academic archive.
     */
    suspend fun syncPapersToCloud(papers: List<SavedPaper>) {
        val user = auth.currentUser ?: run {
            Log.d(TAG, "No authenticated Firebase user; skipping remote cloud sync")
            return
        }

        try {
            val batch = db.batch()
            val userRef = db.collection("users").document(user.uid)

            papers.forEach { paper ->
                // User private saved papers subcollection
                val userPaperRef = userRef.collection("saved_papers").document(paper.id)
                batch.set(userPaperRef, paper)

                // Master research paper archive
                if (paper.title.isNotBlank()) {
                    val masterPaperRef = db.collection("papers").document(paper.id)
                    val paperData = hashMapOf(
                        "id" to paper.id,
                        "title" to paper.title,
                        "authors" to paper.authors,
                        "year" to paper.year,
                        "venue" to paper.venue,
                        "doi" to paper.doi,
                        "url" to paper.url,
                        "pdfUrl" to paper.pdfUrl,
                        "abstractText" to paper.abstractText,
                        "openAccess" to paper.openAccess,
                        "submittedBy" to user.uid,
                        "submittedAt" to (if (paper.publishedAt > 0) paper.publishedAt else System.currentTimeMillis())
                    )
                    batch.set(masterPaperRef, paperData)
                }

                // Master public feed post
                val postRef = db.collection("posts").document(paper.id)
                val postData = hashMapOf(
                    "id" to paper.id,
                    "authorId" to user.uid,
                    "authorName" to paper.authorName,
                    "authorInitials" to paper.authorInitials,
                    "affiliation" to paper.affiliation,
                    "content" to paper.content,
                    "title" to paper.title,
                    "authors" to paper.authors,
                    "year" to paper.year,
                    "venue" to paper.venue,
                    "doi" to paper.doi,
                    "url" to paper.url,
                    "pdfUrl" to paper.pdfUrl,
                    "abstractText" to paper.abstractText,
                    "openAccess" to paper.openAccess,
                    "publishedAt" to paper.publishedAt,
                    "citationOverride" to paper.citationOverride,
                    "endorsementCount" to paper.endorsementCount,
                    "commentCount" to paper.commentCount,
                    "repostCount" to paper.repostCount,
                    "imageUri" to paper.imageUri,
                    "quotedId" to paper.quotedId,
                    "quotedAuthorName" to paper.quotedAuthorName,
                    "quotedTitle" to paper.quotedTitle,
                    "quotedContent" to paper.quotedContent
                )
                batch.set(postRef, postData)
            }

            batch.commit().await()
            Log.d(TAG, "Successfully synced ${papers.size} papers to cloud")
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync failed or offline: ${e.message}")
        }
    }

    /**
     * Atomically increments endorsement counter for 10k users without read-modify-write collisions.
     */
    suspend fun incrementEndorsement(postId: String, delta: Long) {
        try {
            val postRef = db.collection("posts").document(postId)
            postRef.update("endorsementCount", FieldValue.increment(delta)).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed atomic endorsement increment for $postId", e)
        }
    }

    /**
     * Atomically increments repost counter.
     */
    suspend fun incrementRepost(postId: String) {
        try {
            val postRef = db.collection("posts").document(postId)
            postRef.update("repostCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed atomic repost increment for $postId", e)
        }
    }

    /**
     * Atomically increments comment counter on post.
     */
    suspend fun incrementCommentCount(postId: String, delta: Long) {
        try {
            val postRef = db.collection("posts").document(postId)
            postRef.update("commentCount", FieldValue.increment(delta)).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed atomic comment increment for $postId", e)
        }
    }

    /**
     * Real-time stream of the public academic feed from Firestore.
     */
    fun publicFeedFlow(limit: Long = 50): Flow<List<SavedPaper>> = callbackFlow {
        val query = db.collection("posts")
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .limit(limit)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Public feed listener error: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val papers = snapshot.documents.mapNotNull { doc ->
                    try {
                        SavedPaper(
                            id = doc.getString("id") ?: doc.id,
                            authorInitials = doc.getString("authorInitials").orEmpty(),
                            authorName = doc.getString("authorName").orEmpty(),
                            affiliation = doc.getString("affiliation").orEmpty(),
                            content = doc.getString("content").orEmpty(),
                            title = doc.getString("title").orEmpty(),
                            authors = doc.getString("authors").orEmpty(),
                            year = doc.getString("year").orEmpty(),
                            venue = doc.getString("venue").orEmpty(),
                            doi = doc.getString("doi").orEmpty(),
                            url = doc.getString("url").orEmpty(),
                            publishedAt = doc.getLong("publishedAt") ?: 0L,
                            citationOverride = doc.getString("citationOverride").orEmpty(),
                            isEndorsed = false,
                            endorsementCount = doc.getLong("endorsementCount")?.toInt() ?: 0,
                            commentCount = doc.getLong("commentCount")?.toInt() ?: 0,
                            repostCount = doc.getLong("repostCount")?.toInt() ?: 0,
                            isBookmarked = false,
                            imageUri = doc.getString("imageUri").orEmpty(),
                            quotedId = doc.getString("quotedId").orEmpty(),
                            quotedAuthorName = doc.getString("quotedAuthorName").orEmpty(),
                            quotedTitle = doc.getString("quotedTitle").orEmpty(),
                            quotedContent = doc.getString("quotedContent").orEmpty(),
                            pdfUrl = doc.getString("pdfUrl").orEmpty(),
                            pdfLocalPath = "",
                            abstractText = doc.getString("abstractText").orEmpty(),
                            openAccess = doc.getBoolean("openAccess") ?: false
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error mapping document ${doc.id}", e)
                        null
                    }
                }
                trySend(papers)
            }
        }

        awaitClose { registration.remove() }
    }
}
