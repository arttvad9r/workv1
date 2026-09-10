package com.arttvad.worktime

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

class DocumentRoundTripProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = when (uri.lastPathSegment?.substringAfterLast('.')) {
        "csv" -> "text/csv"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "pdf" -> "application/pdf"
        "wtbk" -> "application/octet-stream"
        else -> "application/octet-stream"
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val file = fileFor(uri)
        val flags = when {
            mode.startsWith("w") -> {
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_WRITE_ONLY
            }
            mode.startsWith("a") -> {
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_APPEND or
                    ParcelFileDescriptor.MODE_WRITE_ONLY
            }
            mode.startsWith("r") -> ParcelFileDescriptor.MODE_READ_ONLY
            else -> throw FileNotFoundException("Unsupported mode: $mode")
        }
        return ParcelFileDescriptor.open(file, flags)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return if (fileFor(uri).delete()) 1 else 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private fun fileFor(uri: Uri): File {
        val name = uri.lastPathSegment
            ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
            ?.takeIf(String::isNotBlank)
            ?: throw FileNotFoundException("Missing document name")
        val root = File(requireNotNull(context).cacheDir, "document-roundtrip").apply { mkdirs() }
        return File(root, name)
    }

    companion object {
        const val AUTHORITY = "com.arttvad.worktime.test.documents"

        fun uri(name: String): Uri = Uri.parse("content://$AUTHORITY/$name")
    }
}
