package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AttendanceMemberRow
import com.example.data.model.AttendanceSheet
import com.example.data.model.AttendanceStatus
import com.example.data.model.ShiftOption
import com.example.data.model.TimeKeywordFilter
import com.example.data.model.UiState
import com.example.data.model.currentIsoDate
import com.example.data.model.parseIsoDateTriple
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber800
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy50
import com.example.ui.theme.Navy100
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Red800
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val COL_NO = 44.dp
private val COL_NAMA = 160.dp
private val COL_NRP = 112.dp
private val COL_JABATAN = 98.dp
private val COL_PLOT = 116.dp
private val COL_STATUS = 96.dp
private val COL_MASUK = 72.dp
private val COL_PULANG = 72.dp
private val COL_TEREKAM = 82.dp
private val COL_KET = 156.dp
private val COL_AKSI = 88.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceTabScreen(
    sheetsState: UiState<List<AttendanceSheet>>,
    selectedSheetId: String?,
    attendanceFilter: TimeKeywordFilter,
    defaultShift: String,
    defaultCommanderName: String,
    onSelectSheet: (String) -> Unit,
    onCreateSheet: (sheetDate: String, shift: String, periodLabel: String, commanderName: String, copyExisting: Boolean) -> Unit,
    onUpdateHeader: (sheetDate: String, shift: String, periodLabel: String, commanderName: String) -> Unit,
    onUpsertRow: (editIndex: Int?, row: AttendanceMemberRow) -> Unit,
    onQuickCycleStatus: (rowIndex: Int) -> Unit,
    onMarkAllStatus: (statusCode: String) -> Unit,
    onFillShiftTimes: () -> Unit,
    onEnsure15Rows: () -> Unit,
    onDeleteRow: (rowIndex: Int) -> Unit,
    onDeleteSheet: () -> Unit,
    onCopyWhatsApp: (AttendanceSheet) -> Unit,
    onPrintPdf: (AttendanceSheet) -> Unit,
    onFilterKeywordChange: (String) -> Unit,
    onFilterHourChange: (Int?) -> Unit,
    onFilterDayChange: (Int?) -> Unit,
    onFilterMonthChange: (Int?) -> Unit,
    onFilterYearChange: (Int?) -> Unit,
    onFilterToday: () -> Unit,
    onResetFilter: () -> Unit
) {
    val allSheets = (sheetsState as? UiState.Success)?.data.orEmpty()
    val filteredSheets = remember(allSheets, attendanceFilter) {
        if (!attendanceFilter.isActive) allSheets else allSheets.filter { attendanceFilter.matchesAttendanceSheet(it) }
    }
    val activeSheet = remember(filteredSheets, allSheets, selectedSheetId) {
        val pool = if (attendanceFilter.isActive) filteredSheets else allSheets
        pool.firstOrNull { it.id == selectedSheetId } ?: pool.firstOrNull()
    }
    val availableYears = remember(allSheets) {
        allSheets.mapNotNull { parseIsoDateTriple(it.sheetDate)?.first }.distinct()
    }

    var showSearchPanel by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditHeaderDialog by remember { mutableStateOf(false) }
    var editingRowPair by remember { mutableStateOf<Pair<Int?, AttendanceMemberRow>?>(null) }
    var showDeleteSheetConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (sheetsState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    color = Gold400,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Error -> {
                Surface(
                    color = Color.White.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Text(
                        text = sheetsState.message,
                        color = Red800,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Card: Sheet Selector + Search Toggle + Export Tools
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.90f)),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Navy900),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = Gold400)
                                        }
                                        Column {
                                            Text(
                                                text = "Tabel Absensi 15 Anggota",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Navy900,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "Rekap kehadiran terekam & siap cetak PDF",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Slate500
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { showSearchPanel = !showSearchPanel },
                                            modifier = Modifier.testTag("toggle_attendance_search")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Cari & Saring Absensi",
                                                tint = if (showSearchPanel || attendanceFilter.isActive) Gold500 else Navy800
                                            )
                                        }
                                        Button(
                                            onClick = { showCreateDialog = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Navy900,
                                                contentColor = Gold400
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("create_attendance_sheet_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Lembar Baru", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                // Saved Sheet Selector Strip
                                if (filteredSheets.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(filteredSheets, key = { it.id }) { sheet ->
                                            val isSelected = sheet.id == activeSheet?.id
                                            Surface(
                                                onClick = { onSelectSheet(sheet.id) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) Navy900 else Slate100,
                                                border = BorderStroke(1.dp, if (isSelected) Gold500 else Slate200)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                ) {
                                                    Text(
                                                        text = "${sheet.sheetDate} • ${sheet.shift}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Gold400 else Navy900
                                                    )
                                                    Text(
                                                        text = "${sheet.rows.size} Anggota • ${sheet.periodLabel}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) Color.White else Slate500,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Action Buttons for Active Sheet
                                if (activeSheet != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onCopyWhatsApp(activeSheet) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("copy_attendance_wa_button")
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = Emerald600)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Salin WA", style = MaterialTheme.typography.labelSmall, color = Navy900)
                                        }
                                        Button(
                                            onClick = { onPrintPdf(activeSheet) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("print_attendance_pdf_button")
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("PDF Tabel", style = MaterialTheme.typography.labelSmall)
                                        }
                                        if (allSheets.size > 1) {
                                            IconButton(
                                                onClick = { showDeleteSheetConfirm = true },
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Red100)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Hapus Lembar", tint = Red600, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Collapsible Search & Date/Time Filter Panel
                    item {
                        AnimatedVisibility(visible = showSearchPanel) {
                            SearchAndTimeFilterPanel(
                                title = "Cari Anggota / Status / Tanggal Absensi",
                                filter = attendanceFilter,
                                availableYears = availableYears,
                                totalCount = allSheets.size,
                                filteredCount = filteredSheets.size,
                                testTagPrefix = "attendance",
                                onKeywordChange = onFilterKeywordChange,
                                onHourChange = onFilterHourChange,
                                onDayChange = onFilterDayChange,
                                onMonthChange = onFilterMonthChange,
                                onYearChange = onFilterYearChange,
                                onSelectToday = onFilterToday,
                                onReset = onResetFilter
                            )
                        }
                    }

                    if (activeSheet == null) {
                        item {
                            Surface(
                                color = Color.White.copy(alpha = 0.90f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (attendanceFilter.isActive) {
                                            "Tidak ada lembar absensi yang cocok dengan filter."
                                        } else {
                                            "Belum ada lembar absensi tersimpan."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate700,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { showCreateDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
                                    ) {
                                        Text("+ Buat Tabel Absensi 15 Anggota")
                                    }
                                }
                            }
                        }
                    } else {
                        val visibleIndexedRows = activeSheet.rows.mapIndexed { idx, row -> idx to row }
                            .filter { (_, row) ->
                                !attendanceFilter.isActive || attendanceFilter.matchesAttendanceRow(row, activeSheet.sheetDate)
                            }

                        // Metadata Banner + KPI Attendance Summary
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.91f)),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activeSheet.formattedDisplayDate(),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Navy900
                                            )
                                            Text(
                                                text = "${activeSheet.shift} • ${activeSheet.periodLabel} • Danru: ${activeSheet.commanderName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Slate500
                                            )
                                        }
                                        IconButton(onClick = { showEditHeaderDialog = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Info Lembar", tint = Navy800)
                                        }
                                    }

                                    // 6-Pill Attendance Summary
                                    val hCount = activeSheet.rows.count { it.statusEnum == AttendanceStatus.HADIR }
                                    val iCount = activeSheet.rows.count { it.statusEnum == AttendanceStatus.IZIN }
                                    val sCount = activeSheet.rows.count { it.statusEnum == AttendanceStatus.SAKIT }
                                    val aCount = activeSheet.rows.count { it.statusEnum == AttendanceStatus.ALPHA }
                                    val lCount = activeSheet.rows.count { it.statusEnum == AttendanceStatus.LIBUR }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SummaryCountChip("Total: ${activeSheet.rows.size}", Navy900, Gold400)
                                        SummaryCountChip("Hadir (H): $hCount", Emerald100, Emerald800)
                                        SummaryCountChip("Izin (I): $iCount", Navy100, Navy800)
                                        SummaryCountChip("Sakit (S): $sCount", Amber100, Amber800)
                                        SummaryCountChip("Alpha (A): $aCount", Red100, Red800)
                                        SummaryCountChip("Libur (L): $lCount", Slate100, Slate700)
                                    }

                                    // Quick Batch Recording Buttons
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onMarkAllStatus(AttendanceStatus.HADIR.code) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("mark_all_hadir_button")
                                        ) {
                                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(15.dp), tint = Emerald600)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Semua Hadir (H)", style = MaterialTheme.typography.labelSmall, color = Navy900)
                                        }
                                        OutlinedButton(
                                            onClick = onFillShiftTimes,
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp), tint = Navy800)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Samakan Jam Shift", style = MaterialTheme.typography.labelSmall, color = Navy900)
                                        }
                                        if (activeSheet.rows.size < 15) {
                                            OutlinedButton(
                                                onClick = onEnsure15Rows,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(15.dp), tint = Navy800)
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text("Lengkapi 15 Anggota", style = MaterialTheme.typography.labelSmall, color = Navy900)
                                            }
                                        } else if (activeSheet.rows.size < 25) {
                                            OutlinedButton(
                                                onClick = {
                                                    val num = activeSheet.rows.size + 1
                                                    editingRowPair = null to AttendanceMemberRow(
                                                        nama = "Anggota Security ${num.toString().padStart(2, '0')}",
                                                        nrp = "SEC-BPS-${num.toString().padStart(3, '0')}",
                                                        jabatan = "ANGGOTA",
                                                        plot = "Plot BPS",
                                                        status = AttendanceStatus.HADIR.code
                                                    )
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = Navy800)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Tambah Baris", style = MaterialTheme.typography.labelSmall, color = Navy900)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Tip: Ketuk badge Status (H/I/S/A/L) untuk ganti cepat & rekam waktu otomatis, atau ketuk baris untuk edit lengkap.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate500
                                    )
                                }
                            }
                        }

                        // THE NEAT BORDERED 15-MEMBER ATTENDANCE TABLE
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                                border = BorderStroke(1.5.dp, Navy800)
                            ) {
                                val hScroll = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(hScroll)
                                ) {
                                    // Formal Navy Header Row
                                    Row(
                                        modifier = Modifier
                                            .background(Navy900)
                                            .height(44.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HeaderGridCell("NO", COL_NO, TextAlign.Center)
                                        GridDivider(Color.White.copy(alpha = 0.25f))
                                        HeaderGridCell("NAMA ANGGOTA", COL_NAMA, TextAlign.Start)
                                        GridDivider(Color.White.copy(alpha = 0.25f))
                                        HeaderGridCell("NRP / ID", COL_NRP, TextAlign.Start)
                                        GridDivider(Color.White.copy(alpha = 0.25f))
                                        HeaderGridCell("JABATAN", COL_JABATAN, TextAlign.Start)
                                        GridDivider(Color.White.copy(alpha = 0.25f))
                                        HeaderGridCell("PLOT", COL_PLOT, TextAlign.Start)
                                        GridDivider(Color.White.copy(alpha = 0.25f))
                                        HeaderGridCell("STATUS", COL_STATUS, TextAlign.Center)
                                        GridDivider(Color.White.copy(alpha = 0.25f))
                                        HeaderGridCell("MASUK", COL_MASUK, TextAlign.Center)
                                        GridDivider(Color.White.copy(alpha = 0.25f))
                                        HeaderGridCell("PULANG", COL_PULANG, TextAlign.Center)
                                        GridDivider(Color.White.copy(alpha = 0.25f))
                                        HeaderGridCell("TEREKAM", COL_TEREKAM, TextAlign.Center)
                                        GridDivider(Color.White.copy(alpha = 0.25f))
                                        HeaderGridCell("KETERANGAN", COL_KET, TextAlign.Start)
                                        GridDivider(Color.White.copy(alpha = 0.25f))
                                        HeaderGridCell("AKSI", COL_AKSI, TextAlign.Center)
                                    }
                                    HorizontalDivider(thickness = 1.5.dp, color = Gold500)

                                    if (visibleIndexedRows.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Tidak ada anggota yang cocok dengan pencarian.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Slate500
                                            )
                                        }
                                    } else {
                                        visibleIndexedRows.forEachIndexed { displayPos, (originalIdx, row) ->
                                            val rowBg = if (displayPos % 2 == 0) Color.White else Slate50
                                            AttendanceGridRow(
                                                rowNumber = originalIdx + 1,
                                                row = row,
                                                background = rowBg,
                                                onQuickCycleStatus = { onQuickCycleStatus(originalIdx) },
                                                onEditRow = { editingRowPair = originalIdx to row },
                                                onDeleteRow = { onDeleteRow(originalIdx) }
                                            )
                                            if (displayPos < visibleIndexedRows.lastIndex) {
                                                HorizontalDivider(color = Slate200)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Legend footer card
                        item {
                            Surface(
                                color = Color.White.copy(alpha = 0.88f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Keterangan Kode: H = Hadir • I = Izin • S = Sakit • A = Alpha • L = Libur / Off Duty. Semua perubahan otomatis tersimpan ke cloud Firestore.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate700,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AttendanceHeaderDialog(
            title = "Buat Lembar Absensi 15 Anggota",
            initialDate = currentIsoDate(),
            initialShift = defaultShift,
            initialPeriod = "Regu Plot BPS (15 Anggota)",
            initialCommander = defaultCommanderName,
            showCopyNamesToggle = activeSheet != null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { date, shift, period, commander, copyNames ->
                onCreateSheet(date, shift, period, commander, copyNames)
                showCreateDialog = false
            }
        )
    }

    if (showEditHeaderDialog && activeSheet != null) {
        AttendanceHeaderDialog(
            title = "Edit Informasi Lembar Absensi",
            initialDate = activeSheet.sheetDate,
            initialShift = activeSheet.shift,
            initialPeriod = activeSheet.periodLabel,
            initialCommander = activeSheet.commanderName,
            showCopyNamesToggle = false,
            onDismiss = { showEditHeaderDialog = false },
            onConfirm = { date, shift, period, commander, _ ->
                onUpdateHeader(date, shift, period, commander)
                showEditHeaderDialog = false
            }
        )
    }

    editingRowPair?.let { (editIdx, row) ->
        EditAttendanceRowDialog(
            rowNumber = (editIdx ?: (activeSheet?.rows?.size ?: 0)) + 1,
            initialRow = row,
            onDismiss = { editingRowPair = null },
            onSave = { updated ->
                onUpsertRow(editIdx, updated)
                editingRowPair = null
            }
        )
    }

    if (showDeleteSheetConfirm && activeSheet != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSheetConfirm = false },
            title = { Text("Hapus Lembar Absensi?", style = MaterialTheme.typography.titleMedium, color = Navy900) },
            text = {
                Text(
                    text = "Lembar absensi tanggal ${activeSheet.sheetDate} (${activeSheet.shift}) akan dihapus permanen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSheet()
                        showDeleteSheetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteSheetConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SummaryCountChip(text: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.25f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HeaderGridCell(text: String, width: Dp, align: TextAlign) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 8.dp),
        contentAlignment = if (align == TextAlign.Center) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Gold400,
            textAlign = align,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GridDivider(color: Color = Slate200) {
    VerticalDivider(
        thickness = 1.dp,
        color = color,
        modifier = Modifier.height(44.dp)
    )
}

@Composable
private fun AttendanceGridRow(
    rowNumber: Int,
    row: AttendanceMemberRow,
    background: Color,
    onQuickCycleStatus: () -> Unit,
    onEditRow: () -> Unit,
    onDeleteRow: () -> Unit
) {
    val status = row.statusEnum
    val (statusBg, statusFg) = when (status) {
        AttendanceStatus.HADIR -> Emerald100 to Emerald800
        AttendanceStatus.IZIN -> Navy100 to Navy800
        AttendanceStatus.SAKIT -> Amber100 to Amber800
        AttendanceStatus.ALPHA -> Red100 to Red800
        AttendanceStatus.LIBUR -> Slate200 to Slate700
    }

    Row(
        modifier = Modifier
            .background(background)
            .clickable(onClick = onEditRow)
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(COL_NO), contentAlignment = Alignment.Center) {
            Text(
                text = rowNumber.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Navy900
            )
        }
        GridDivider()
        DataGridCell(row.nama, COL_NAMA, fontWeight = FontWeight.Bold, color = Navy900)
        GridDivider()
        DataGridCell(row.nrp, COL_NRP, color = Slate700)
        GridDivider()
        DataGridCell(row.jabatan, COL_JABATAN, color = Navy800)
        GridDivider()
        DataGridCell(row.plot, COL_PLOT, color = Slate700)
        GridDivider()
        Box(
            modifier = Modifier
                .width(COL_STATUS)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = onQuickCycleStatus,
                color = statusBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, statusFg.copy(alpha = 0.4f)),
                modifier = Modifier.testTag("attendance_status_chip_$rowNumber")
            ) {
                Text(
                    text = "${status.code} • ${status.label}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusFg,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                )
            }
        }
        GridDivider()
        DataGridCell(row.jamMasuk.ifBlank { "-" }, COL_MASUK, align = TextAlign.Center)
        GridDivider()
        DataGridCell(row.jamPulang.ifBlank { "-" }, COL_PULANG, align = TextAlign.Center)
        GridDivider()
        DataGridCell(
            text = row.recordedAt.ifBlank { "-" },
            width = COL_TEREKAM,
            align = TextAlign.Center,
            color = Emerald800,
            fontWeight = FontWeight.SemiBold
        )
        GridDivider()
        DataGridCell(row.keterangan.ifBlank { "-" }, COL_KET, color = Slate500)
        GridDivider()
        Row(
            modifier = Modifier.width(COL_AKSI),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEditRow, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Anggota $rowNumber", tint = Navy800, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDeleteRow, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus Baris $rowNumber", tint = Red600, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun DataGridCell(
    text: String,
    width: Dp,
    align: TextAlign = TextAlign.Start,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Slate800
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 8.dp),
        contentAlignment = if (align == TextAlign.Center) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = fontWeight,
            color = color,
            textAlign = align,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AttendanceHeaderDialog(
    title: String,
    initialDate: String,
    initialShift: String,
    initialPeriod: String,
    initialCommander: String,
    showCopyNamesToggle: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (date: String, shift: String, period: String, commander: String, copyExisting: Boolean) -> Unit
) {
    var sheetDate by remember { mutableStateOf(initialDate) }
    var selectedShift by remember { mutableStateOf(initialShift) }
    var periodLabel by remember { mutableStateOf(initialPeriod) }
    var commanderName by remember { mutableStateOf(initialCommander) }
    var copyNames by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy900)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = Gold400, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = sheetDate,
                        onValueChange = { if (it.length <= 10) sheetDate = it },
                        label = { Text("Tanggal (yyyy-MM-dd)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Shift Tugas", style = MaterialTheme.typography.labelMedium, color = Navy900)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        ShiftOption.entries.forEach { shift ->
                            val active = selectedShift == shift.label
                            Surface(
                                onClick = { selectedShift = shift.label },
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) Navy900 else Slate100,
                                border = BorderStroke(1.dp, if (active) Gold500 else Slate200),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = shift.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) Gold400 else Slate700,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = periodLabel,
                        onValueChange = { if (it.length <= 60) periodLabel = it },
                        label = { Text("Nama Regu / Periode") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = commanderName,
                        onValueChange = { if (it.length <= 80) commanderName = it },
                        label = { Text("Danru / Penanggung Jawab") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (showCopyNamesToggle) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { copyNames = !copyNames }
                        ) {
                            Checkbox(checked = copyNames, onCheckedChange = { copyNames = it })
                            Text(
                                text = "Salin daftar nama 15 anggota dari lembar aktif saat ini",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate700
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onConfirm(sheetDate, selectedShift, periodLabel, commanderName, copyNames) },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditAttendanceRowDialog(
    rowNumber: Int,
    initialRow: AttendanceMemberRow,
    onDismiss: () -> Unit,
    onSave: (AttendanceMemberRow) -> Unit
) {
    var nama by remember(initialRow) { mutableStateOf(initialRow.nama) }
    var nrp by remember(initialRow) { mutableStateOf(initialRow.nrp) }
    var jabatan by remember(initialRow) { mutableStateOf(initialRow.jabatan) }
    var plot by remember(initialRow) { mutableStateOf(initialRow.plot) }
    var statusCode by remember(initialRow) { mutableStateOf(initialRow.statusEnum.code) }
    var jamMasuk by remember(initialRow) { mutableStateOf(initialRow.jamMasuk) }
    var jamPulang by remember(initialRow) { mutableStateOf(initialRow.jamPulang) }
    var recordedAt by remember(initialRow) {
        mutableStateOf(
            initialRow.recordedAt.ifBlank {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            }
        )
    }
    var keterangan by remember(initialRow) { mutableStateOf(initialRow.keterangan) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy900)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Gold400)
                        Text(
                            text = "Absensi Anggota #${rowNumber.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nama,
                        onValueChange = { if (it.length <= 80) nama = it },
                        label = { Text("Nama Anggota") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attendance_row_name_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = nrp,
                            onValueChange = { if (it.length <= 40) nrp = it },
                            label = { Text("NRP / ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = jabatan,
                            onValueChange = { if (it.length <= 40) jabatan = it },
                            label = { Text("Jabatan") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = plot,
                        onValueChange = { if (it.length <= 30) plot = it },
                        label = { Text("Plot Tugas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Status Kehadiran", style = MaterialTheme.typography.labelMedium, color = Navy900)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttendanceStatus.entries.forEach { st ->
                            val selected = statusCode == st.code
                            Surface(
                                onClick = {
                                    statusCode = st.code
                                    recordedAt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) Navy900 else Slate100,
                                border = BorderStroke(1.dp, if (selected) Gold500 else Slate200)
                            ) {
                                Text(
                                    text = "${st.code} (${st.label})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Gold400 else Slate700,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = jamMasuk,
                            onValueChange = { if (it.length <= 16) jamMasuk = it },
                            label = { Text("Masuk") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = jamPulang,
                            onValueChange = { if (it.length <= 16) jamPulang = it },
                            label = { Text("Pulang") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = recordedAt,
                            onValueChange = { if (it.length <= 24) recordedAt = it },
                            label = { Text("Terekam") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = keterangan,
                        onValueChange = { if (it.length <= 150) keterangan = it },
                        label = { Text("Keterangan (opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nama.isNotBlank()) {
                                    onSave(
                                        AttendanceMemberRow(
                                            nama = nama.trim(),
                                            nrp = nrp.trim(),
                                            jabatan = jabatan.trim(),
                                            plot = plot.trim(),
                                            status = statusCode,
                                            jamMasuk = jamMasuk.trim(),
                                            jamPulang = jamPulang.trim(),
                                            recordedAt = recordedAt.trim(),
                                            keterangan = keterangan.trim()
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400),
                            modifier = Modifier.testTag("save_attendance_row_button")
                        ) {
                            Text("Simpan & Rekam")
                        }
                    }
                }
            }
        }
    }
}
