package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val farmName: String = "My Dairy Farm",
    val currencySymbol: String = "$",
    val defaultMilkPrice: Double = 1.50,
    val feedingRemindersEnabled: Boolean = true,
    val lowStockAlertsEnabled: Boolean = true,
    val productionDropAlertsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Cattle(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tagNumber: String = "",
    val name: String = "",
    val species: String = "Cow",
    val breed: String = "",
    val registrationDate: String = "",
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MilkRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val cattleId: String = "",
    val cattleTag: String = "",
    val cattleName: String = "",
    val date: String = "",
    val morningLitres: Double = 0.0,
    val eveningLitres: Double = 0.0,
    val totalLitres: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FeedItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Concentrate",
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val costPerUnit: Double = 0.0,
    val minStockLevel: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isLowStock: Boolean
        get() = quantity <= minStockLevel
}

@IgnoreExtraProperties
data class FeedingSchedule(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetGroup: String = "",
    val feedId: String = "",
    val feedName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val feedingTime: String = "06:00",
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    val lastCompletedDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FeedingRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val scheduleId: String = "",
    val scheduleTitle: String = "",
    val feedId: String = "",
    val feedName: String = "",
    val quantityConsumed: Double = 0.0,
    val unit: String = "kg",
    val date: String = "",
    val time: String = "",
    val status: String = "Completed",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FarmTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val date: String = "",
    val type: String = "Income",
    val category: String = "Milk Sales",
    val description: String = "",
    val amount: Double = 0.0,
    val quantityLitres: Double = 0.0,
    val pricePerLitre: Double = 0.0,
    val paymentMethod: String = "Cash",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object FarmConstants {
    val SPECIES_OPTIONS = listOf("Cow", "Buffalo")
    val BREED_SUGGESTIONS = listOf("Holstein", "Jersey", "Gir", "Sahiwal", "Murrah", "Crossbred")
    val FEED_CATEGORIES = listOf(
        "Green Fodder",
        "Dry Fodder",
        "Concentrate",
        "Silage",
        "Mineral Mix",
        "Supplements"
    )
    val FEED_UNITS = listOf("kg", "grams", "bales", "litres")
    val INCOME_CATEGORIES = listOf("Milk Sales", "Cattle Sale", "Manure / Compost", "Dairy Products", "Other Income")
    val EXPENSE_CATEGORIES = listOf(
        "Cattle Feed",
        "Medicines & Vet",
        "Labour",
        "Transportation",
        "Equipment & Maintenance",
        "Electricity & Water",
        "Other Miscellaneous"
    )
    val PAYMENT_METHODS = listOf("Cash", "UPI", "Bank Transfer", "Card")
    val CURRENCY_OPTIONS = listOf("$", "₹", "€", "£")
}
