package com.eymen.gelir.backup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/** Private receipt files. Images are resized and re-encoded as JPEG before storage. */
object ReceiptStorage {
    const val MAX_BYTES = 2 * 1024 * 1024
    private const val MAX_SOURCE = 12 * 1024 * 1024
    private val safeName = Regex("[0-9a-fA-F-]{36}\\.jpg")

    fun fileName(uuid: String): String {
        UUID.fromString(uuid)
        return uuid + ".jpg"
    }

    private fun file(context: Context, name: String): File {
        require(safeName.matches(name)) { "Geçersiz fotoğraf dosyası." }
        val folder = File(context.filesDir, "receipts")
        check(folder.isDirectory || folder.mkdirs())
        return File(folder, name)
    }

    fun read(context: Context, name: String): ByteArray {
        val bytes = file(context, name).readBytes()
        require(bytes.size in 4..MAX_BYTES && isJpeg(bytes)) { "Fiş fotoğrafı okunamadı." }
        return bytes
    }

    fun delete(context: Context, name: String?) {
        if (name != null) runCatching { file(context, name).delete() }
    }

    fun saveFromUri(context: Context, uri: Uri, uuid: String): String {
        val input = context.contentResolver.openInputStream(uri) ?: error("Görsel açılamadı.")
        val raw = input.use { readLimited(it, MAX_SOURCE) }
        val bound = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bound)
        require(bound.outWidth in 1..20000 && bound.outHeight in 1..20000) { "Geçersiz görsel." }
        val sample = generateSequence(1) { it * 2 }.first {
            maxOf(bound.outWidth, bound.outHeight) / it <= 1600
        }
        val bitmap = BitmapFactory.decodeByteArray(
            raw, 0, raw.size, BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: error("Görsel açılamadı.")
        val target = if (maxOf(bitmap.width, bitmap.height) > 1600) {
            val ratio = 1600.0 / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else bitmap
        val output = ByteArrayOutputStream()
        for (quality in listOf(82, 65, 45)) {
            output.reset()
            require(target.compress(Bitmap.CompressFormat.JPEG, quality, output)) { "Görsel sıkıştırılamadı." }
            if (output.size() <= MAX_BYTES) break
        }
        if (target !== bitmap) target.recycle()
        bitmap.recycle()
        return write(context, uuid, output.toByteArray())
    }

    fun write(context: Context, uuid: String, bytes: ByteArray): String {
        require(bytes.size in 4..MAX_BYTES && isJpeg(bytes)) { "Geçersiz JPEG." }
        val name = fileName(uuid)
        val dest = file(context, name)
        val temp = File.createTempFile("image", ".tmp", dest.parentFile)
        try {
            FileOutputStream(temp).use {
                it.write(bytes)
                it.fd.sync()
            }
            check(temp.renameTo(dest)) { "Fotoğraf kaydedilemedi." }
        } finally { temp.delete() }
        return name
    }

    private fun readLimited(input: InputStream, limit: Int): ByteArray {
        val data = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(data.size() + count <= limit) { "Görsel çok büyük." }
            data.write(buffer, 0, count)
        }
        return data.toByteArray()
    }

    fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 4 && (bytes[0].toInt() and 255) == 255 &&
            (bytes[1].toInt() and 255) == 216 &&
            (bytes[bytes.lastIndex - 1].toInt() and 255) == 255 &&
            (bytes[bytes.lastIndex].toInt() and 255) == 217
}
