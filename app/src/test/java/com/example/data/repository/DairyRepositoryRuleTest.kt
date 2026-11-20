package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Cattle
import com.example.data.model.FarmTransaction
import com.example.data.model.FeedItem
import com.example.data.model.FeedingSchedule
import com.example.data.model.MilkRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DairyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun cattleAndMilkCrud_authenticatedOwner_succeedsAndPreservesIsActive() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val profileRepo = FarmProfileRepository(firestore, auth)
        val milkRepo = MilkRepository(firestore, auth)

        val profileRes = withTimeout(TIMEOUT_MS) {
            profileRepo.ensureProfileExists("Alice", ALICE_EMAIL)
        }
        assertTrue(profileRes.isSuccess)

        val cattleId = withTimeout(TIMEOUT_MS) {
            milkRepo.addCattle(
                Cattle(
                    tagNumber = "COW-201",
                    name = "Buttercup",
                    species = "Cow",
                    breed = "Jersey",
                    registrationDate = "2026-11-13",
                    isActive = false,
                    notes = "Dry period"
                )
            ).getOrThrow()
        }

        val cattleList = withTimeout(FLOW_TIMEOUT_MS) {
            milkRepo.observeCattle(uid).first { list -> list.any { it.id == cattleId } }
        }
        val created = cattleList.first { it.id == cattleId }
        assertFalse(created.isActive)
        assertEquals("Buttercup", created.name)

        val updateRes = withTimeout(TIMEOUT_MS) {
            milkRepo.updateCattle(created.copy(isActive = true))
        }
        assertTrue(updateRes.isSuccess)

        val milkId = withTimeout(TIMEOUT_MS) {
            milkRepo.addMilkRecord(
                MilkRecord(
                    cattleId = cattleId,
                    cattleTag = "COW-201",
                    cattleName = "Buttercup",
                    date = "2026-11-13",
                    morningLitres = 9.5,
                    eveningLitres = 8.0,
                    notes = "Good yield"
                )
            ).getOrThrow()
        }
        val records = withTimeout(FLOW_TIMEOUT_MS) {
            milkRepo.observeMilkRecords(uid).first { list -> list.any { it.id == milkId } }
        }
        assertEquals(17.5, records.first { it.id == milkId }.totalLitres, 0.01)
    }

    @Test
    fun feedingScheduleCompletion_deductsFeedStockAtomically() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val feedingRepo = FeedingRepository(firestore, auth)

        val feedId = withTimeout(TIMEOUT_MS) {
            feedingRepo.addFeedItem(
                FeedItem(
                    name = "Silage Corn",
                    category = "Silage",
                    quantity = 200.0,
                    unit = "kg",
                    costPerUnit = 0.5,
                    minStockLevel = 40.0
                )
            ).getOrThrow()
        }

        val scheduleId = withTimeout(TIMEOUT_MS) {
            feedingRepo.addFeedingSchedule(
                FeedingSchedule(
                    title = "Evening Silage",
                    targetGroup = "All Milking Cows",
                    feedId = feedId,
                    feedName = "Silage Corn",
                    quantity = 30.0,
                    unit = "kg",
                    feedingTime = "17:30",
                    isActive = true,
                    lastCompletedDate = ""
                )
            ).getOrThrow()
        }

        val feedItem = withTimeout(FLOW_TIMEOUT_MS) {
            feedingRepo.observeFeedItems(uid).first { list -> list.any { it.id == feedId } }
        }.first { it.id == feedId }

        val schedule = withTimeout(FLOW_TIMEOUT_MS) {
            feedingRepo.observeFeedingSchedules(uid).first { list -> list.any { it.id == scheduleId } }
        }.first { it.id == scheduleId }

        val completionRes = withTimeout(TIMEOUT_MS) {
            feedingRepo.completeFeedingActivity(
                schedule = schedule,
                feedItem = feedItem,
                quantityConsumed = 30.0,
                date = "2026-11-13",
                time = "17:35",
                notes = "Fed on time"
            )
        }
        assertTrue(completionRes.isSuccess)

        val updatedFeed = withTimeout(FLOW_TIMEOUT_MS) {
            feedingRepo.observeFeedItems(uid).first { list ->
                list.firstOrNull { it.id == feedId }?.quantity == 170.0
            }
        }.first { it.id == feedId }
        assertEquals(170.0, updatedFeed.quantity, 0.01)
    }

    @Test
    fun transactionsAndCrossUserIsolation_enforcesPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceFinance = FinanceRepository(firestore, auth)
        val txId = withTimeout(TIMEOUT_MS) {
            aliceFinance.addTransaction(
                FarmTransaction(
                    date = "2026-11-13",
                    type = "Income",
                    category = "Milk Sales",
                    description = "Evening bulk sale",
                    amount = 120.0,
                    quantityLitres = 80.0,
                    pricePerLitre = 1.5,
                    paymentMethod = "UPI"
                )
            ).getOrThrow()
        }
        assertTrue(txId.isNotBlank())

        // Cross-user isolation: Bob cannot read Alice's transactions
        signInTestUser(BOB_EMAIL)
        val bobFinance = FinanceRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobFinance.observeTransactions(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for cross-user observation")
        } catch (e: Exception) {
            val fsException = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
        }

        // Unauthenticated isolation
        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobFinance.observeTransactions(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated observation")
        } catch (e: Exception) {
            val fsException = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_dairy@test.com"
        const val BOB_EMAIL = "bob_dairy@test.com"
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
