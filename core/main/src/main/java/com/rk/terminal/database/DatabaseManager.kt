package com.rk.terminal.database

import android.content.Context
import com.google.gson.Gson
import com.rk.terminal.model.AppDatabase
import com.rk.terminal.model.Media
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DatabaseManager(private val context: Context) {
    private val gson = Gson()
    private val dbFile = File(context.filesDir, "database.json")

    fun saveDatabase(database: AppDatabase) {
        dbFile.writeText(gson.toJson(database))
    }

    fun loadDatabase(): AppDatabase {
        return if (dbFile.exists()) {
            gson.fromJson(dbFile.readText(), AppDatabase::class.java)
        } else {
            AppDatabase()
        }
    }

    fun createZipBackup(): File {
        val zipFile = File(context.cacheDir, "database_backup.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val entry = ZipEntry("database.json")
            zos.putNextEntry(entry)
            FileInputStream(dbFile).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
        return zipFile
    }

    fun restoreFromZip(zipFile: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "database.json") {
                    FileOutputStream(dbFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}
