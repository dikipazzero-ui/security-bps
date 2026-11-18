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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.IndonesianMonths
import com.example.data.model.PjuChecklistSheet
import com.example.data.model.PjuClusterRow
import com.example.data.model.PjuComplianceLevel
import com.example.data.model.PjuIssueEntry
import com.example.data.model.PjuLampStatus
import com.example.data.model.PjuTemplates
import com.example.data.model.ShiftOption
import com.example.data.model.TimeKeywordFilter
import com.example.data.model.UiState
import com.example.data.model.currentIsoDate
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber800
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold600
import com.example.ui.theme.Navy50
import com.example.ui.theme.Navy100
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Red800
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import java.util.Calendar

private enum class PjuSubOption(val label: String) {
    MATRIKS("Matriks Tabel PJU"),
    REKAP("Rekap Berkala"),
    MASALAH("Masalah & Catatan")
}

private data class PjuCellEditTarget(
    val clusterIndex: Int,
    val clusterName: String,
    val day: Int,
    val slots: List<PjuLampStatus>
)

private val PJU_COL_NO = 38.dp
private val PJU_COL_CLUSTER = 144.dp
private val PJU_COL_DAY = 38.dp
private val PJU_ROW_HEIGHT = 48.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PjuTabScreen(
    sheetsState: UiState<List<PjuChecklistSheet>>,
    selectedSheetId: String?,
    pjuFilter: TimeKeywordFilter,
    activeShift: String,
    selectedPjuShift: String,
    selectedPjuTime: String,
    currentUserId: String = "",
    isRoomCreator: Boolean = false,
    isExplicitSaving: Boolean,
    onSelectSheet: (String) -> Unit,
    onSelectPjuShift: (String) -> Unit,
    onPjuTimeChange: (String) -> Unit,
    onSetPjuTimeNow: () -> Unit,
    onCreateSheet: (month: Int, year: Int, startDay: Int, endDay: Int, responsibleNames: String, copyClusters: Boolean) -> Unit,
    onUpdateHeader: (month: Int, year: Int, startDay: Int, endDay: Int, responsibleNames: String) -> Unit,
    onSetCellSlots: (clusterIndex: Int, day: Int, slots: List<PjuLampStatus>) -> Unit,
    onMarkDayAllBaik: (day: Int) -> Unit,
    onUpsertCluster: (editIndex: Int?, clusterName: String) -> Unit,
    onDeleteCluster: (index: Int) -> Unit,
    onUpsertIssue: (editIndex: Int?, issue: PjuIssueEntry) -> Unit,
    onDeleteIssue: (index: Int) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onDeleteSheet: () -> Unit,
    onExplicitSave: (latestNotesDraft: String) -> Unit,
    onCopyWhatsApp: (PjuChecklistSheet) -> Unit,
    onPrintPdf: (PjuChecklistSheet) -> Unit,
    onFilterKeywordChange: (String) -> Unit,
    onFilterHourChange: (Int?) -> Unit,
    onFilterDayChange: (Int?) -> Unit,
    onFilterMonthChange: (Int?) -> Unit,
    onFilterYearChange: (Int?) -> Unit,
    onFilterShiftChange: (String?) -> Unit,
    onFilterCurrentMonth: () -> Unit,
    onResetFilter: () -> Unit,
    onOpenGlobalRecap: () -> Unit,
    totalRecapCount: Int
) {
    val allSheets = (sheetsState as? UiState.Success)?.data.orEmpty()
    val filteredSheets = remember(allSheets, pjuFilter) {
        if (!pjuFilter.isActive) allSheets else allSheets.filter { pjuFilter.matchesPjuSheet(it) }
    }
    val activeSheet = remember(filteredSheets, allSheets, selectedSheetId) {
        val pool = if (pjuFilter.isActive) filteredSheets else allSheets
        pool.firstOrNull { it.id == selectedSheetId } ?: pool.firstOrNull()
    }
    val availableYears = remember(allSheets) {
        allSheets.map { it.sheetYear.toInt() }.distinct()
    }

    var notesDraft by remember(activeSheet?.id, activeSheet?.notes) {
        mutableStateOf(activeSheet?.notes.orEmpty())
    }
    var subOption by remember { mutableStateOf(PjuSubOption.MATRIKS) }
    var showSearchPanel by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditHeaderDialog by remember { mutableStateOf(false) }
    var showDeleteSheetConfirm by remember { mutableStateOf(false) }
    var cellEditTarget by remember { mutableStateOf<PjuCellEditTarget?>(null) }
    var editingClusterPair by remember { mutableStateOf<Pair<Int?, String>?>(null) }
    var editingIssuePair by remember { mutableStateOf<Pair<Int?, PjuIssueEntry>?>(null) }
    var quickMarkDayDialog by remember { mutableStateOf(false) }

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
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. DIGITAL HEADER & SHEET SWITCHER CARD
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
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
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = Gold400
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "PERUMAHAN BUMI PAPAN SELARAS",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Slate500,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Checklist PJU • ${activeSheet?.monthYear ?: "Baru"}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Navy900,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = { showSearchPanel = !showSearchPanel },
                                            modifier = Modifier.testTag("toggle_pju_search")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Cari & Saring PJU",
                                                tint = if (showSearchPanel || pjuFilter.isActive) Gold500 else Navy800
                                            )
                                        }
                                        if (activeSheet != null) {
                                            IconButton(
                                                onClick = { onCopyWhatsApp(activeSheet) },
                                                modifier = Modifier.testTag("pju_copy_wa_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Salin Rekap WA PJU",
                                                    tint = Navy800
                                                )
                                            }
                                            IconButton(
                                                onClick = { onPrintPdf(activeSheet) },
                                                modifier = Modifier.testTag("pju_print_pdf_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PictureAsPdf,
                                                    contentDescription = "Cetak / PDF Checklist PJU",
                                                    tint = Navy800
                                                )
                                            }
                                        }
                                    }
                                }

                                // Horizontally scrollable period sheet selector + Add Sheet
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    item {
                                        Surface(
                                            onClick = { showCreateDialog = true },
                                            color = Gold500,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("create_pju_sheet_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = Navy950,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Periode Baru",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Navy950,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                    val displayList = if (pjuFilter.isActive) filteredSheets else allSheets
                                    items(displayList, key = { it.id }) { sheet ->
                                        val isSelected = activeSheet?.id == sheet.id
                                        Surface(
                                            onClick = { onSelectSheet(sheet.id) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Navy900 else Slate100,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) Gold500 else Slate200
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Text(
                                                    text = "${sheet.monthYear} (${sheet.periodStartDay}–${sheet.periodEndDay})",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Gold400 else Navy900
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. SEARCH & PERIODIC FILTER PANEL
                    item {
                        AnimatedVisibility(visible = showSearchPanel) {
                            SearchAndTimeFilterPanel(
                                title = "Cari & Saring PJU (Cluster / Jam / Bulan / Tahun / Shift)",
                                filter = pjuFilter,
                                availableYears = availableYears,
                                totalCount = allSheets.size,
                                filteredCount = filteredSheets.size,
                                testTagPrefix = "pju",
                                onKeywordChange = onFilterKeywordChange,
                                onHourChange = onFilterHourChange,
                                onDayChange = onFilterDayChange,
                                onMonthChange = onFilterMonthChange,
                                onYearChange = onFilterYearChange,
                                onShiftChange = onFilterShiftChange,
                                onSelectToday = onFilterCurrentMonth,
                                onReset = onResetFilter,
                                onOpenGlobalRecap = onOpenGlobalRecap,
                                globalRecapCount = totalRecapCount
                            )
                        }
                    }

                    if (activeSheet == null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.90f),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Gold500, modifier = Modifier.size(36.dp))
                                    Text(
                                        text = if (pjuFilter.isActive) "Tidak ada lembar PJU yang cocok dengan filter." else "Belum ada lembar checklist PJU.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate700,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(onClick = { showCreateDialog = true }) {
                                        Text("+ Buat Checklist PJU Baru")
                                    }
                                }
                            }
                        }
                    } else {
                        // 3. PERIODIC COMPLIANCE & LIVE KPI STATUS CARD
                        item {
                            val compliance = remember(activeSheet) { activeSheet.complianceInfo() }
                            val counts = remember(activeSheet) { activeSheet.countSlotsByStatus() }
                            val (bannerBg, bannerBorder, bannerTextColor) = when (compliance.level) {
                                PjuComplianceLevel.COMPLIANT -> Triple(Emerald100, Emerald600, Emerald800)
                                PjuComplianceLevel.DUE_SOON -> Triple(Amber100, Gold600, Amber800)
                                PjuComplianceLevel.OVERDUE -> Triple(Red100, Red600, Red800)
                                PjuComplianceLevel.EMPTY -> Triple(Navy50, Navy700, Navy900)
                            }
                            val todayCal = remember { Calendar.getInstance() }
                            val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
                            val targetQuickDay = if (todayDay in activeSheet.periodStartDay..activeSheet.periodEndDay) {
                                todayDay
                            } else {
                                activeSheet.periodStartDay.toInt()
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Navy900.copy(alpha = 0.94f)),
                                border = BorderStroke(1.dp, Gold500.copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Rule 2-day compliance bar
                                    Surface(
                                        color = bannerBg,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, bannerBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (compliance.level == PjuComplianceLevel.OVERDUE) {
                                                    Icons.Default.Warning
                                                } else {
                                                    Icons.Default.CheckCircle
                                                },
                                                contentDescription = null,
                                                tint = bannerTextColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = compliance.badgeText,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = bannerTextColor,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = compliance.detailText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = bannerTextColor
                                                )
                                            }
                                            Surface(
                                                onClick = { showEditHeaderDialog = true },
                                                color = Color.White.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Navy900, modifier = Modifier.size(13.dp))
                                                    Text("Edit", style = MaterialTheme.typography.labelSmall, color = Navy900, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    // Digital KPI Chips
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PjuKpiChip("Cluster", "${activeSheet.clusters.size}", Gold500, Navy950)
                                        PjuKpiChip("✓ Nyala", "${counts[PjuLampStatus.BAIK] ?: 0}", Emerald100, Emerald800)
                                        PjuKpiChip("! Redup", "${counts[PjuLampStatus.REDUP] ?: 0}", Amber100, Amber800)
                                        PjuKpiChip("✕ Mati", "${counts[PjuLampStatus.MATI] ?: 0}", Red100, Red800)
                                        PjuKpiChip("Masalah", "${activeSheet.issues.size}", if (activeSheet.issues.isNotEmpty()) Red600 else Navy700, Color.White)
                                    }

                                    // Quick 1-Tap Action Row + PJ info
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            onClick = { quickMarkDayDialog = true },
                                            color = Gold500,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("pju_quick_mark_day_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = Navy950, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "Tandai Semua Nyala (Tgl $targetQuickDay)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Navy950,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Surface(
                                            onClick = {
                                                editingIssuePair = null to PjuIssueEntry(
                                                    tanggal = currentIsoDate(),
                                                    cluster = activeSheet.clusters.firstOrNull()?.clusterName ?: "PJU KRISAN"
                                                )
                                            },
                                            color = Red600,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("pju_add_issue_quick_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                                Text(
                                                    text = "+ Masalah",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3B. PJU SHIFT & WAKTU PENGECEKAN SELECTOR CARD
                        item(key = "pju_shift_time_card") {
                            val effectiveShift = selectedPjuShift.ifBlank { activeShift }
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Navy900,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Text(
                                                text = "Shift & Waktu Pengecekan PJU",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Navy900
                                            )
                                        }
                                        Surface(
                                            color = Navy900,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "$effectiveShift • ${selectedPjuTime.ifBlank { "--:--" }} WIB",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Gold400,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // 3 Shift Pilihan untuk Opsi PJU
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ShiftOption.entries.forEach { shiftOpt ->
                                            val isSelected = effectiveShift == shiftOpt.label
                                            Surface(
                                                onClick = { onSelectPjuShift(shiftOpt.label) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) Navy900 else Slate100,
                                                border = BorderStroke(
                                                    1.2.dp,
                                                    if (isSelected) Gold500 else Slate200
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("pju_shift_option_${shiftOpt.label.replace(" ", "_").lowercase()}")
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = shiftOpt.label,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isSelected) Gold400 else Navy900
                                                    )
                                                    Text(
                                                        text = shiftOpt.timeRange.removeSuffix(" WIB"),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else Slate500,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Waktu Pengecekan PJU + Tombol Sekarang
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedPjuTime,
                                            onValueChange = { if (it.length <= 8) onPjuTimeChange(it) },
                                            label = { Text("Waktu Pengecekan (WIB)") },
                                            placeholder = { Text("HH:mm") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = Navy700,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Slate50,
                                                focusedBorderColor = Gold500,
                                                unfocusedBorderColor = Slate200
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("pju_time_input")
                                        )
                                        OutlinedButton(
                                            onClick = onSetPjuTimeNow,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Navy800),
                                            modifier = Modifier
                                                .height(52.dp)
                                                .testTag("pju_set_time_now_button")
                                        ) {
                                            Text(
                                                text = "Sekarang",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Navy900
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. HORIZONTALLY SCROLLABLE PJU SUB-OPTION TABS
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PjuSubOption.entries.forEach { option ->
                                    val selected = subOption == option
                                    val icon = when (option) {
                                        PjuSubOption.MATRIKS -> Icons.Default.GridOn
                                        PjuSubOption.REKAP -> Icons.Default.Analytics
                                        PjuSubOption.MASALAH -> Icons.Default.ReportProblem
                                    }
                                    Surface(
                                        onClick = { subOption = option },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) Navy900 else Color.White.copy(alpha = 0.90f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) Gold500 else Slate200
                                        ),
                                        modifier = Modifier.testTag("pju_subtab_${option.name.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (selected) Gold400 else Navy800,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = option.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (selected) Gold400 else Navy900
                                            )
                                            if (option == PjuSubOption.MASALAH && activeSheet.issues.isNotEmpty()) {
                                                Surface(
                                                    color = Red600,
                                                    shape = RoundedCornerShape(50)
                                                ) {
                                                    Text(
                                                        text = activeSheet.issues.size.toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 5. SUB-VIEW CONTENT
                        when (subOption) {
                            PjuSubOption.MATRIKS -> {
                                item {
                                    PjuMatrixTableCard(
                                        sheet = activeSheet,
                                        pjuFilter = pjuFilter,
                                        onTapCell = { cIdx, cName, day, slots ->
                                            cellEditTarget = PjuCellEditTarget(cIdx, cName, day, slots)
                                        },
                                        onAddCluster = { editingClusterPair = null to "" },
                                        onEditCluster = { idx, name -> editingClusterPair = idx to name },
                                        onDeleteCluster = onDeleteCluster
                                    )
                                }
                            }
                            PjuSubOption.REKAP -> {
                                item {
                                    PjuPeriodicSummarySection(
                                        sheet = activeSheet,
                                        onQuickInspectDay = { day ->
                                            subOption = PjuSubOption.MATRIKS
                                        }
                                    )
                                }
                            }
                            PjuSubOption.MASALAH -> {
                                item {
                                    PjuNotesAndIssuesSection(
                                        sheet = activeSheet,
                                        notesDraft = notesDraft,
                                        onNotesDraftChange = { notesDraft = it },
                                        onSaveNotes = onUpdateNotes,
                                        onAddIssue = {
                                            editingIssuePair = null to PjuIssueEntry(
                                                tanggal = currentIsoDate(),
                                                cluster = activeSheet.clusters.firstOrNull()?.clusterName ?: "PJU KRISAN"
                                            )
                                        },
                                        onEditIssue = { idx, item -> editingIssuePair = idx to item },
                                        onDeleteIssue = onDeleteIssue
                                    )
                                }
                            }
                        }

                        // 6. OFFICIAL KETERANGAN & PENANGGUNG JAWAB FOOTER (matches paper form)
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "KETERANGAN :",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Navy900,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "1. WAJIB MELAKUKAN CHECKLIST PADA SEMUA PJU MAKSIMAL 2 HARI SEKALI",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Navy900,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "2. APABILA PADA SAAT PENGECEKAN TERDAPAT MASALAH, MAKA WAJIB MENGISI PERMASALAHAN PJU PADA FORM YANG TERSEDIA",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Red800,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Penanggung Jawab",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Slate500
                                            )
                                            Text(
                                                text = activeSheet.responsibleNames,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Navy900,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        if (allSheets.size > 1 && (isRoomCreator || currentUserId.isBlank() || activeSheet.userId == currentUserId)) {
                                            TextButton(
                                                onClick = { showDeleteSheetConfirm = true },
                                                colors = ButtonDefaults.textButtonColors(contentColor = Red600)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Hapus Lembar")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 7. DEDICATED BOTTOM SAVE BUTTON (very bottom of PJU option)
                        item(key = "pju_bottom_save") {
                            val chosenShift = selectedPjuShift.ifBlank { activeSheet.resolvedShift().ifBlank { activeShift } }
                            val chosenTime = selectedPjuTime.ifBlank { activeSheet.resolvedUpdatedTime() }.ifBlank { "--:--" }
                            BottomOptionSaveCard(
                                buttonLabel = "Simpan Checklist & Catatan PJU ($chosenShift)",
                                statusSubtitle = "Simpan kondisi lampu, masalah & catatan PJU (${activeSheet.monthYear} • $chosenShift • $chosenTime WIB)",
                                isSaving = isExplicitSaving,
                                testTag = "bottom_save_pju_button",
                                onSaveClick = { onExplicitSave(notesDraft) },
                                onOpenRecapSearch = onOpenGlobalRecap,
                                recapCount = totalRecapCount
                            )
                        }

                        item { Spacer(modifier = Modifier.height(18.dp)) }
                    }
                }
            }
        }
    }

    // DIALOG: Cell 4-Lamp Editor (mirrors 4 sub-rows in physical form!)
    cellEditTarget?.let { target ->
        PjuCellEditorDialog(
            target = target,
            onDismiss = { cellEditTarget = null },
            onSave = { updatedSlots ->
                onSetCellSlots(target.clusterIndex, target.day, updatedSlots)
                cellEditTarget = null
            },
            onReportProblem = {
                cellEditTarget = null
                subOption = PjuSubOption.MASALAH
                val monthStr = activeSheet?.sheetMonth?.toString()?.padStart(2, '0') ?: "11"
                val yearStr = activeSheet?.sheetYear?.toString() ?: "2026"
                val dayStr = target.day.toString().padStart(2, '0')
                editingIssuePair = null to PjuIssueEntry(
                    tanggal = "$yearStr-$monthStr-$dayStr",
                    cluster = target.clusterName,
                    titikLampu = "Lampu 1-4",
                    keluhan = "",
                    tindakLanjut = "",
                    status = "Belum Ditangani"
                )
            }
        )
    }

    // DIALOG: Quick Mark Entire Day All Baik
    if (quickMarkDayDialog && activeSheet != null) {
        val days = activeSheet.daysList()
        val calToday = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
        var selectedDay by remember {
            mutableStateOf(if (calToday in days) calToday else days.firstOrNull() ?: 16)
        }
        AlertDialog(
            onDismissRequest = { quickMarkDayDialog = false },
            title = { Text("Checklist Cepat Semua Cluster", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih tanggal pengecekan untuk menandai seluruh ${activeSheet.clusters.size} cluster (4 titik lampu/cluster) sebagai Nyala / Baik (✓):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        days.forEach { d ->
                            val isSel = selectedDay == d
                            Surface(
                                onClick = { selectedDay = d },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Navy900 else Slate100,
                                border = BorderStroke(1.dp, if (isSel) Gold500 else Slate200)
                            ) {
                                Text(
                                    text = d.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Gold400 else Navy900,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMarkDayAllBaik(selectedDay)
                        quickMarkDayDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Tandai Tgl $selectedDay Semua ✓")
                }
            },
            dismissButton = {
                TextButton(onClick = { quickMarkDayDialog = false }) { Text("Batal") }
            }
        )
    }

    // DIALOG: Create or Edit Sheet Header
    if (showCreateDialog) {
        val cal = remember { Calendar.getInstance() }
        PjuSheetConfigDialog(
            title = "Buat Lembar Checklist PJU Baru",
            initialMonth = cal.get(Calendar.MONTH) + 1,
            initialYear = cal.get(Calendar.YEAR),
            initialStartDay = 16,
            initialEndDay = 30,
            initialResponsible = activeSheet?.responsibleNames ?: PjuTemplates.DEFAULT_RESPONSIBLE,
            showCopyClustersOption = !allSheets.isEmpty(),
            onDismiss = { showCreateDialog = false },
            onConfirm = { m, y, start, end, resp, copyClusters ->
                onCreateSheet(m, y, start, end, resp, copyClusters)
                showCreateDialog = false
            }
        )
    }

    if (showEditHeaderDialog && activeSheet != null) {
        PjuSheetConfigDialog(
            title = "Ubah Periode & Penanggung Jawab",
            initialMonth = activeSheet.sheetMonth.toInt(),
            initialYear = activeSheet.sheetYear.toInt(),
            initialStartDay = activeSheet.periodStartDay.toInt(),
            initialEndDay = activeSheet.periodEndDay.toInt(),
            initialResponsible = activeSheet.responsibleNames,
            showCopyClustersOption = false,
            onDismiss = { showEditHeaderDialog = false },
            onConfirm = { m, y, start, end, resp, _ ->
                onUpdateHeader(m, y, start, end, resp)
                showEditHeaderDialog = false
            }
        )
    }

    // DIALOG: Add / Edit Cluster Name
    editingClusterPair?.let { (editIdx, initialName) ->
        var clusterInput by remember(editIdx, initialName) { mutableStateOf(initialName) }
        AlertDialog(
            onDismissRequest = { editingClusterPair = null },
            title = { Text(if (editIdx == null) "Tambah Cluster PJU" else "Ubah Nama Cluster", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = clusterInput,
                    onValueChange = { clusterInput = it },
                    label = { Text("Nama Cluster / Lokasi PJU") },
                    placeholder = { Text("Contoh: PJU BOULEVARD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpsertCluster(editIdx, clusterInput)
                    editingClusterPair = null
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingClusterPair = null }) { Text("Batal") }
            }
        )
    }

    // DIALOG: Add / Edit PJU Problem Entry
    editingIssuePair?.let { (editIdx, initialIssue) ->
        PjuIssueEditorDialog(
            isEdit = editIdx != null,
            initial = initialIssue,
            clusterOptions = activeSheet?.clusters?.map { it.clusterName }?.ifEmpty { PjuTemplates.defaultClusterNames }
                ?: PjuTemplates.defaultClusterNames,
            onDismiss = { editingIssuePair = null },
            onSave = { updated ->
                onUpsertIssue(editIdx, updated)
                editingIssuePair = null
            }
        )
    }

    if (showDeleteSheetConfirm && activeSheet != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSheetConfirm = false },
            title = { Text("Hapus Lembar Checklist PJU?") },
            text = {
                Text("Lembar PJU ${activeSheet.monthYear} (Tgl ${activeSheet.periodStartDay}–${activeSheet.periodEndDay}) beserta datanya akan dihapus.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSheet()
                        showDeleteSheetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSheetConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun PjuKpiChip(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.labelMedium, color = contentColor, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun PjuMatrixTableCard(
    sheet: PjuChecklistSheet,
    pjuFilter: TimeKeywordFilter,
    onTapCell: (clusterIndex: Int, clusterName: String, day: Int, slots: List<PjuLampStatus>) -> Unit,
    onAddCluster: () -> Unit,
    onEditCluster: (index: Int, name: String) -> Unit,
    onDeleteCluster: (index: Int) -> Unit
) {
    val days = remember(sheet.periodStartDay, sheet.periodEndDay) { sheet.daysList() }
    val cal = remember { Calendar.getInstance() }
    val isCurrentMonthYear = cal.get(Calendar.MONTH) + 1 == sheet.sheetMonth.toInt() &&
        cal.get(Calendar.YEAR) == sheet.sheetYear.toInt()
    val todayDay = if (isCurrentMonthYear) cal.get(Calendar.DAY_OF_MONTH) else -1

    val indexedClusters = remember(sheet.clusters, pjuFilter, sheet) {
        sheet.clusters.mapIndexed { idx, row -> idx to row }
            .filter { (_, row) ->
                if (!pjuFilter.isActive) true else pjuFilter.matchesPjuCluster(row, sheet)
            }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FORM CHECKLIST PENERANGAN JALAN UMUM",
                        style = MaterialTheme.typography.titleSmall,
                        color = Navy900,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "BULAN ${sheet.monthYear.uppercase()} • Tap sel tanggal untuk cek 4 titik lampu",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }
                OutlinedButton(
                    onClick = onAddCluster,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_pju_cluster_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Cluster", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Compact Color Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PjuLegendChip("✓ Nyala", Emerald100, Emerald800)
                PjuLegendChip("! Redup", Amber100, Amber800)
                PjuLegendChip("✕ Mati", Red100, Red800)
                PjuLegendChip("- Kosong", Slate100, Slate500)
            }

            // MATRIX WITH PINNED NO + CLUSTER AND HORIZONTALLY SCROLLABLE DATES
            val rightScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Navy800, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                // LEFT PINNED COLUMNS: NO. & CLUSTER
                Column(
                    modifier = Modifier
                        .width(PJU_COL_NO + PJU_COL_CLUSTER)
                        .background(Color.White)
                ) {
                    // Two-row header height matching "KONDISI LAMPU" + "TANGGAL" (32.dp + 24.dp = 56.dp)
                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth()
                            .background(Navy900)
                            .border(0.5.dp, Navy700),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(PJU_COL_NO)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Gold400
                            )
                        }
                        VerticalDivider(color = Navy700, thickness = 1.dp)
                        Box(
                            modifier = Modifier
                                .width(PJU_COL_CLUSTER)
                                .fillMaxHeight()
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "CLUSTER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Gold400
                            )
                        }
                    }

                    // Cluster rows
                    indexedClusters.forEachIndexed { displayIdx, (actualIdx, cluster) ->
                        val rowBg = if (displayIdx % 2 == 0) Color.White else Slate50
                        Row(
                            modifier = Modifier
                                .height(PJU_ROW_HEIGHT)
                                .fillMaxWidth()
                                .background(rowBg)
                                .clickable { onEditCluster(actualIdx, cluster.clusterName) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(PJU_COL_NO)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${actualIdx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            }
                            VerticalDivider(color = Slate200, thickness = 1.dp)
                            Row(
                                modifier = Modifier
                                    .width(PJU_COL_CLUSTER)
                                    .fillMaxHeight()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cluster.clusterName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (sheet.clusters.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteCluster(actualIdx) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus cluster",
                                            tint = Slate400,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Slate200, thickness = 1.dp)
                    }
                }

                VerticalDivider(color = Navy800, thickness = 1.5.dp)

                // RIGHT SCROLLABLE COLUMNS: KONDISI LAMPU -> TANGGAL -> [16..30]
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rightScrollState)
                ) {
                    val totalDaysWidth = PJU_COL_DAY * days.size
                    // Header row 1: KONDISI LAMPU
                    Box(
                        modifier = Modifier
                            .width(totalDaysWidth)
                            .height(28.dp)
                            .background(Navy900),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "KONDISI LAMPU",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Gold400
                        )
                    }
                    HorizontalDivider(color = Navy700, thickness = 1.dp)
                    // Header row 2: TANGGAL / numbers
                    Row(
                        modifier = Modifier
                            .width(totalDaysWidth)
                            .height(28.dp)
                            .background(Navy800),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        days.forEachIndexed { idx, day ->
                            val isToday = day == todayDay
                            Box(
                                modifier = Modifier
                                    .width(PJU_COL_DAY)
                                    .fillMaxHeight()
                                    .background(if (isToday) Gold500 else Navy800),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isToday) Navy950 else Color.White
                                )
                            }
                            if (idx < days.lastIndex) {
                                VerticalDivider(color = Navy700, thickness = 1.dp)
                            }
                        }
                    }

                    // Body rows with 4 sub-lamp bands per cell (faithful to paper form)
                    indexedClusters.forEachIndexed { displayIdx, (actualIdx, cluster) ->
                        val rowBg = if (displayIdx % 2 == 0) Color.White else Slate50
                        Row(
                            modifier = Modifier
                                .height(PJU_ROW_HEIGHT)
                                .background(rowBg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            days.forEachIndexed { dIdx, day ->
                                val slots = cluster.slotsForDay(day)
                                val isToday = day == todayDay
                                Column(
                                    modifier = Modifier
                                        .width(PJU_COL_DAY)
                                        .fillMaxHeight()
                                        .background(if (isToday) Navy50.copy(alpha = 0.55f) else Color.Transparent)
                                        .clickable {
                                            onTapCell(actualIdx, cluster.clusterName, day, slots)
                                        }
                                        .testTag("pju_cell_${actualIdx}_$day")
                                ) {
                                    slots.forEachIndexed { sIdx, status ->
                                        val (slotBg, slotFg) = pjuSlotColors(status)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .background(slotBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (status == PjuLampStatus.NONE) "" else status.symbol,
                                                fontSize = 8.sp,
                                                lineHeight = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = slotFg
                                            )
                                        }
                                        if (sIdx < 3) {
                                            HorizontalDivider(color = Slate200.copy(alpha = 0.85f), thickness = 0.5.dp)
                                        }
                                    }
                                }
                                if (dIdx < days.lastIndex) {
                                    VerticalDivider(color = Slate200, thickness = 1.dp)
                                }
                            }
                        }
                        HorizontalDivider(color = Slate200, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PjuLegendChip(text: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

private fun pjuSlotColors(status: PjuLampStatus): Pair<Color, Color> = when (status) {
    PjuLampStatus.BAIK -> Emerald100.copy(alpha = 0.85f) to Emerald800
    PjuLampStatus.REDUP -> Amber100 to Amber800
    PjuLampStatus.MATI -> Red100 to Red800
    PjuLampStatus.NONE -> Color.Transparent to Slate400
}

@Composable
private fun PjuPeriodicSummarySection(
    sheet: PjuChecklistSheet,
    onQuickInspectDay: (Int) -> Unit
) {
    val checkedDays = remember(sheet) { sheet.checkedDays() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Health Breakdown Per Cluster
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Rekap Kesehatan Per Cluster (${sheet.monthYear})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Navy900,
                    fontWeight = FontWeight.ExtraBold
                )
                sheet.clusters.forEachIndexed { idx, cluster ->
                    val pct = sheet.clusterHealthPercent(cluster)
                    val days = sheet.daysList()
                    val allSlots = days.flatMap { cluster.slotsForDay(it) }.filter { it != PjuLampStatus.NONE }
                    val baikCount = allSlots.count { it == PjuLampStatus.BAIK }
                    val redupCount = allSlots.count { it == PjuLampStatus.REDUP }
                    val matiCount = allSlots.count { it == PjuLampStatus.MATI }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}. ${cluster.clusterName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Navy900,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (pct != null) "$pct% Nyala (✓$baikCount !$redupCount ✕$matiCount)" else "Belum Dicek",
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    pct == null -> Slate500
                                    matiCount > 0 -> Red800
                                    redupCount > 0 -> Amber800
                                    else -> Emerald800
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (pct ?: 0) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = when {
                                pct == null -> Slate200
                                matiCount > 0 -> Red600
                                redupCount > 0 -> Gold600
                                else -> Emerald600
                            },
                            trackColor = Slate100
                        )
                    }
                    if (idx < sheet.clusters.lastIndex) {
                        HorizontalDivider(color = Slate100)
                    }
                }
            }
        }

        // 2. Chronological Periodic Check Log
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Riwayat Pengecekan Berkala (Per Tanggal)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Navy900,
                    fontWeight = FontWeight.ExtraBold
                )
                if (checkedDays.isEmpty()) {
                    Text(
                        text = "Belum ada tanggal yang diisi checklist. Ketuk tab 'Matriks Tabel PJU' atau tombol 'Tandai Semua Nyala' untuk mulai mencatat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                } else {
                    checkedDays.reversed().forEach { day ->
                        val checkedClusters = sheet.clusters.count { it.hasAnyCheckOnDay(day) }
                        val daySlots = sheet.clusters.flatMap { it.slotsForDay(day) }.filter { it != PjuLampStatus.NONE }
                        val b = daySlots.count { it == PjuLampStatus.BAIK }
                        val r = daySlots.count { it == PjuLampStatus.REDUP }
                        val m = daySlots.count { it == PjuLampStatus.MATI }
                        Surface(
                            onClick = { onQuickInspectDay(day) },
                            shape = RoundedCornerShape(10.dp),
                            color = Slate50,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Tanggal $day ${sheet.monthYear}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Navy900,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$checkedClusters / ${sheet.clusters.size} Cluster dicek",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate500
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    PjuLegendChip("✓$b", Emerald100, Emerald800)
                                    if (r > 0) PjuLegendChip("!$r", Amber100, Amber800)
                                    if (m > 0) PjuLegendChip("✕$m", Red100, Red800)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PjuNotesAndIssuesSection(
    sheet: PjuChecklistSheet,
    notesDraft: String,
    onNotesDraftChange: (String) -> Unit,
    onSaveNotes: (String) -> Unit,
    onAddIssue: () -> Unit,
    onEditIssue: (index: Int, entry: PjuIssueEntry) -> Unit,
    onDeleteIssue: (index: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // CATATAN Card matching physical sheet rectangle
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
            border = BorderStroke(1.dp, Navy800)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATATAN :",
                        style = MaterialTheme.typography.labelLarge,
                        color = Navy900,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Button(
                        onClick = { onSaveNotes(notesDraft) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
                    ) {
                        Text("Simpan Catatan", style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedTextField(
                    value = notesDraft,
                    onValueChange = { onNotesDraftChange(it.take(2000)) },
                    placeholder = { Text("Tulis catatan pengecekan PJU di sini...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pju_notes_input")
                )
            }
        }

        // FORM PERMASALAHAN PJU
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Form Permasalahan PJU",
                            style = MaterialTheme.typography.titleSmall,
                            color = Navy900,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Wajib diisi jika terdapat lampu mati / redup / kendala tiang",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                    Button(
                        onClick = onAddIssue,
                        colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lapor Masalah", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (sheet.issues.isEmpty()) {
                    Surface(
                        color = Emerald100.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nihil permasalahan PJU tercatat pada periode ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald800,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    sheet.issues.forEachIndexed { idx, issue ->
                        val (stBg, stFg) = when (issue.status) {
                            "Selesai" -> Emerald100 to Emerald800
                            "Dalam Perbaikan" -> Amber100 to Amber800
                            else -> Red100 to Red800
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate50,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${issue.cluster} • ${issue.titikLampu}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Navy900,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(color = stBg, shape = RoundedCornerShape(50)) {
                                        Text(
                                            text = issue.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = stFg,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Tanggal: ${issue.tanggal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500
                                )
                                Text(
                                    text = issue.keluhan,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Navy900
                                )
                                if (issue.tindakLanjut.isNotBlank()) {
                                    Text(
                                        text = "Tindak Lanjut: ${issue.tindakLanjut}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Navy700,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { onEditIssue(idx, issue) }) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Edit")
                                    }
                                    TextButton(
                                        onClick = { onDeleteIssue(idx) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Red600)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Hapus")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PjuCellEditorDialog(
    target: PjuCellEditTarget,
    onDismiss: () -> Unit,
    onSave: (List<PjuLampStatus>) -> Unit,
    onReportProblem: () -> Unit
) {
    var slots by remember(target) { mutableStateOf(target.slots) }
    val hasProblem = slots.any { it == PjuLampStatus.MATI || it == PjuLampStatus.REDUP }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${target.clusterName} • Tanggal ${target.day}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Navy900,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ketuk setiap titik lampu (4 baris sub-kolom sesuai form checklist) untuk mengubah status:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )

                // Quick Set All Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { slots = List(4) { PjuLampStatus.BAIK } },
                        color = Emerald100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Semua ✓",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Emerald800,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    Surface(
                        onClick = { slots = List(4) { PjuLampStatus.MATI } },
                        color = Red100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Semua ✕",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Red800,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    Surface(
                        onClick = { slots = List(4) { PjuLampStatus.NONE } },
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Reset -",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate700,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // 4 Individual Lamp Slot Selectors
                slots.forEachIndexed { idx, cur ->
                    val (bg, fg) = when (cur) {
                        PjuLampStatus.BAIK -> Emerald100 to Emerald800
                        PjuLampStatus.REDUP -> Amber100 to Amber800
                        PjuLampStatus.MATI -> Red100 to Red800
                        PjuLampStatus.NONE -> Slate100 to Slate700
                    }
                    Surface(
                        onClick = {
                            val updated = slots.toMutableList()
                            updated[idx] = cur.next()
                            slots = updated
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = bg,
                        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Titik Lampu ${idx + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Navy900,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${cur.symbol}  ${cur.label}",
                                style = MaterialTheme.typography.labelLarge,
                                color = fg,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                if (hasProblem) {
                    OutlinedButton(
                        onClick = onReportProblem,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red600),
                        border = BorderStroke(1.dp, Red600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Isi Form Permasalahan PJU", fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { onSave(slots) },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PjuSheetConfigDialog(
    title: String,
    initialMonth: Int,
    initialYear: Int,
    initialStartDay: Int,
    initialEndDay: Int,
    initialResponsible: String,
    showCopyClustersOption: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (month: Int, year: Int, startDay: Int, endDay: Int, responsibleNames: String, copyClusters: Boolean) -> Unit
) {
    var month by remember { mutableStateOf(initialMonth.coerceIn(1, 12)) }
    var yearStr by remember { mutableStateOf(initialYear.toString()) }
    var startStr by remember { mutableStateOf(initialStartDay.toString()) }
    var endStr by remember { mutableStateOf(initialEndDay.toString()) }
    var responsible by remember { mutableStateOf(initialResponsible) }
    var copyClusters by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Navy900, fontWeight = FontWeight.ExtraBold)
                Text("Pilih Bulan:", style = MaterialTheme.typography.labelMedium, color = Navy900, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    IndonesianMonths.shortNames.forEachIndexed { idx, mName ->
                        val mNum = idx + 1
                        val sel = month == mNum
                        Surface(
                            onClick = { month = mNum },
                            color = if (sel) Navy900 else Slate100,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = mName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Gold400 else Navy900,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Tahun") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startStr,
                        onValueChange = { startStr = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Tgl Mulai") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endStr,
                        onValueChange = { endStr = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Tgl Akhir") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Preset Period buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { startStr = "1"; endStr = "15" },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) { Text("Tgl 1–15", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { startStr = "16"; endStr = "30" },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) { Text("Tgl 16–30", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { startStr = "1"; endStr = "31" },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) { Text("1 Bulan", style = MaterialTheme.typography.labelSmall) }
                }
                OutlinedTextField(
                    value = responsible,
                    onValueChange = { responsible = it.take(120) },
                    label = { Text("Penanggung Jawab") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showCopyClustersOption) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { copyClusters = !copyClusters }
                    ) {
                        Checkbox(checked = copyClusters, onCheckedChange = { copyClusters = it })
                        Text(
                            text = "Salin daftar cluster dari lembar aktif",
                            style = MaterialTheme.typography.bodySmall,
                            color = Navy900
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val y = yearStr.toIntOrNull()?.coerceIn(2020, 2100) ?: initialYear
                            val s = startStr.toIntOrNull()?.coerceIn(1, 31) ?: 16
                            val e = endStr.toIntOrNull()?.coerceIn(s, 31) ?: 30
                            onConfirm(month, y, s, e, responsible, copyClusters)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PjuIssueEditorDialog(
    isEdit: Boolean,
    initial: PjuIssueEntry,
    clusterOptions: List<String>,
    onDismiss: () -> Unit,
    onSave: (PjuIssueEntry) -> Unit
) {
    var tanggal by remember { mutableStateOf(initial.tanggal) }
    var cluster by remember { mutableStateOf(initial.cluster.ifBlank { clusterOptions.firstOrNull() ?: "PJU KRISAN" }) }
    var titikLampu by remember { mutableStateOf(initial.titikLampu) }
    var keluhan by remember { mutableStateOf(initial.keluhan) }
    var tindakLanjut by remember { mutableStateOf(initial.tindakLanjut) }
    var status by remember { mutableStateOf(initial.status) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isEdit) "Edit Permasalahan PJU" else "Catat Permasalahan PJU",
                    style = MaterialTheme.typography.titleMedium,
                    color = Navy900,
                    fontWeight = FontWeight.ExtraBold
                )
                OutlinedTextField(
                    value = tanggal,
                    onValueChange = { tanggal = it.take(24) },
                    label = { Text("Tanggal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Pilih Cluster:", style = MaterialTheme.typography.labelSmall, color = Slate500)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    clusterOptions.forEach { opt ->
                        val sel = cluster.equals(opt, ignoreCase = true)
                        Surface(
                            onClick = { cluster = opt },
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) Navy900 else Slate100
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Gold400 else Navy900,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = titikLampu,
                    onValueChange = { titikLampu = it.take(40) },
                    label = { Text("Titik / Nomor Lampu") },
                    placeholder = { Text("Contoh: Lampu 2 dekat taman") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = keluhan,
                    onValueChange = { keluhan = it.take(400) },
                    label = { Text("Keluhan / Kerusakan") },
                    placeholder = { Text("Contoh: Lampu mati total / kabel putus / redup") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tindakLanjut,
                    onValueChange = { tindakLanjut = it.take(400) },
                    label = { Text("Tindak Lanjut (Opsional)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Status Penanganan:", style = MaterialTheme.typography.labelSmall, color = Slate500)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PjuTemplates.issueStatusOptions.forEach { st ->
                        val sel = status == st
                        Surface(
                            onClick = { status = st },
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) Navy900 else Slate100,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = st,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Gold400 else Navy900,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            onSave(
                                PjuIssueEntry(
                                    tanggal = tanggal,
                                    cluster = cluster,
                                    titikLampu = titikLampu,
                                    keluhan = keluhan,
                                    tindakLanjut = tindakLanjut,
                                    status = status
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
                    ) {
                        Text("Simpan Masalah", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
