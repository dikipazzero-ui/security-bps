package com.example.media

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.OfficerProfile
import com.example.data.model.ReportCategory
import com.example.data.model.SecurityReport
import java.io.FileOutputStream

object PdfReportPrinter {

    fun printDailyReport(
        context: Context,
        dateLabel: String,
        activeShift: String,
        profile: OfficerProfile,
        reports: List<SecurityReport>
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Laporan_Security_BPS_${activeShift.replace(" ", "_")}"
        val adapter = SecurityReportPrintAdapter(
            context = context,
            dateLabel = dateLabel,
            activeShift = activeShift,
            profile = profile,
            reports = reports
        )
        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        )
    }
}

private class SecurityReportPrintAdapter(
    private val context: Context,
    private val dateLabel: String,
    private val activeShift: String,
    private val profile: OfficerProfile,
    private val reports: List<SecurityReport>
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        pdfDocument = PrintedPdfDocument(context, newAttributes)
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("Laporan_Harian_Security_BPS.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val doc = pdfDocument ?: run {
            callback.onWriteFailed("Dokumen PDF gagal dibuat.")
            return
        }

        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 36f
            val contentWidth = (pageWidth - margin * 2).toInt()

            var pageNumber = 1
            var page = doc.startPage(pageNumber - 1)
            var canvas = page.canvas
            var y = margin

            fun startNewPage() {
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(pageNumber - 1)
                canvas = page.canvas
                y = margin
            }

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > pageHeight - margin) {
                    startNewPage()
                }
            }

            // Kop Surat Header
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9.5f
            }
            val linePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                strokeWidth = 2f
            }

            canvas.drawText("LAPORAN HARIAN SATUAN PENGAMANAN (SECURITY)", margin, y + 14f, titlePaint)
            y += 20f
            canvas.drawText("PLOT PENGAMANAN: BADAN PUSAT STATISTIK (BPS)", margin, y + 12f, subTitlePaint)
            y += 20f

            canvas.drawText("Tanggal: $dateLabel   |   Shift Aktif: $activeShift", margin, y + 10f, metaPaint)
            y += 14f
            canvas.drawText("Petugas: ${profile.name} (${profile.nrp}) — ${profile.role}", margin, y + 10f, metaPaint)
            y += 16f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 18f

            val catHeaderBgPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                style = Paint.Style.FILL
            }
            val catHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(251, 191, 36)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val itemHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9.5f
            }
            val borderPaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            ReportCategory.entries.forEach { cat ->
                ensureSpace(44f)
                canvas.drawRect(margin, y, pageWidth - margin, y + 22f, catHeaderBgPaint)
                canvas.drawText(cat.fullTitle, margin + 8f, y + 15f, catHeaderTextPaint)
                y += 28f

                val catItems = reports.filter { it.category.toInt() == cat.number }
                if (catItems.isEmpty()) {
                    ensureSpace(24f)
                    canvas.drawText("- Nihil / belum ada rincian kegiatan pada kategori ini.", margin + 8f, y + 12f, bodyPaint)
                    y += 24f
                } else {
                    catItems.forEachIndexed { idx, item ->
                        val headerLine = "${idx + 1}. [${item.time} WIB | ${item.shift}] • ${item.status} • Oleh: ${item.officerName}"
                        val descLayout = StaticLayout.Builder.obtain(
                            item.description,
                            0,
                            item.description.length,
                            bodyPaint,
                            contentWidth - 16
                        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(2f, 1.0f)
                            .build()

                        val photoRowsHeight = if (item.photos.isNotEmpty()) 76f else 0f
                        val blockHeight = 20f + descLayout.height + photoRowsHeight + 14f
                        ensureSpace(blockHeight)

                        val boxTop = y
                        canvas.drawText(headerLine, margin + 8f, y + 14f, itemHeaderPaint)
                        y += 20f

                        canvas.save()
                        canvas.translate(margin + 8f, y)
                        descLayout.draw(canvas)
                        canvas.restore()
                        y += descLayout.height + 6f

                        if (item.photos.isNotEmpty()) {
                            var photoX = margin + 8f
                            val thumbW = 112f
                            val thumbH = 68f
                            item.photos.take(4).forEach { b64 ->
                                val bmp = ImageUtils.decodeBase64ToBitmap(b64)
                                if (bmp != null) {
                                    val dest = Rect(
                                        photoX.toInt(),
                                        y.toInt(),
                                        (photoX + thumbW).toInt(),
                                        (y + thumbH).toInt()
                                    )
                                    canvas.drawBitmap(bmp, null, dest, null)
                                    bmp.recycle()
                                }
                                photoX += thumbW + 8f
                            }
                            y += thumbH + 6f
                        }

                        y += 6f
                        canvas.drawRect(margin, boxTop, pageWidth - margin, y, borderPaint)
                        y += 8f
                    }
                }
                y += 8f
            }

            doc.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use { out ->
                doc.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.localizedMessage ?: "Gagal menulis PDF.")
        } finally {
            doc.close()
            pdfDocument = null
        }
    }

    override fun onFinish() {
        pdfDocument?.close()
        pdfDocument = null
    }
}
