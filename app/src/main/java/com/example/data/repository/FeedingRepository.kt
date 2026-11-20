package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.FeedItem
import com.example.data.model.FeedingRecord
import com.example.data.model.FeedingSchedule
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

class FeedingRepository(
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

    fun observeFeedItems(userId: String): Flow<List<FeedItem>> {
        val colRef = db.collection("users").document(userId).collection("feedItems")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FeedItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.name.lowercase() }
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

    suspend fun addFeedItem(item: FeedItem): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("feedItems").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to item.name.trim().take(80),
            "category" to item.category,
            "quantity" to item.quantity.coerceIn(0.0, 1000000.0),
            "unit" to item.unit,
            "costPerUnit" to item.costPerUnit.coerceIn(0.0, 1000000.0),
            "minStockLevel" to item.minStockLevel.coerceIn(0.0, 1000000.0),
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

    suspend fun updateFeedItem(item: FeedItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(item.id.isNotBlank()) { "FeedItem ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("feedItems").document(item.id)
        val updates = mapOf(
            "name" to item.name.trim().take(80),
            "category" to item.category,
            "quantity" to item.quantity.coerceIn(0.0, 1000000.0),
            "unit" to item.unit,
            "costPerUnit" to item.costPerUnit.coerceIn(0.0, 1000000.0),
            "minStockLevel" to item.minStockLevel.coerceIn(0.0, 1000000.0),
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

    suspend fun restockFeedItem(
        feedId: String,
        newTotalQuantity: Double
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("feedItems").document(feedId)
        val updates = mapOf(
            "quantity" to newTotalQuantity.coerceIn(0.0, 1000000.0),
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

    suspend fun deleteFeedItem(feedId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("feedItems").document(feedId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    fun observeFeedingSchedules(userId: String): Flow<List<FeedingSchedule>> {
        val colRef = db.collection("users").document(userId).collection("feedingSchedules")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FeedingSchedule::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.feedingTime }
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

    suspend fun addFeedingSchedule(schedule: FeedingSchedule): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("feedingSchedules").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to schedule.title.trim().take(100),
            "targetGroup" to schedule.targetGroup.trim().take(80),
            "feedId" to schedule.feedId,
            "feedName" to schedule.feedName.trim().take(80),
            "quantity" to schedule.quantity.coerceIn(0.01, 100000.0),
            "unit" to schedule.unit,
            "feedingTime" to schedule.feedingTime,
            "isActive" to schedule.isActive,
            "lastCompletedDate" to schedule.lastCompletedDate,
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

    suspend fun updateFeedingSchedule(schedule: FeedingSchedule): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(schedule.id.isNotBlank()) { "FeedingSchedule ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("feedingSchedules").document(schedule.id)
        val updates = mapOf(
            "title" to schedule.title.trim().take(100),
            "targetGroup" to schedule.targetGroup.trim().take(80),
            "feedId" to schedule.feedId,
            "feedName" to schedule.feedName.trim().take(80),
            "quantity" to schedule.quantity.coerceIn(0.01, 100000.0),
            "unit" to schedule.unit,
            "feedingTime" to schedule.feedingTime,
            "isActive" to schedule.isActive,
            "lastCompletedDate" to schedule.lastCompletedDate,
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

    suspend fun deleteFeedingSchedule(scheduleId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("feedingSchedules").document(scheduleId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    fun observeFeedingRecords(userId: String): Flow<List<FeedingRecord>> {
        val colRef = db.collection("users").document(userId).collection("feedingRecords")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FeedingRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<FeedingRecord> { it.date }
                        .thenByDescending { it.time }
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

    /**
     * Atomically records a completed feeding activity, updates the schedule's lastCompletedDate,
     * and deducts consumed quantity from the associated feed item's stock.
     */
    suspend fun completeFeedingActivity(
        schedule: FeedingSchedule?,
        feedItem: FeedItem,
        quantityConsumed: Double,
        date: String,
        time: String,
        notes: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val recordRef = userDoc.collection("feedingRecords").document()
        val feedRef = userDoc.collection("feedItems").document(feedItem.id)
        val safeConsumed = quantityConsumed.coerceIn(0.01, 100000.0)
        val updatedStock = (feedItem.quantity - safeConsumed).coerceAtLeast(0.0)

        val recordPayload = mapOf(
            "userId" to uid,
            "scheduleId" to (schedule?.id ?: ""),
            "scheduleTitle" to (schedule?.title ?: "Manual Feeding: ${feedItem.name}").take(100),
            "feedId" to feedItem.id,
            "feedName" to feedItem.name.trim().take(80),
            "quantityConsumed" to safeConsumed,
            "unit" to feedItem.unit,
            "date" to date,
            "time" to time,
            "status" to "Completed",
            "notes" to notes.trim().take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(recordRef, recordPayload)
        batch.update(
            feedRef,
            mapOf(
                "quantity" to updatedStock,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (schedule != null && schedule.id.isNotBlank()) {
            val scheduleRef = userDoc.collection("feedingSchedules").document(schedule.id)
            batch.update(
                scheduleRef,
                mapOf(
                    "lastCompletedDate" to date,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        try {
            batch.commit().await()
            recordRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, recordRef.path, auth)
            throw e
        }
    }

    suspend fun markFeedingMissed(
        schedule: FeedingSchedule,
        date: String,
        time: String,
        notes: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val recordRef = userDoc.collection("feedingRecords").document()
        val scheduleRef = userDoc.collection("feedingSchedules").document(schedule.id)
        val recordPayload = mapOf(
            "userId" to uid,
            "scheduleId" to schedule.id,
            "scheduleTitle" to schedule.title.trim().take(100),
            "feedId" to schedule.feedId,
            "feedName" to schedule.feedName.trim().take(80),
            "quantityConsumed" to schedule.quantity.coerceIn(0.01, 100000.0),
            "unit" to schedule.unit,
            "date" to date,
            "time" to time,
            "status" to "Missed",
            "notes" to notes.trim().take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val batch = db.batch()
        batch.set(recordRef, recordPayload)
        batch.update(
            scheduleRef,
            mapOf(
                "lastCompletedDate" to date,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        try {
            batch.commit().await()
            recordRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, recordRef.path, auth)
            throw e
        }
    }
}
