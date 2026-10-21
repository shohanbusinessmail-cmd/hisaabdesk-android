
package com.hisaabdesk.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hisaabdesk.app.data.local.HisaabDatabase
import com.hisaabdesk.app.data.local.entity.FinancialAccount
import com.hisaabdesk.app.data.local.entity.LedgerEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented: requires emulator/device. NOT run in sandbox or standard CI (documented in TEST_PLAN). */
@RunWith(AndroidJUnit4::class)
class DatabaseSmokeTest {
    @Test fun insertAndBalancePersist() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(ctx, HisaabDatabase::class.java).build()
        val id = db.accountDao().insert(FinancialAccount(name = "নগদ", type = "CASH", openingBalancePoisha = 100_000))
        db.ledgerDao().insert(LedgerEntry(groupId = "t1", accountId = id, type = "INCOME", amountPoisha = 50_000, occurredAt = 1L))
        assertEquals(50_000L, db.accountDao().ledgerSum(id))
        db.close()
    }
}
