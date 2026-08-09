package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun syncPapersToCloud(papers: List<SavedPaper>) {
        val user = auth.currentUser ?: return
        val batch = db.batch()
        val userRef = db.collection("users").document(user.uid)
        
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
