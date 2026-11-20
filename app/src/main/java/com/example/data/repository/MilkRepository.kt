package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Cattle
import com.example.data.model.MilkRecord
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MilkRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun observeCattle(userId: String): Flow<List<Cattle>> {
        val colRef = db.collection("users").document(userId).collection("cattle")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Cattle::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path, auth)
                }
                throw error
            }
    }

    suspend fun addCattle(cattle: Cattle): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("cattle").document()
        val payload = mapOf(
            "userId" to uid,
            "tagNumber" to cattle.tagNumber.trim().take(40),
            "name" to cattle.name.trim().take(80),
            "species" to cattle.species,
            "breed" to cattle.breed.trim().take(60),
            "registrationDate" to cattle.registrationDate,
            "isActive" to cattle.isActive,
            "notes" to cattle.notes.trim().take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateCattle(cattle: Cattle): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(cattle.id.isNotBlank()) { "Cattle ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("cattle").document(cattle.id)
        val updates = mapOf(
            "tagNumber" to cattle.tagNumber.trim().take(40),
            "name" to cattle.name.trim().take(80),
            "species" to cattle.species,
            "breed" to cattle.breed.trim().take(60),
            "registrationDate" to cattle.registrationDate,
            "isActive" to cattle.isActive,
            "notes" to cattle.notes.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteCattle(cattleId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("cattle").document(cattleId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    fun observeMilkRecords(userId: String): Flow<List<MilkRecord>> {
        val colRef = db.collection("users").document(userId).collection("milkRecords")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MilkRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<MilkRecord> { it.date }
                        .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path, auth)
                }
                throw error
            }
    }

    suspend fun addMilkRecord(record: MilkRecord): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("milkRecords").document()
        val morning = record.morningLitres.coerceIn(0.0, 500.0)
        val evening = record.eveningLitres.coerceIn(0.0, 500.0)
        val total = (morning + evening).coerceIn(0.0, 1000.0)
        val payload = mapOf(
            "userId" to uid,
            "cattleId" to record.cattleId,
            "cattleTag" to record.cattleTag.trim().take(40),
            "cattleName" to record.cattleName.trim().take(80),
            "date" to record.date,
            "morningLitres" to morning,
            "eveningLitres" to evening,
            "totalLitres" to total,
            "notes" to record.notes.trim().take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateMilkRecord(record: MilkRecord): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(record.id.isNotBlank()) { "MilkRecord ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("milkRecords").document(record.id)
        val morning = record.morningLitres.coerceIn(0.0, 500.0)
        val evening = record.eveningLitres.coerceIn(0.0, 500.0)
        val total = (morning + evening).coerceIn(0.0, 1000.0)
        val updates = mapOf(
            "cattleId" to record.cattleId,
            "cattleTag" to record.cattleTag.trim().take(40),
            "cattleName" to record.cattleName.trim().take(80),
            "date" to record.date,
            "morningLitres" to morning,
            "eveningLitres" to evening,
            "totalLitres" to total,
            "notes" to record.notes.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteMilkRecord(recordId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("milkRecords").document(recordId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }
}
