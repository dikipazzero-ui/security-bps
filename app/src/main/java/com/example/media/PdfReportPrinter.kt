package com.example.media

import android.content.Context
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
import com.example.data.model.AttendanceSheet
import com.example.data.model.AttendanceStatus
import com.example.data.model.DailyActivitySheet
import com.example.data.model.OfficerProfile
import com.example.data.model.PjuChecklistSheet
import com.example.data.model.PjuLampStatus
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

    fun printActivityTableSheet(
        context: Context,
        sheet: DailyActivitySheet,
        profile: OfficerProfile
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Tabel_Kegiatan_BPS_${sheet.sheetDate}_${sheet.shift.replace(" ", "_")}"
        val adapter = SecurityTablePrintAdapter(
            context = context,
            sheet = sheet,
            profile = profile
        )
        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        )
    }

    fun printAttendanceSheet(
        context: Context,
        sheet: AttendanceSheet,
        profile: OfficerProfile
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Absensi_4_Anggota_${sheet.shift.replace(" ", "_")}_BPS_${sheet.sheetDate}"
        val adapter = SecurityAttendancePrintAdapter(
            context = context,
            sheet = sheet,
            profile = profile
        )
        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        )
    }

    fun printPjuChecklistSheet(
        context: Context,
        sheet: PjuChecklistSheet,
        profile: OfficerProfile
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val safeMonth = sheet.monthYear.replace(" ", "_")
        val jobName = "Checklist_PJU_BPS_${safeMonth}"
        val adapter = SecurityPjuPrintAdapter(
            context = context,
            sheet = sheet,
            profile = profile
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
            val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 7.5f
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
                        val dateBadge = item.formattedDateBadge().takeIf { it.isNotBlank() }?.let { "$it • " }.orEmpty()
                        val headerLine = "${idx + 1}. [$dateBadge${item.time} WIB | ${item.shift}] • ${item.status} • Oleh: ${item.officerName}"
                        val descLayout = StaticLayout.Builder.obtain(
                            item.description,
                            0,
                            item.description.length,
                            bodyPaint,
                            contentWidth - 16
                        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(2f, 1.0f)
                            .build()

                        val hasCaptions = item.photoCaptions.any { it.isNotBlank() }
                        val photoRowsHeight = if (item.photos.isNotEmpty()) {
                            if (hasCaptions) 98f else 76f
                        } else {
                            0f
                        }
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
                            item.photos.take(4).forEachIndexed { pIdx, b64 ->
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
                                val caption = item.photoCaptions.getOrNull(pIdx)?.trim().orEmpty()
                                if (caption.isNotBlank()) {
                                    val capLayout = StaticLayout.Builder.obtain(
                                        caption,
                                        0,
                                        caption.length,
                                        captionPaint,
                                        thumbW.toInt()
                                    ).setMaxLines(2).build()
                                    canvas.save()
                                    canvas.translate(photoX, y + thumbH + 3f)
                                    capLayout.draw(canvas)
                                    canvas.restore()
                                }
                                photoX += thumbW + 8f
                            }
                            y += thumbH + (if (hasCaptions) 26f else 6f)
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

private class SecurityTablePrintAdapter(
    private val context: Context,
    private val sheet: DailyActivitySheet,
    private val profile: OfficerProfile
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
        val info = PrintDocumentInfo.Builder("Tabel_Pelaporan_Security_BPS_${sheet.sheetDate}.pdf")
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
            callback.onWriteFailed("Dokumen PDF tabel gagal dibuat.")
            return
        }

        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 28f
            val tableWidth = pageWidth - margin * 2

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

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9f
            }
            val headerFill = Paint().apply {
                color = Color.rgb(15, 23, 42)
                style = Paint.Style.FILL
            }
            val headerText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(251, 191, 36)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 8.5f
            }
            val cellStroke = Paint().apply {
                color = Color.rgb(100, 116, 139)
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }

            fun drawRowCells(
                colWidths: FloatArray,
                values: List<String>,
                isHeader: Boolean
            ) {
                val layouts = values.mapIndexed { idx, txt ->
                    val w = (colWidths[idx] - 8f).toInt().coerceAtLeast(10)
                    StaticLayout.Builder.obtain(
                        txt,
                        0,
                        txt.length,
                        if (isHeader) headerText else cellText,
                        w
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
                }
                val rowHeight = (layouts.maxOfOrNull { it.height } ?: 12) + 10f
                ensureSpace(rowHeight + 2f)

                var x = margin
                layouts.forEachIndexed { idx, layout ->
                    val colW = colWidths[idx]
                    if (isHeader) {
                        canvas.drawRect(x, y, x + colW, y + rowHeight, headerFill)
                    }
                    canvas.drawRect(x, y, x + colW, y + rowHeight, cellStroke)
                    canvas.save()
                    canvas.translate(x + 4f, y + 5f)
                    layout.draw(canvas)
                    canvas.restore()
                    x += colW
                }
                y += rowHeight
            }

            // Document Header matching handwritten logbook sheet
            canvas.drawText("BUKU JURNAL / TABEL PELAPORAN KEGIATAN SECURITY BPS", margin, y + 12f, titlePaint)
            y += 18f
            canvas.drawText(
                "Hari/Tanggal: ${sheet.formattedDisplayDate()}   |   Shift: ${sheet.shift}   |   No: ${sheet.docNumber.ifBlank { "-" }}",
                margin,
                y + 10f,
                metaPaint
            )
            y += 14f
            canvas.drawText("Penyusun: ${profile.name} (${profile.nrp}) — ${profile.role}", margin, y + 10f, metaPaint)
            y += 16f

            // Table 1: Personnel
            val pCols = floatArrayOf(
                tableWidth * 0.06f,
                tableWidth * 0.32f,
                tableWidth * 0.18f,
                tableWidth * 0.10f,
                tableWidth * 0.12f,
                tableWidth * 0.22f
            )
            drawRowCells(pCols, listOf("NO", "NAMA", "JABATAN", "PLOT", "H / T", "KET"), isHeader = true)
            if (sheet.personnelRows.isEmpty()) {
                drawRowCells(pCols, listOf("-", "Belum ada data personel", "-", "-", "-", "-"), isHeader = false)
            } else {
                sheet.personnelRows.forEachIndexed { idx, p ->
                    drawRowCells(
                        pCols,
                        listOf("${idx + 1}", p.nama, p.jabatan, p.plot, p.kehadiran, p.ket),
                        isHeader = false
                    )
                }
            }
            y += 10f

            // Table 2: Inventory
            val iCols = floatArrayOf(
                tableWidth * 0.06f,
                tableWidth * 0.36f,
                tableWidth * 0.15f,
                tableWidth * 0.14f,
                tableWidth * 0.29f
            )
            drawRowCells(iCols, listOf("NO", "INVENTARIS", "JUMLAH", "KONDISI", "KETERANGAN"), isHeader = true)
            if (sheet.inventoryRows.isEmpty()) {
                drawRowCells(iCols, listOf("-", "Belum ada data inventaris", "-", "-", "-"), isHeader = false)
            } else {
                sheet.inventoryRows.forEachIndexed { idx, inv ->
                    drawRowCells(
                        iCols,
                        listOf("${idx + 1}", inv.inventaris, inv.jumlah, inv.kondisi, inv.keterangan),
                        isHeader = false
                    )
                }
            }
            y += 10f

            // Table 3: Activity Log
            val aCols = floatArrayOf(
                tableWidth * 0.06f,
                tableWidth * 0.14f,
                tableWidth * 0.80f
            )
            drawRowCells(aCols, listOf("NO", "JAM", "URAIAN KEGIATAN"), isHeader = true)
            if (sheet.activityRows.isEmpty()) {
                drawRowCells(aCols, listOf("-", "-", "Belum ada uraian kegiatan"), isHeader = false)
            } else {
                sheet.activityRows.forEachIndexed { idx, act ->
                    drawRowCells(
                        aCols,
                        listOf("${idx + 1}", act.jam, act.uraian),
                        isHeader = false
                    )
                }
            }

            doc.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use { out ->
                doc.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.localizedMessage ?: "Gagal menulis PDF tabel.")
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

private class SecurityAttendancePrintAdapter(
    private val context: Context,
    private val sheet: AttendanceSheet,
    private val profile: OfficerProfile
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
        val info = PrintDocumentInfo.Builder("Absensi_4_Anggota_${sheet.shift.replace(" ", "_")}_BPS_${sheet.sheetDate}.pdf")
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
            callback.onWriteFailed("Dokumen PDF absensi gagal dibuat.")
            return
        }

        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 24f
            val tableWidth = pageWidth - margin * 2

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

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 8.8f
            }
            val headerFill = Paint().apply {
                color = Color.rgb(15, 23, 42)
                style = Paint.Style.FILL
            }
            val altFill = Paint().apply {
                color = Color.rgb(248, 250, 252)
                style = Paint.Style.FILL
            }
            val headerText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(251, 191, 36)
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 8f
            }
            val cellStroke = Paint().apply {
                color = Color.rgb(100, 116, 139)
                style = Paint.Style.STROKE
                strokeWidth = 0.7f
            }

            val hCount = sheet.rows.count { it.statusEnum == AttendanceStatus.HADIR }
            val iCount = sheet.rows.count { it.statusEnum == AttendanceStatus.IZIN }
            val sCount = sheet.rows.count { it.statusEnum == AttendanceStatus.SAKIT }
            val aCount = sheet.rows.count { it.statusEnum == AttendanceStatus.ALPHA }
            val lCount = sheet.rows.count { it.statusEnum == AttendanceStatus.LIBUR }

            canvas.drawText("DAFTAR ABSENSI ANGGOTA SECURITY - PLOT BPS", margin, y + 12f, titlePaint)
            y += 18f
            canvas.drawText(
                "Tanggal: ${sheet.formattedDisplayDate()}  |  Shift: ${sheet.shift}  |  Regu: ${sheet.periodLabel}",
                margin,
                y + 10f,
                metaPaint
            )
            y += 13f
            canvas.drawText(
                "Danru: ${sheet.commanderName}  |  Pelapor: ${profile.name} (${profile.nrp})",
                margin,
                y + 10f,
                metaPaint
            )
            y += 13f
            canvas.drawText(
                "Rekap: Total ${sheet.rows.size} Anggota  |  Hadir: $hCount  |  Izin: $iCount  |  Sakit: $sCount  |  Alpha: $aCount  |  Libur: $lCount",
                margin,
                y + 10f,
                metaPaint
            )
            y += 16f

            // Columns: NO | NAMA | NRP | JABATAN | PLOT | STATUS | MASUK | PULANG | TEREKAM | KET
            val cols = floatArrayOf(
                tableWidth * 0.05f,
                tableWidth * 0.19f,
                tableWidth * 0.12f,
                tableWidth * 0.11f,
                tableWidth * 0.12f,
                tableWidth * 0.08f,
                tableWidth * 0.07f,
                tableWidth * 0.07f,
                tableWidth * 0.07f,
                tableWidth * 0.12f
            )

            fun drawRow(values: List<String>, isHeader: Boolean, isAlt: Boolean) {
                val layouts = values.mapIndexed { idx, txt ->
                    val w = (cols[idx] - 6f).toInt().coerceAtLeast(8)
                    StaticLayout.Builder.obtain(
                        txt,
                        0,
                        txt.length,
                        if (isHeader) headerText else cellText,
                        w
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
                }
                val rowHeight = (layouts.maxOfOrNull { it.height } ?: 11) + 9f
                ensureSpace(rowHeight + 2f)

                var x = margin
                layouts.forEachIndexed { idx, layout ->
                    val colW = cols[idx]
                    if (isHeader) {
                        canvas.drawRect(x, y, x + colW, y + rowHeight, headerFill)
                    } else if (isAlt) {
                        canvas.drawRect(x, y, x + colW, y + rowHeight, altFill)
                    }
                    canvas.drawRect(x, y, x + colW, y + rowHeight, cellStroke)
                    canvas.save()
                    canvas.translate(x + 3f, y + 4.5f)
                    layout.draw(canvas)
                    canvas.restore()
                    x += colW
                }
                y += rowHeight
            }

            drawRow(
                listOf("NO", "NAMA ANGGOTA", "NRP / ID", "JABATAN", "PLOT", "STATUS", "MASUK", "PULANG", "REKAM", "KET"),
                isHeader = true,
                isAlt = false
            )

            sheet.rows.forEachIndexed { idx, r ->
                drawRow(
                    listOf(
                        (idx + 1).toString().padStart(2, '0'),
                        r.nama,
                        r.nrp,
                        r.jabatan,
                        r.plot,
                        "${r.statusEnum.code} (${r.statusEnum.label})",
                        r.jamMasuk.ifBlank { "-" },
                        r.jamPulang.ifBlank { "-" },
                        r.recordedAt.ifBlank { "-" },
                        r.keterangan.ifBlank { "-" }
                    ),
                    isHeader = false,
                    isAlt = idx % 2 == 1
                )
            }

            y += 22f
            ensureSpace(55f)
            canvas.drawText("Mengetahui,", margin + 20f, y, metaPaint)
            canvas.drawText("Danru / Petugas Absensi,", pageWidth - margin - 140f, y, metaPaint)
            y += 38f
            canvas.drawText("( Korlap Security BPS )", margin + 10f, y, metaPaint)
            canvas.drawText("( ${sheet.commanderName} )", pageWidth - margin - 140f, y, metaPaint)

            doc.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use { out ->
                doc.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.localizedMessage ?: "Gagal menulis PDF absensi.")
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

private class SecurityPjuPrintAdapter(
    private val context: Context,
    private val sheet: PjuChecklistSheet,
    private val profile: OfficerProfile
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
        val info = PrintDocumentInfo.Builder("Form_Checklist_PJU_${sheet.monthYear.replace(" ", "_")}.pdf")
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
            callback.onWriteFailed("Dokumen PDF PJU gagal dibuat.")
            return
        }

        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 24f
            val tableWidth = pageWidth - margin * 2

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

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 8.2f
            }
            val boldMetaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 8.2f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val cellBoldLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val slotTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 6.8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val strokePaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val thinStrokePaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }
            val headerBgPaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                style = Paint.Style.FILL
            }

            // Header matching paper form
            canvas.drawText("PERUMAHAN BUMI PAPAN SELARAS", margin, y + 11f, titlePaint)
            y += 14f
            canvas.drawText("FORM CHECKLIST PENERANGAN JALAN UMUM", margin, y + 11f, titlePaint)
            y += 14f
            canvas.drawText("BULAN ${sheet.monthYear.uppercase()}", margin, y + 10f, subHeaderPaint)
            y += 13f
            canvas.drawText("Dicetak oleh: ${profile.name} (${profile.nrp})  |  Periode Tgl ${sheet.periodStartDay}–${sheet.periodEndDay}", margin, y + 9f, metaPaint)
            y += 15f

            val days = sheet.daysList()
            val noColW = 26f
            val clusterColW = 110f
            val daysTotalW = tableWidth - noColW - clusterColW
            val dayColW = if (days.isNotEmpty()) daysTotalW / days.size else daysTotalW

            // Matrix Table Header (2 stacked rows on right side, merged on left)
            val h1 = 15f
            val h2 = 15f
            val headerTotalH = h1 + h2
            canvas.drawRect(margin, y, margin + tableWidth, y + headerTotalH, headerBgPaint)
            canvas.drawRect(margin, y, margin + noColW, y + headerTotalH, strokePaint)
            canvas.drawText("NO.", margin + noColW / 2f, y + 18f, cellHeaderPaint)

            canvas.drawRect(margin + noColW, y, margin + noColW + clusterColW, y + headerTotalH, strokePaint)
            canvas.drawText("CLUSTER", margin + noColW + clusterColW / 2f, y + 18f, cellHeaderPaint)

            val daysStartX = margin + noColW + clusterColW
            canvas.drawRect(daysStartX, y, margin + tableWidth, y + h1, strokePaint)
            canvas.drawText("KONDISI LAMPU", daysStartX + daysTotalW / 2f, y + 10.5f, cellHeaderPaint)

            canvas.drawRect(daysStartX, y + h1, margin + tableWidth, y + h1 + h2 * 0.55f, strokePaint)
            val tanggalBandY = y + h1
            val numBandH = h2
            canvas.drawRect(daysStartX, tanggalBandY, margin + tableWidth, tanggalBandY + numBandH, strokePaint)
            days.forEachIndexed { idx, day ->
                val dx = daysStartX + idx * dayColW
                canvas.drawRect(dx, tanggalBandY, dx + dayColW, tanggalBandY + numBandH, strokePaint)
                canvas.drawText(day.toString(), dx + dayColW / 2f, tanggalBandY + 10.5f, cellHeaderPaint)
            }
            y += headerTotalH

            val rowH = 36f
            val slotH = rowH / 4f
            sheet.clusters.forEachIndexed { idx, cluster ->
                ensureSpace(rowH + 4f)
                canvas.drawRect(margin, y, margin + noColW, y + rowH, strokePaint)
                canvas.drawText("${idx + 1}", margin + noColW / 2f, y + rowH / 2f + 3f, cellHeaderPaint)

                canvas.drawRect(margin + noColW, y, daysStartX, y + rowH, strokePaint)
                canvas.drawText(
                    cluster.clusterName.take(20),
                    margin + noColW + 4f,
                    y + rowH / 2f + 3f,
                    cellBoldLeftPaint
                )

                days.forEachIndexed { dIdx, day ->
                    val dx = daysStartX + dIdx * dayColW
                    val slots = cluster.slotsForDay(day)
                    canvas.drawRect(dx, y, dx + dayColW, y + rowH, strokePaint)
                    slots.forEachIndexed { sIdx, st ->
                        val sy = y + sIdx * slotH
                        if (sIdx > 0) {
                            canvas.drawLine(dx, sy, dx + dayColW, sy, thinStrokePaint)
                        }
                        if (st != PjuLampStatus.NONE) {
                            canvas.drawText(st.symbol, dx + dayColW / 2f, sy + slotH - 2f, slotTextPaint)
                        }
                    }
                }
                y += rowH
            }

            y += 10f

            // CATATAN box
            val notesStr = sheet.notes.ifBlank { "-" }
            val notesLayout = StaticLayout.Builder.obtain(
                notesStr,
                0,
                notesStr.length,
                metaPaint,
                (tableWidth - 12f).toInt()
            ).build()
            val notesBoxH = (notesLayout.height + 24f).coerceAtLeast(52f)
            ensureSpace(notesBoxH + 10f)
            canvas.drawRect(margin, y, margin + tableWidth, y + notesBoxH, strokePaint)
            canvas.drawText("CATATAN :", margin + 6f, y + 12f, boldMetaPaint)
            canvas.save()
            canvas.translate(margin + 6f, y + 18f)
            notesLayout.draw(canvas)
            canvas.restore()
            y += notesBoxH + 12f

            // Permasalahan PJU if present
            if (sheet.issues.isNotEmpty()) {
                ensureSpace(32f)
                canvas.drawText("DAFTAR PERMASALAHAN PJU (${sheet.issues.size}):", margin, y + 10f, boldMetaPaint)
                y += 15f
                sheet.issues.forEachIndexed { idx, issue ->
                    val line = "${idx + 1}. [${issue.tanggal}] ${issue.cluster} (${issue.titikLampu}): ${issue.keluhan} | Status: ${issue.status}" +
                        if (issue.tindakLanjut.isNotBlank()) " | TL: ${issue.tindakLanjut}" else ""
                    val layout = StaticLayout.Builder.obtain(line, 0, line.length, metaPaint, tableWidth.toInt()).build()
                    ensureSpace(layout.height + 6f)
                    canvas.save()
                    canvas.translate(margin, y)
                    layout.draw(canvas)
                    canvas.restore()
                    y += layout.height + 5f
                }
                y += 8f
            }

            // KETERANGAN & PENANGGUNG JAWAB footer matching form verbatim
            ensureSpace(74f)
            canvas.drawText("KETERANGAN :", margin, y + 10f, boldMetaPaint)
            y += 13f
            canvas.drawText("1.   WAJIB MELAKUKAN CHECKLIST PADA SEMUA PJU MAKSIMAL 2 HARI SEKALI", margin, y + 10f, boldMetaPaint)
            y += 12f
            canvas.drawText("2.   APABILA PADA SAAT PENGECEKAN TERDAPAT MASALAH, MAKA WAJIB MENGISI PERMASALAHAN PJU PADA FORM YANG TERSEDIA", margin, y + 10f, boldMetaPaint)
            y += 18f
            canvas.drawText("Penanggung Jawab", margin, y + 10f, boldMetaPaint)
            y += 26f
            canvas.drawText(sheet.responsibleNames, margin, y + 10f, boldMetaPaint)

            doc.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use { out ->
                doc.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.localizedMessage ?: "Gagal menulis PDF PJU.")
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
