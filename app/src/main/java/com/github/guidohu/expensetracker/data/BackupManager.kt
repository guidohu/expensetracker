package com.github.guidohu.expensetracker.data

import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** A backup couldn't be read or applied. [message] is written for the user. */
class BackupException(message: String) : Exception(message)

data class BackupSummary(val categories: Int, val expenses: Int, val wishlistItems: Int)

/** The slice of app settings that travels in a backup. Implemented by [UserPreferences]; an interface so tests needn't build one. */
interface BackupSettings {
    fun exportBackupSettings(): Map<String, String>
    fun restoreBackupSettings(settings: Map<String, String>)
}

/** Runs a block atomically — Room's `withTransaction` in the app, a plain call in tests. */
typealias TransactionRunner = suspend (suspend () -> Unit) -> Unit

/**
 * Exports all app data to a single .zip of CSV files, and restores from one.
 *
 * CSV inside a zip keeps the backup openable in any spreadsheet for inspection, while the zip
 * keeps it a single file to hand to a file picker. Row ids are written and restored as-is so the
 * category references of expenses and wishlist items stay valid.
 *
 * Restore replaces everything: the whole file is parsed and validated first, and the database
 * is only touched (inside one transaction) once it is known to be good, so a bad file can never
 * leave the user with half their data.
 */
class BackupManager(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val wishlistDao: WishlistDao,
    private val settings: BackupSettings,
    private val inTransaction: TransactionRunner,
) {

    suspend fun export(output: OutputStream): BackupSummary {
        val categories = categoryDao.getAllOnce()
        val expenses = expenseDao.getAllOnce()
        val wishlist = wishlistDao.getAllOnce()
        val currentSettings = settings.exportBackupSettings()

        ZipOutputStream(output).use { zip ->
            zip.writeCsv(INFO_FILE, listOf(
                listOf("key", "value"),
                listOf("format_version", FORMAT_VERSION.toString()),
                listOf("created_at", Instant.now().toString()),
            ))
            zip.writeCsv(SETTINGS_FILE, listOf(listOf("key", "value")) + currentSettings.map { listOf(it.key, it.value) })
            zip.writeCsv(CATEGORIES_FILE, listOf(CATEGORY_COLUMNS) + categories.map {
                listOf(it.id.toString(), it.name, it.color.toString())
            })
            zip.writeCsv(EXPENSES_FILE, listOf(EXPENSE_COLUMNS) + expenses.map {
                listOf(
                    it.id.toString(), it.amount.toString(), it.currencyCode, it.exchangeRate.toString(),
                    it.categoryId.toString(), it.title, it.notes, it.date.toString(), it.mood,
                    it.priority, it.url, it.wishlistAddedAt?.toString(),
                )
            })
            zip.writeCsv(WISHLIST_FILE, listOf(WISHLIST_COLUMNS) + wishlist.map {
                listOf(
                    it.id.toString(), it.title, it.price?.toString(), it.currencyCode, it.note, it.url,
                    it.previewTitle, it.previewDescription, it.previewImageUrl, it.createdAt.toString(),
                    it.priority, it.mood, it.categoryId?.toString(),
                )
            })
        }
        return BackupSummary(categories.size, expenses.size, wishlist.size)
    }

    suspend fun restore(input: InputStream): BackupSummary {
        val files = readZip(input)
        val info = files[INFO_FILE]?.let { Table(INFO_FILE, it, listOf("key", "value")) }
            ?: throw BackupException("This isn't an Expense Tracker backup.")
        val version = info.rows.firstOrNull { it.required("key") == "format_version" }?.text("value")?.toIntOrNull()
        if (version == null || version > FORMAT_VERSION) {
            throw BackupException("This backup was made by a newer version of the app. Update the app and try again.")
        }

        val restoredSettings = Table(SETTINGS_FILE, files.require(SETTINGS_FILE), listOf("key", "value"))
            .rows.associate { it.required("key") to it.text("value") }

        val categories = Table(CATEGORIES_FILE, files.require(CATEGORIES_FILE), CATEGORY_COLUMNS).rows.map {
            Category(id = it.long("id"), name = it.text("name"), color = it.int("color"))
        }
        val categoryIds = categories.map { it.id }.toSet()
        if (categoryIds.size != categories.size) throw BackupException("The backup contains duplicate categories.")

        val expenses = Table(EXPENSES_FILE, files.require(EXPENSES_FILE), EXPENSE_COLUMNS).rows.map {
            Expense(
                id = it.long("id"),
                amount = it.double("amount"),
                currencyCode = it.required("currencyCode"),
                exchangeRate = it.double("exchangeRate"),
                categoryId = it.long("categoryId").also { id ->
                    if (id !in categoryIds) it.fail("refers to a category that isn't in the backup")
                },
                title = it.text("title"),
                notes = it.text("notes"),
                date = it.long("date"),
                mood = it.optional("mood"),
                priority = it.required("priority"),
                url = it.optional("url"),
                wishlistAddedAt = it.optionalLong("wishlistAddedAt"),
            )
        }
        if (expenses.map { it.id }.toSet().size != expenses.size) throw BackupException("The backup contains duplicate expenses.")

        val wishlist = Table(WISHLIST_FILE, files.require(WISHLIST_FILE), WISHLIST_COLUMNS).rows.map {
            WishlistItem(
                id = it.long("id"),
                title = it.text("title"),
                price = it.optionalDouble("price"),
                currencyCode = it.optional("currencyCode"),
                note = it.text("note"),
                url = it.optional("url"),
                previewTitle = it.optional("previewTitle"),
                previewDescription = it.optional("previewDescription"),
                previewImageUrl = it.optional("previewImageUrl"),
                createdAt = it.long("createdAt"),
                priority = it.required("priority"),
                mood = it.optional("mood"),
                // Same outcome the database would give if the category had been deleted.
                categoryId = it.optionalLong("categoryId")?.takeIf { id -> id in categoryIds },
            )
        }
        if (wishlist.map { it.id }.toSet().size != wishlist.size) throw BackupException("The backup contains duplicate wishlist items.")

        inTransaction {
            expenseDao.deleteAll()
            wishlistDao.deleteAll()
            categoryDao.deleteAll()
            categories.forEach { categoryDao.insert(it) }
            expenses.forEach { expenseDao.insert(it) }
            wishlist.forEach { wishlistDao.insert(it) }
        }
        settings.restoreBackupSettings(restoredSettings)
        return BackupSummary(categories.size, expenses.size, wishlist.size)
    }

    private fun ZipOutputStream.writeCsv(name: String, rows: List<List<String?>>) {
        putNextEntry(ZipEntry(name))
        val text = StringBuilder().also { sb -> rows.forEach { Csv.writeRow(sb, it) } }
        write(text.toString().toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun readZip(input: InputStream): Map<String, String> {
        val files = mutableMapOf<String, String>()
        try {
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zip.readBounded(MAX_ENTRY_BYTES)
                        files[entry.name.substringAfterLast('/')] = bytes.toString(Charsets.UTF_8).removePrefix("﻿")
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: java.io.IOException) {
            throw BackupException("This file couldn't be read as a backup.")
        }
        if (files.isEmpty()) throw BackupException("This isn't an Expense Tracker backup.")
        return files
    }

    private fun ZipInputStream.readBounded(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = read(buffer)
            if (n < 0) break
            if (out.size() + n > limit) throw BackupException("This file is too large to be a backup.")
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private fun Map<String, String>.require(name: String): String =
        this[name] ?: throw BackupException("The backup is incomplete ($name is missing).")

    /** A parsed CSV file whose header has been checked to contain every column in [columns]. */
    private class Table(private val file: String, text: String, columns: List<String>) {
        val rows: List<Row>

        init {
            val parsed = Csv.parse(text)
            val header = parsed.firstOrNull()?.map { it.orEmpty() }
                ?: throw BackupException("The backup is damaged ($file is empty).")
            val missing = columns.filter { it !in header }
            if (missing.isNotEmpty()) throw BackupException("The backup is damaged ($file lacks ${missing.joinToString()}).")
            rows = parsed.drop(1).mapIndexed { index, fields ->
                Row(file, index + 2, header.withIndex().associate { (i, name) -> name to fields.getOrNull(i) })
            }
        }
    }

    private class Row(private val file: String, private val line: Int, private val values: Map<String, String?>) {
        fun fail(problem: String): Nothing = throw BackupException("The backup is damaged ($file, line $line $problem).")

        fun optional(column: String): String? = values[column]
        fun text(column: String): String = values[column].orEmpty()
        fun required(column: String): String = values[column]?.takeIf { it.isNotEmpty() } ?: fail("has no $column")
        fun optionalLong(column: String): Long? = optional(column)?.let { long(column) }
        fun optionalDouble(column: String): Double? = optional(column)?.let { double(column) }
        fun long(column: String): Long = required(column).toLongOrNull() ?: fail("has an invalid $column")
        fun int(column: String): Int = required(column).toIntOrNull() ?: fail("has an invalid $column")
        fun double(column: String): Double =
            required(column).toDoubleOrNull()?.takeIf { it.isFinite() } ?: fail("has an invalid $column")
    }

    companion object {
        const val FORMAT_VERSION = 1
        private const val MAX_ENTRY_BYTES = 64 * 1024 * 1024

        private const val INFO_FILE = "backup_info.csv"
        private const val SETTINGS_FILE = "settings.csv"
        private const val CATEGORIES_FILE = "categories.csv"
        private const val EXPENSES_FILE = "expenses.csv"
        private const val WISHLIST_FILE = "wishlist_items.csv"

        private val CATEGORY_COLUMNS = listOf("id", "name", "color")
        private val EXPENSE_COLUMNS = listOf(
            "id", "amount", "currencyCode", "exchangeRate", "categoryId", "title", "notes", "date",
            "mood", "priority", "url", "wishlistAddedAt",
        )
        private val WISHLIST_COLUMNS = listOf(
            "id", "title", "price", "currencyCode", "note", "url", "previewTitle", "previewDescription",
            "previewImageUrl", "createdAt", "priority", "mood", "categoryId",
        )
    }
}
