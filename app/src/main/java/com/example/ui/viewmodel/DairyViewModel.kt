package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.Cattle
import com.example.data.model.FarmTransaction
import com.example.data.model.FeedItem
import com.example.data.model.FeedingRecord
import com.example.data.model.FeedingSchedule
import com.example.data.model.MilkRecord
import com.example.data.model.UserProfile
import com.example.data.remote.unwrapFirestoreError
import com.example.data.repository.FarmProfileRepository
import com.example.data.repository.FeedingRepository
import com.example.data.repository.FinanceRepository
import com.example.data.repository.MilkRepository
import com.example.notifications.FeedingReminderManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DairyDashboardData(
    val profile: UserProfile? = null,
    val cattle: List<Cattle> = emptyList(),
    val milkRecords: List<MilkRecord> = emptyList(),
    val feedItems: List<FeedItem> = emptyList(),
    val feedingSchedules: List<FeedingSchedule> = emptyList(),
    val feedingRecords: List<FeedingRecord> = emptyList(),
    val transactions: List<FarmTransaction> = emptyList()
)

class DairyViewModel(
    private val appContext: Context,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val profileRepository: FarmProfileRepository,
    private val milkRepository: MilkRepository,
    private val feedingRepository: FeedingRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val profileFlow = profileRepository.observeProfile(currentUserId)
    private val cattleFlow = milkRepository.observeCattle(currentUserId)
    private val milkRecordsFlow = milkRepository.observeMilkRecords(currentUserId)
    private val feedItemsFlow = feedingRepository.observeFeedItems(currentUserId)
    private val schedulesFlow = feedingRepository.observeFeedingSchedules(currentUserId)
    private val feedingRecordsFlow = feedingRepository.observeFeedingRecords(currentUserId)
    private val transactionsFlow = financeRepository.observeTransactions(currentUserId)

    val uiState: StateFlow<UiState<DairyDashboardData>> = combine(
        profileFlow,
        cattleFlow,
        milkRecordsFlow,
        feedItemsFlow,
        schedulesFlow
    ) { profile, cattle, milkRecords, feedItems, schedules ->
        DairyDashboardData(
            profile = profile,
            cattle = cattle,
            milkRecords = milkRecords,
            feedItems = feedItems,
            feedingSchedules = schedules
        )
    }.combine(feedingRecordsFlow) { partial, feedingRecords ->
        partial.copy(feedingRecords = feedingRecords)
    }.combine(transactionsFlow) { partial, transactions ->
        partial.copy(transactions = transactions)
    }
        .map<DairyDashboardData, UiState<DairyDashboardData>> { data ->
            FeedingReminderManager.syncAllReminders(
                appContext,
                data.feedingSchedules,
                data.profile?.feedingRemindersEnabled != false
            )
            UiState.Success(data)
        }
        .catch { error ->
            if (error is CancellationException && unwrapFirestoreError(error) === error) {
                throw error
            }
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing farm data", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load farm data"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            profileRepository.ensureProfileExists(userDisplayName, userEmail)
                .onFailure { Log.w(TAG, "Profile initialization notice: ${it.message}") }
        }
    }

    private fun runWrite(successMessage: String, block: suspend () -> Result<Any?>) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val res = block()
                res.onSuccess {
                    _events.emit(successMessage)
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Operation failed")
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    // --- Profile & Settings ---
    fun updateProfile(profile: UserProfile) = runWrite("Farm settings saved") {
        profileRepository.updateProfile(profile)
    }

    // --- Cattle CRUD ---
    fun saveCattle(cattle: Cattle) {
        if (cattle.id.isBlank()) {
            runWrite("Cattle ${cattle.tagNumber} registered") {
                milkRepository.addCattle(cattle)
            }
        } else {
            runWrite("Cattle ${cattle.tagNumber} updated") {
                milkRepository.updateCattle(cattle)
            }
        }
    }

    fun deleteCattle(cattleId: String) = runWrite("Cattle record removed") {
        milkRepository.deleteCattle(cattleId)
    }

    // --- Milk Records CRUD ---
    fun saveMilkRecord(record: MilkRecord) {
        if (record.id.isBlank()) {
            runWrite("Milk yield recorded") {
                milkRepository.addMilkRecord(record)
            }
        } else {
            runWrite("Milk record updated") {
                milkRepository.updateMilkRecord(record)
            }
        }
    }

    fun deleteMilkRecord(recordId: String) = runWrite("Milk record deleted") {
        milkRepository.deleteMilkRecord(recordId)
    }

    // --- Feed Inventory CRUD ---
    fun saveFeedItem(item: FeedItem) {
        if (item.id.isBlank()) {
            runWrite("${item.name} added to feed stock") {
                feedingRepository.addFeedItem(item)
            }
        } else {
            runWrite("${item.name} updated") {
                feedingRepository.updateFeedItem(item)
            }
        }
    }

    fun restockFeedItem(
        feedItem: FeedItem,
        addedQty: Double,
        recordExpense: Boolean,
        totalPurchaseCost: Double,
        date: String,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val newQty = (feedItem.quantity + addedQty).coerceAtLeast(0.0)
                val res = feedingRepository.restockFeedItem(feedItem.id, newQty)
                if (res.isSuccess && recordExpense && totalPurchaseCost > 0.0) {
                    financeRepository.addTransaction(
                        FarmTransaction(
                            date = date,
                            type = "Expense",
                            category = "Cattle Feed",
                            description = "Restocked $addedQty ${feedItem.unit} of ${feedItem.name}",
                            amount = totalPurchaseCost,
                            paymentMethod = paymentMethod
                        )
                    )
                }
                res.onSuccess {
                    _events.emit("Restocked ${feedItem.name}")
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Restock failed")
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteFeedItem(feedId: String) = runWrite("Feed item removed") {
        feedingRepository.deleteFeedItem(feedId)
    }

    // --- Feeding Schedules & Activities ---
    fun saveFeedingSchedule(schedule: FeedingSchedule) {
        if (schedule.id.isBlank()) {
            runWrite("Feeding schedule created") {
                feedingRepository.addFeedingSchedule(schedule)
            }
        } else {
            runWrite("Feeding schedule updated") {
                feedingRepository.updateFeedingSchedule(schedule)
            }
        }
    }

    fun deleteFeedingSchedule(scheduleId: String) {
        FeedingReminderManager.cancelReminder(appContext, scheduleId)
        runWrite("Schedule removed") {
            feedingRepository.deleteFeedingSchedule(scheduleId)
        }
    }

    fun completeFeedingActivity(
        schedule: FeedingSchedule?,
        feedItem: FeedItem,
        quantityConsumed: Double,
        date: String,
        time: String,
        notes: String
    ) = runWrite("Feeding logged & stock updated") {
        feedingRepository.completeFeedingActivity(
            schedule = schedule,
            feedItem = feedItem,
            quantityConsumed = quantityConsumed,
            date = date,
            time = time,
            notes = notes
        )
    }

    fun markFeedingMissed(schedule: FeedingSchedule, date: String, time: String) =
        runWrite("Marked as missed") {
            feedingRepository.markFeedingMissed(schedule, date, time)
        }

    // --- Financial Transactions CRUD ---
    fun saveTransaction(tx: FarmTransaction) {
        if (tx.id.isBlank()) {
            runWrite("${tx.type} recorded") {
                financeRepository.addTransaction(tx)
            }
        } else {
            runWrite("Transaction updated") {
                financeRepository.updateTransaction(tx)
            }
        }
    }

    fun deleteTransaction(txId: String) = runWrite("Transaction deleted") {
        financeRepository.deleteTransaction(txId)
    }

    companion object {
        private const val TAG = "DairyVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun provideFactory(
            context: Context,
            userId: String,
            displayName: String,
            email: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = context.applicationContext
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val auth = FirebaseAuth.getInstance()
                DairyViewModel(
                    appContext = app,
                    currentUserId = userId,
                    userDisplayName = displayName,
                    userEmail = email,
                    profileRepository = FarmProfileRepository(db, auth),
                    milkRepository = MilkRepository(db, auth),
                    feedingRepository = FeedingRepository(db, auth),
                    financeRepository = FinanceRepository(db, auth)
                )
            }
        }
    }
}
