package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.FarmTransaction
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

class FinanceRepository(
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

    fun observeTransactions(userId: String): Flow<List<FarmTransaction>> {
        val colRef = db.collection("users").document(userId).collection("transactions")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FarmTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<FarmTransaction> { it.date }
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

    suspend fun addTransaction(tx: FarmTransaction): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document()
        val payload = mapOf(
            "userId" to uid,
            "date" to tx.date,
            "type" to tx.type,
            "category" to tx.category.trim().take(60),
            "description" to tx.description.trim().take(300),
            "amount" to tx.amount.coerceIn(0.01, 100000000.0),
            "quantityLitres" to tx.quantityLitres.coerceIn(0.0, 1000000.0),
            "pricePerLitre" to tx.pricePerLitre.coerceIn(0.0, 100000.0),
            "paymentMethod" to tx.paymentMethod,
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

    suspend fun updateTransaction(tx: FarmTransaction): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(tx.id.isNotBlank()) { "Transaction ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("transactions").document(tx.id)
        val updates = mapOf(
            "date" to tx.date,
            "type" to tx.type,
            "category" to tx.category.trim().take(60),
            "description" to tx.description.trim().take(300),
            "amount" to tx.amount.coerceIn(0.01, 100000000.0),
            "quantityLitres" to tx.quantityLitres.coerceIn(0.0, 1000000.0),
            "pricePerLitre" to tx.pricePerLitre.coerceIn(0.0, 100000.0),
            "paymentMethod" to tx.paymentMethod,
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

    suspend fun deleteTransaction(txId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(txId)
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
