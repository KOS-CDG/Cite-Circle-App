package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    // Lazy and failure-tolerant on purpose. These were eager property initializers, and
    // HomeViewModel holds a FirestoreRepository as a `private val`, so both Firebase singletons
    // were being spun up at app start against a placeholder google-services.json
    // (project_id "dummy-project"). Nothing here can succeed until Firebase is really
    // provisioned -- auth.currentUser is always null, so the sync below returns immediately --
    // so there is no reason to touch the SDK unless a caller actually asks for a sync.
    private val db by lazy { runCatching { FirebaseFirestore.getInstance() }.getOrNull() }
    private val auth by lazy { runCatching { FirebaseAuth.getInstance() }.getOrNull() }

    suspend fun syncPapersToCloud(papers: List<SavedPaper>) {
        val firestore = db ?: return
        val user = auth?.currentUser ?: return
        val batch = firestore.batch()
        val userRef = firestore.collection("users").document(user.uid)

        try {
            papers.forEach { paper ->
                val paperRef = userRef.collection("saved_papers").document(paper.id)
                batch.set(paperRef, paper)
            }
            batch.commit().await()
            Log.d("FirestoreRepository", "Successfully synced papers to cloud")
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Failed to sync to cloud", e)
        }
    }
}
