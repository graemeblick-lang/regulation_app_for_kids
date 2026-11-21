package com.calmcorner.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A grown-up-made calm idea for her, e.g. "Cuddle the dog", with an optional photo. */
data class CopingCard(val id: String, val text: String, val photo: String?)

/** Coping cards and photos, stored only in the app's private storage on this phone. */
class CopingCardStore(private val context: Context) {
    private val file = File(context.filesDir, "coping_cards.json")
    val cards = mutableStateListOf<CopingCard>().apply { addAll(load()) }

    fun add(text: String, photo: String?) {
        cards.add(CopingCard(UUID.randomUUID().toString(), text.trim(), photo))
        save()
    }

    fun remove(card: CopingCard) {
        card.photo?.let { Photos.delete(context, it) }
        cards.remove(card)
        save()
    }

    fun move(card: CopingCard, up: Boolean) {
        val i = cards.indexOf(card)
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in cards.indices) return
        cards[i] = cards[j]
        cards[j] = card
        save()
    }

    private fun load(): List<CopingCard> = try {
        if (!file.exists()) emptyList() else {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CopingCard(o.getString("id"), o.getString("text"), o.optString("photo").ifEmpty { null })
            }
        }
    } catch (e: Exception) { emptyList() }

    private fun save() {
        val arr = JSONArray()
        cards.forEach { c -> arr.put(JSONObject().put("id", c.id).put("text", c.text).put("photo", c.photo ?: "")) }
        file.writeText(arr.toString())
    }
}

val LocalCopingCards = staticCompositionLocalOf<CopingCardStore> { error("CopingCardStore not provided") }

/** Saves picked photos (shrunk) into private app storage. Nothing leaves the phone. */
object Photos {
    private fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    /** Copies a picked image into app storage, shrunk to at most 1080px. Returns the file name. */
    fun save(context: Context, uri: Uri): String? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2160) sample *= 2
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Could not read image")
        val scale = minOf(1f, 1080f / maxOf(bmp.width, bmp.height))
        val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        val name = UUID.randomUUID().toString() + ".jpg"
        File(dir(context), name).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        name
    } catch (e: Exception) { null }

    fun load(context: Context, name: String): Bitmap? = try {
        BitmapFactory.decodeFile(File(dir(context), name).absolutePath)
    } catch (e: Exception) { null }

    fun delete(context: Context, name: String) {
        File(dir(context), name).delete()
    }
}
