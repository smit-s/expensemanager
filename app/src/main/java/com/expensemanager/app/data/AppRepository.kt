package com.expensemanager.app.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val stateFile = File(appContext.filesDir, "app_state.json")
    private val backupDir = File(appContext.filesDir, "backups").apply { mkdirs() }
    private val exportFile = File(appContext.filesDir, "transactions_export.csv")

    fun loadState(): PersistedState? {
        if (!stateFile.exists()) return null
        return runCatching { json.decodeFromString<PersistedState>(stateFile.readText()) }.getOrNull()
    }

    fun saveState(state: PersistedState) {
        stateFile.writeText(json.encodeToString(state))
    }

    fun backupNow(): String {
        if (!stateFile.exists()) saveState(PersistedState())
        val backup = File(backupDir, "backup_${System.currentTimeMillis()}.bk")
        stateFile.copyTo(backup, overwrite = true)
        return backup.name
    }

    fun backupToDownloads(): String {
        if (!stateFile.exists()) saveState(PersistedState())
        val fileName = "expense_manager_backup_${System.currentTimeMillis()}.bk"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }
        val resolver = appContext.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Failed to create backup file in Downloads")

        resolver.openOutputStream(uri)?.use { output ->
            stateFile.inputStream().use { input -> input.copyTo(output) }
        } ?: throw IllegalStateException("Failed to write backup file")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val doneValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, doneValues, null, null)
        }
        return fileName
    }

    fun restoreLatest(): Boolean {
        val latest = backupDir.listFiles()
            ?.filter { it.extension.equals("bk", ignoreCase = true) }
            ?.maxByOrNull { it.lastModified() }
            ?: return false
        latest.copyTo(stateFile, overwrite = true)
        return true
    }

    fun restoreFromUri(uri: Uri): Boolean {
        val input = appContext.contentResolver.openInputStream(uri) ?: return false
        input.use { stream ->
            stateFile.outputStream().use { out -> stream.copyTo(out) }
        }
        return loadState() != null
    }

    fun exportCsv(transactions: List<ExpenseTransaction>): String {
        val lines = buildList {
            add("id,type,amount,category,paymentMode,note,tags,date,time")
            transactions.forEach {
                add(
                    listOf(
                        it.id,
                        it.type.name,
                        it.amount.toString(),
                        csv(it.category),
                        csv(it.paymentMode),
                        csv(it.note),
                        csv(it.tags.joinToString("|")),
                        it.date.toString(),
                        it.time.toString()
                    ).joinToString(",")
                )
            }
        }
        val csvText = lines.joinToString("\n")
        exportFile.writeText(csvText)

        val fileName = "transactions_export_${System.currentTimeMillis()}.csv"
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            val resolver = appContext.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Failed to create CSV file in Downloads")

            resolver.openOutputStream(uri)?.use { output ->
                output.write(csvText.toByteArray())
            } ?: throw IllegalStateException("Failed to write CSV file")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val doneValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, doneValues, null, null)
            }
            "Downloads/$fileName"
        }.getOrElse { exportFile.absolutePath }
    }

    fun importCsv(): List<ExpenseTransaction> {
        if (!exportFile.exists()) return emptyList()
        return exportFile.readLines().drop(1).mapNotNull { line ->
            val p = parseCsvLine(line)
            if (p.size < 9) return@mapNotNull null
            runCatching {
                ExpenseTransaction(
                    id = p[0],
                    type = TransactionType.valueOf(p[1]),
                    amount = p[2].toDouble(),
                    category = p[3],
                    paymentMode = p[4],
                    note = p[5],
                    tags = if (p[6].isBlank()) emptyList() else p[6].split("|"),
                    date = LocalDate.parse(p[7]),
                    time = LocalTime.parse(p[8])
                )
            }.getOrNull()
        }
    }

    fun getStateFile(): File = stateFile

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    out.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        out.add(current.toString())
        return out
    }
}


