package com.calmcorner.app

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.calmcorner.app.data.SessionRecord
import com.calmcorner.app.data.WEEK_MS
import com.calmcorner.app.data.summarise
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds a one-page weekly summary PDF and opens the share sheet. Only sent where the grown-up chooses. */
object SummaryPdf {
    fun share(context: Context, records: List<SessionRecord>) {
        val now = System.currentTimeMillis()
        val file = build(context, records, now)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Calm Corner weekly summary")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share weekly summary").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun build(context: Context, records: List<SessionRecord>, now: Long): File {
        val s = summarise(records, now)
        val day = SimpleDateFormat("EEE d MMM", Locale.getDefault())
        val time = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()) // A4 in points
        val c = page.canvas
        val title = Paint().apply { textSize = 22f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val h2 = Paint().apply { textSize = 15f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val body = Paint().apply { textSize = 12f; isAntiAlias = true }
        val grey = Paint().apply { textSize = 10f; color = 0xFF777777.toInt(); isAntiAlias = true }
        var y = 60f
        val x = 48f
        c.drawText("Calm Corner: weekly summary", x, y, title); y += 22f
        c.drawText("${day.format(Date(now - WEEK_MS))} to ${day.format(Date(now))}", x, y, grey); y += 30f

        c.drawText("This week", x, y, h2); y += 20f
        c.drawText("Finished exercises: ${s.stars}", x, y, body); y += 17f
        if (s.checkIns > 0) { c.drawText("Felt calmer afterwards: ${s.feltBetter} of ${s.checkIns} check-ins", x, y, body); y += 17f }
        s.mostHelpful?.let { c.drawText("Most helpful: ${it.title}", x, y, body); y += 17f }
        y += 8f
        if (s.perTool.isNotEmpty()) {
            c.drawText("Tools used", x, y, h2); y += 20f
            s.perTool.forEach { (tool, n) -> c.drawText("${tool.title}: $n", x, y, body); y += 16f }
            y += 8f
        }

        val week = records.filter { now - it.time in 0 until WEEK_MS }.asReversed()
        if (week.isNotEmpty()) {
            c.drawText("Sessions (before → after colour)", x, y, h2); y += 20f
            val dot = Paint().apply { isAntiAlias = true }
            for (r in week) {
                if (y > 790f) { c.drawText("…", x, y, body); break }
                c.drawText(time.format(Date(r.time)), x, y, body)
                c.drawText(r.tool.title, x + 140f, y, body)
                fun zoneDot(cx: Float, z: com.calmcorner.app.data.Zone?) {
                    dot.color = z?.color?.toArgb() ?: 0xFFBDBDBD.toInt()
                    c.drawCircle(cx, y - 4f, 5f, dot)
                }
                zoneDot(x + 330f, r.before)
                c.drawText("→", x + 342f, y, body)
                zoneDot(x + 365f, r.after)
                c.drawText(listOfNotNull(r.before?.label, r.after?.label).joinToString(" → "), x + 380f, y, grey)
                y += 16f
            }
        } else {
            c.drawText("No exercises this week.", x, y, body)
        }
        c.drawText("Made by Calm Corner. All data stays on the phone unless shared like this.", x, 820f, grey)
        doc.finishPage(page)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "calm-corner-summary.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
