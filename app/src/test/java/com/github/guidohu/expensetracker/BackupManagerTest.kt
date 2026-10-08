package com.github.guidohu.expensetracker

import com.github.guidohu.expensetracker.data.BackupException
import com.github.guidohu.expensetracker.data.BackupManager
import com.github.guidohu.expensetracker.data.BackupSettings
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.Csv
import com.github.guidohu.expensetracker.data.Expense
import com.github.guidohu.expensetracker.data.WishlistItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupManagerTest {

    private class FakeSettings(var values: Map<String, String> = emptyMap()) : BackupSettings {
        override fun exportBackupSettings() = values
        override fun restoreBackupSettings(settings: Map<String, String>) { values = settings }
    }

    private class Env(settings: BackupSettings = FakeSettings()) {
        val categories = FakeCategoryDao()
        val expenses = FakeExpenseDao()
        val wishlist = FakeWishlistDao()
        val manager = BackupManager(categories, expenses, wishlist, settings, inTransaction = { it() })
    }

    private fun zipOf(vararg files: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            files.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun csvRoundTripsAwkwardValuesAndKeepsNullDistinctFromEmpty() {
        val sb = StringBuilder()
        val row = listOf("plain", "with,comma", "with \"quote\"", "multi\nline", "", null, " spaced ")
        Csv.writeRow(sb, row)
        Csv.writeRow(sb, listOf("second", null, null, "x", "y", "z", "w"))
        val parsed = Csv.parse(sb.toString())
        assertEquals(2, parsed.size)
        assertEquals(row, parsed[0])
        assertEquals("second", parsed[1][0])
    }

    @Test
    fun exportThenRestoreReproducesAllDataAndSettings() = runBlocking {
        val source = Env(FakeSettings(mapOf("default_currency" to "CHF", "monthly_budget" to "1500.5")))
        source.categories.insert(Category(id = 3, name = "Food, \"fancy\"", color = -123456))
        source.categories.insert(Category(id = 7, name = "Other", color = 42))
        source.expenses.insert(
            Expense(id = 10, amount = 12.34, currencyCode = "EUR", exchangeRate = 0.95, categoryId = 3,
                title = "Lunch", notes = "line1\nline2", date = 20000, mood = "HAPPY", priority = "NEED",
                url = "https://example.com/?a=1,2", wishlistAddedAt = 19990)
        )
        source.expenses.insert(
            Expense(id = 11, amount = 1.0, currencyCode = "USD", exchangeRate = 1.0, categoryId = 7,
                title = "", notes = "", date = 20001)
        )
        source.wishlist.insert(
            WishlistItem(id = 5, title = "Headphones", price = 249.0, currencyCode = "USD", note = "", url = null,
                previewTitle = "P", previewDescription = null, previewImageUrl = null, createdAt = 19000,
                priority = "WANT", mood = null, categoryId = 7)
        )
        val bytes = ByteArrayOutputStream().also { source.manager.export(it) }.toByteArray()

        val targetSettings = FakeSettings()
        val target = Env(targetSettings)
        // Pre-existing data must be replaced, not merged.
        target.categories.insert(Category(id = 99, name = "Stale", color = 1))
        val summary = target.manager.restore(ByteArrayInputStream(bytes))

        assertEquals(2, summary.categories); assertEquals(2, summary.expenses); assertEquals(1, summary.wishlistItems)
        assertEquals(source.categories.getAllOnce(), target.categories.getAllOnce())
        assertEquals(source.expenses.getAllOnce(), target.expenses.getAllOnce())
        assertEquals(source.wishlist.getAllOnce(), target.wishlist.getAllOnce())
        assertEquals(source.expenses.getAllOnce()[1].title, "")
        assertNull(target.wishlist.getAllOnce()[0].url)
        assertEquals("CHF", targetSettings.values["default_currency"])
    }

    @Test
    fun invalidBackupLeavesExistingDataUntouched() = runBlocking {
        val env = Env()
        env.categories.insert(Category(id = 1, name = "Keep", color = 1))
        val bad = zipOf(
            "backup_info.csv" to "key,value\r\nformat_version,1\r\n",
            "settings.csv" to "key,value\r\n",
            "categories.csv" to "id,name,color\r\n1,Food,5\r\n",
            "expenses.csv" to "id,amount,currencyCode,exchangeRate,categoryId,title,notes,date,mood,priority,url,wishlistAddedAt\r\n" +
                "1,2.0,USD,1.0,999,Lunch,,100,,WANT,,\r\n",
            "wishlist_items.csv" to "id,title,price,currencyCode,note,url,previewTitle,previewDescription,previewImageUrl,createdAt,priority,mood,categoryId\r\n",
        )
        try {
            env.manager.restore(ByteArrayInputStream(bad))
            fail("expected BackupException")
        } catch (e: BackupException) {
            assertTrue(e.message!!.contains("expenses.csv"))
        }
        assertEquals(listOf("Keep"), env.categories.getAllOnce().map { it.name })
    }

    @Test
    fun rejectsNonBackupAndNewerFormat() {
        val env = Env()
        for (bytes in listOf(
            "not a zip".toByteArray(),
            zipOf("other.txt" to "hi"),
            zipOf("backup_info.csv" to "key,value\r\nformat_version,99\r\n"),
        )) {
            try {
                runBlocking { env.manager.restore(ByteArrayInputStream(bytes)) }
                fail("expected BackupException")
            } catch (_: BackupException) {
            }
        }
    }
}
