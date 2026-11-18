package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityTableRow
import com.example.data.model.DailyActivitySheet
import com.example.data.model.InventoryRow
import com.example.data.model.PersonnelRow
import com.example.data.model.ShiftOption
import com.example.data.model.TimeKeywordFilter
import com.example.data.model.UiState
import com.example.data.model.currentIsoDate
import com.example.data.model.parseIsoDateTriple
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber800
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
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
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TableReportTabScreen(
    sheetsState: UiState<List<DailyActivitySheet>>,
    selectedSheetId: String?,
    tableFilter: TimeKeywordFilter,
    defaultShift: String,
    isExplicitSaving: Boolean,
    onSelectSheet: (String) -> Unit,
    onCreateSheet: (sheetDate: String, shift: String, docNumber: String, includeDefault: Boolean, onSuccess: () -> Unit) -> Unit,
    onUpdateHeader: (sheetDate: String, shift: String, docNumber: String) -> Unit,
    onUpsertPersonnel: (editIndex: Int?, row: PersonnelRow) -> Unit,
    onDeletePersonnel: (Int) -> Unit,
    onUpsertInventory: (editIndex: Int?, row: InventoryRow) -> Unit,
    onDeleteInventory: (Int) -> Unit,
    onLoadDefaultInventory: () -> Unit,
    onUpsertActivity: (editIndex: Int?, row: ActivityTableRow) -> Unit,
    onDeleteActivity: (Int) -> Unit,
    onDeleteSheet: () -> Unit,
    onExplicitSave: () -> Unit,
    onCopyWhatsApp: (DailyActivitySheet) -> Unit,
    onPrintPdf: (DailyActivitySheet) -> Unit,
    onFilterKeywordChange: (String) -> Unit,
    onFilterHourChange: (Int?) -> Unit,
    onFilterDayChange: (Int?) -> Unit,
    onFilterMonthChange: (Int?) -> Unit,
    onFilterYearChange: (Int?) -> Unit,
    onFilterShiftChange: (String?) -> Unit,
    onFilterToday: () -> Unit,
    onResetFilter: () -> Unit
) {
    val allSheets = (sheetsState as? UiState.Success)?.data.orEmpty()
    val filteredSheets = remember(allSheets, tableFilter) {
        if (!tableFilter.isActive) allSheets else allSheets.filter { tableFilter.matchesSheetHeader(it) }
    }
    val activeSheet = remember(filteredSheets, allSheets, selectedSheetId) {
        filteredSheets.firstOrNull { it.id == selectedSheetId }
            ?: filteredSheets.firstOrNull()
            ?: allSheets.firstOrNull { it.id == selectedSheetId }
            ?: allSheets.firstOrNull()
    }
    val availableYears = remember(allSheets) {
        allSheets.mapNotNull { parseIsoDateTriple(it.sheetDate)?.first }.distinct()
    }

    // Modal state
    var showNewSheetDialog by remember { mutableStateOf(false) }
    var showEditHeaderDialog by remember { mutableStateOf(false) }
    var showDeleteSheetConfirm by remember { mutableStateOf(false) }

    var editingPersonnelPair by remember { mutableStateOf<Pair<Int?, PersonnelRow>?>(null) }
    var editingInventoryPair by remember { mutableStateOf<Pair<Int?, InventoryRow>?>(null) }
    var editingActivityPair by remember { mutableStateOf<Pair<Int?, ActivityTableRow>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Sheet Manager & Actions Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = Gold500
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tabel Buku Jurnal & Kegiatan Security",
                                style = MaterialTheme.typography.titleMedium,
                                color = Navy900
                            )
                            Text(
                                text = "Format Personel, Inventaris Pos & Uraian Kegiatan per Jam",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }

                    // Sheet picker strip if there are multiple sheets
                    if (allSheets.isNotEmpty()) {
                        Text(
                            text = "Pilih Lembar Shift (${filteredSheets.size}/${allSheets.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate700
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredSheets.ifEmpty { allSheets }, key = { it.id }) { sheet ->
                                val selected = sheet.id == activeSheet?.id
                                Surface(
                                    onClick = { onSelectSheet(sheet.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) Navy900 else Slate100,
                                    border = BorderStroke(1.dp, if (selected) Gold500 else Slate300)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "${sheet.sheetDate} • ${sheet.shift}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Gold400 else Navy900
                                        )
                                        Text(
                                            text = "No: ${sheet.docNumber.ifBlank { "-" }} • ${sheet.activityRows.size} keg.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) Color.White.copy(alpha = 0.8f) else Slate500
                                        )
                                    }
                                }
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showNewSheetDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("new_sheet_button")
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lembar Baru", style = MaterialTheme.typography.labelMedium)
                        }

                        if (activeSheet != null) {
                            Button(
                                onClick = { onCopyWhatsApp(activeSheet) },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald800, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("copy_table_wa_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Salin WA", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { onPrintPdf(activeSheet) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Navy700),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("print_table_pdf_button")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cetak PDF Tabel", style = MaterialTheme.typography.labelMedium, color = Navy900)
                            }

                            if (allSheets.size > 1) {
                                OutlinedButton(
                                    onClick = { showDeleteSheetConfirm = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Red600.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Red600, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Hapus Lembar", style = MaterialTheme.typography.labelMedium, color = Red600)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Search & Granular Time Filter (Hour / Day / Month / Year / Shift)
        item(key = "table_filter_panel") {
            SearchAndTimeFilterPanel(
                title = "Cari & Saring Tabel (Jam / Hari / Bulan / Tahun / Shift)",
                filter = tableFilter,
                availableYears = availableYears,
                totalCount = allSheets.size,
                filteredCount = filteredSheets.size,
                testTagPrefix = "table",
                onKeywordChange = onFilterKeywordChange,
                onHourChange = onFilterHourChange,
                onDayChange = onFilterDayChange,
                onMonthChange = onFilterMonthChange,
                onYearChange = onFilterYearChange,
                onShiftChange = onFilterShiftChange,
                onSelectToday = onFilterToday,
                onReset = onResetFilter
            )
        }

        // 3. Active Sheet 3-Table Content
        when (sheetsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Navy900)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        color = Red100,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = sheetsState.message,
                            color = Red800,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                if (activeSheet == null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Belum ada lembar tabel pelaporan.",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Navy900
                                )
                                Button(
                                    onClick = { showNewSheetDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Buat Lembar Tabel Sekarang")
                                }
                            }
                        }
                    }
                } else {
                    // Header Info Banner matching top of logbook page (Day/Date, Shift, No.)
                    item(key = "sheet_header_info") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                            border = BorderStroke(1.5.dp, Navy800)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = activeSheet.formattedDisplayDate(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Navy900
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(color = Gold500, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = activeSheet.shift,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Navy950,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "No: ${activeSheet.docNumber.ifBlank { "-" }}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Slate700,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showEditHeaderDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = Navy900)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Ubah Header", style = MaterialTheme.typography.labelSmall, color = Navy900)
                                }
                            }
                        }
                    }

                    // TABEL 1: DAFTAR PERSONEL JAGA (NO | NAMA | JABATAN | PLOT | H/T | KET)
                    item(key = "table_personnel") {
                        TableSectionCard(
                            icon = Icons.Default.People,
                            title = "1. TABEL PERSONEL JAGA",
                            subtitle = "Ketuk baris untuk mengubah data anggota",
                            onAddClick = {
                                editingPersonnelPair = Pair(null, PersonnelRow())
                            },
                            addButtonLabel = "Anggota",
                            addButtonTag = "add_personnel_row_button"
                        ) {
                            val hScroll = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(hScroll)
                                    .border(1.dp, Navy800, RoundedCornerShape(8.dp))
                            ) {
                                // Header Row
                                Row(
                                    modifier = Modifier.background(Navy900)
                                ) {
                                    GridHeaderCell("NO", 40.dp, TextAlign.Center)
                                    GridHeaderCell("NAMA", 148.dp)
                                    GridHeaderCell("JABATAN", 104.dp)
                                    GridHeaderCell("PLOT", 58.dp, TextAlign.Center)
                                    GridHeaderCell("H / T", 76.dp, TextAlign.Center)
                                    GridHeaderCell("KET", 112.dp)
                                    GridHeaderCell("AKSI", 56.dp, TextAlign.Center)
                                }
                                val filteredPersonnel = if (tableFilter.keyword.isBlank()) {
                                    activeSheet.personnelRows.mapIndexed { idx, row -> idx to row }
                                } else {
                                    val q = tableFilter.keyword.trim().lowercase(Locale.getDefault())
                                    activeSheet.personnelRows.mapIndexedNotNull { idx, row ->
                                        val match = row.nama.lowercase(Locale.getDefault()).contains(q) ||
                                            row.jabatan.lowercase(Locale.getDefault()).contains(q) ||
                                            row.plot.lowercase(Locale.getDefault()).contains(q) ||
                                            row.kehadiran.lowercase(Locale.getDefault()).contains(q) ||
                                            row.ket.lowercase(Locale.getDefault()).contains(q)
                                        if (match) idx to row else null
                                    }
                                }

                                if (filteredPersonnel.isEmpty()) {
                                    Text(
                                        text = "Belum ada data anggota pada tabel ini.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                } else {
                                    filteredPersonnel.forEachIndexed { displayIdx, (origIdx, row) ->
                                        val rowBg = if (displayIdx % 2 == 0) Color.White else Slate50
                                        Row(
                                            modifier = Modifier
                                                .background(rowBg)
                                                .clickable { editingPersonnelPair = Pair(origIdx, row) },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            GridDataCell("${origIdx + 1}", 40.dp, TextAlign.Center, FontWeight.Bold)
                                            GridDataCell(row.nama, 148.dp, fontWeight = FontWeight.SemiBold)
                                            GridDataCell(row.jabatan, 104.dp)
                                            GridDataCell(row.plot, 58.dp, TextAlign.Center, FontWeight.Bold)
                                            GridDataCell(
                                                text = row.kehadiran,
                                                width = 76.dp,
                                                align = TextAlign.Center,
                                                fontWeight = FontWeight.Bold,
                                                textColor = if (row.kehadiran.equals("Hadir", ignoreCase = true)) Emerald800 else Red800
                                            )
                                            GridDataCell(row.ket.ifBlank { "-" }, 112.dp)
                                            Box(
                                                modifier = Modifier
                                                    .width(56.dp)
                                                    .padding(vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(
                                                    onClick = { onDeletePersonnel(origIdx) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Hapus anggota", tint = Red600, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Slate300, thickness = 0.8.dp)
                                    }
                                }
                            }
                        }
                    }

                    // TABEL 2: DAFTAR INVENTARIS POS (NO | INVENTARIS | JUMLAH | KONDISI | KETERANGAN)
                    item(key = "table_inventory") {
                        TableSectionCard(
                            icon = Icons.Default.Inventory2,
                            title = "2. TABEL INVENTARIS POS BPS",
                            subtitle = "Ketuk baris untuk ubah jumlah, kondisi, atau keterangan barang",
                            onAddClick = {
                                editingInventoryPair = Pair(null, InventoryRow())
                            },
                            addButtonLabel = "Barang",
                            addButtonTag = "add_inventory_row_button",
                            extraHeaderAction = {
                                TextButton(
                                    onClick = onLoadDefaultInventory,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = Navy900)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Isi 12 Standar", style = MaterialTheme.typography.labelSmall, color = Navy900)
                                }
                            }
                        ) {
                            val hScroll = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(hScroll)
                                    .border(1.dp, Navy800, RoundedCornerShape(8.dp))
                            ) {
                                Row(modifier = Modifier.background(Navy900)) {
                                    GridHeaderCell("NO", 40.dp, TextAlign.Center)
                                    GridHeaderCell("INVENTARIS", 196.dp)
                                    GridHeaderCell("JUMLAH", 92.dp, TextAlign.Center)
                                    GridHeaderCell("KONDISI", 88.dp, TextAlign.Center)
                                    GridHeaderCell("KETERANGAN", 172.dp)
                                    GridHeaderCell("AKSI", 56.dp, TextAlign.Center)
                                }
                                val filteredInventory = if (tableFilter.keyword.isBlank()) {
                                    activeSheet.inventoryRows.mapIndexed { idx, row -> idx to row }
                                } else {
                                    val q = tableFilter.keyword.trim().lowercase(Locale.getDefault())
                                    activeSheet.inventoryRows.mapIndexedNotNull { idx, row ->
                                        val match = row.inventaris.lowercase(Locale.getDefault()).contains(q) ||
                                            row.jumlah.lowercase(Locale.getDefault()).contains(q) ||
                                            row.kondisi.lowercase(Locale.getDefault()).contains(q) ||
                                            row.keterangan.lowercase(Locale.getDefault()).contains(q)
                                        if (match) idx to row else null
                                    }
                                }

                                if (filteredInventory.isEmpty()) {
                                    Text(
                                        text = "Belum ada data inventaris. Tekan 'Isi 12 Standar' atau '+ Barang'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                } else {
                                    filteredInventory.forEachIndexed { displayIdx, (origIdx, row) ->
                                        val rowBg = if (displayIdx % 2 == 0) Color.White else Slate50
                                        val isDamaged = row.kondisi.contains("rusak", ignoreCase = true)
                                        Row(
                                            modifier = Modifier
                                                .background(rowBg)
                                                .clickable { editingInventoryPair = Pair(origIdx, row) },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            GridDataCell("${origIdx + 1}", 40.dp, TextAlign.Center, FontWeight.Bold)
                                            GridDataCell(row.inventaris, 196.dp, fontWeight = FontWeight.Medium)
                                            GridDataCell(row.jumlah, 92.dp, TextAlign.Center)
                                            GridDataCell(
                                                text = row.kondisi,
                                                width = 88.dp,
                                                align = TextAlign.Center,
                                                fontWeight = FontWeight.Bold,
                                                textColor = if (isDamaged) Red800 else Emerald800
                                            )
                                            GridDataCell(row.keterangan.ifBlank { "-" }, 172.dp)
                                            Box(
                                                modifier = Modifier
                                                    .width(56.dp)
                                                    .padding(vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(
                                                    onClick = { onDeleteInventory(origIdx) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Hapus inventaris", tint = Red600, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Slate300, thickness = 0.8.dp)
                                    }
                                }
                            }
                        }
                    }

                    // TABEL 3: URAIAN KEGIATAN (NO | JAM | URAIAN KEGIATAN)
                    item(key = "table_activities") {
                        TableSectionCard(
                            icon = Icons.Default.Schedule,
                            title = "3. TABEL URAIAN KEGIATAN",
                            subtitle = "Catatan kronologis per jam • Ketuk baris untuk mengedit",
                            onAddClick = {
                                val nowJam = SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date())
                                editingActivityPair = Pair(null, ActivityTableRow(jam = nowJam, uraian = ""))
                            },
                            addButtonLabel = "Kegiatan",
                            addButtonTag = "add_activity_row_button"
                        ) {
                            val hScroll = rememberScrollState()
                            val filteredActivities = activeSheet.activityRows.mapIndexedNotNull { idx, row ->
                                if (!tableFilter.isActive || tableFilter.matchesActivityRow(row, activeSheet.sheetDate)) {
                                    idx to row
                                } else {
                                    null
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(hScroll)
                                    .border(1.dp, Navy800, RoundedCornerShape(8.dp))
                            ) {
                                Row(modifier = Modifier.background(Navy900)) {
                                    GridHeaderCell("NO", 40.dp, TextAlign.Center)
                                    GridHeaderCell("JAM", 74.dp, TextAlign.Center)
                                    GridHeaderCell("URAIAN KEGIATAN", 360.dp)
                                    GridHeaderCell("AKSI", 56.dp, TextAlign.Center)
                                }

                                if (filteredActivities.isEmpty()) {
                                    Text(
                                        text = if (tableFilter.isActive) {
                                            "Tidak ada baris uraian kegiatan yang cocok dengan filter Jam/Kata kunci."
                                        } else {
                                            "Belum ada uraian kegiatan pada lembar ini."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                } else {
                                    filteredActivities.forEachIndexed { displayIdx, (origIdx, row) ->
                                        val rowBg = if (displayIdx % 2 == 0) Color.White else Slate50
                                        Row(
                                            modifier = Modifier
                                                .background(rowBg)
                                                .clickable { editingActivityPair = Pair(origIdx, row) },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            GridDataCell("${origIdx + 1}", 40.dp, TextAlign.Center, FontWeight.Bold)
                                            GridDataCell(row.jam, 74.dp, TextAlign.Center, FontWeight.Bold, Navy900)
                                            GridDataCell(row.uraian, 360.dp)
                                            Box(
                                                modifier = Modifier
                                                    .width(56.dp)
                                                    .padding(vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(
                                                    onClick = { onDeleteActivity(origIdx) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Hapus kegiatan", tint = Red600, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Slate300, thickness = 0.8.dp)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Dedicated Bottom Save Button (very bottom of Tabel option)
                    item(key = "table_bottom_save") {
                        BottomOptionSaveCard(
                            buttonLabel = "Simpan Tabel Kegiatan (${activeSheet.shift})",
                            statusSubtitle = "Simpan permanen Personel, Inventaris & ${activeSheet.activityRows.size} Uraian Kegiatan (${activeSheet.sheetDate})",
                            isSaving = isExplicitSaving,
                            testTag = "bottom_save_tabel_button",
                            onSaveClick = onExplicitSave
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Dialogs
    if (showNewSheetDialog) {
        NewActivitySheetDialog(
            defaultShift = defaultShift,
            onDismiss = { showNewSheetDialog = false },
            onCreate = { date, shift, docNum, includeDefault ->
                onCreateSheet(date, shift, docNum, includeDefault) {
                    showNewSheetDialog = false
                }
            }
        )
    }

    if (showEditHeaderDialog && activeSheet != null) {
        EditSheetHeaderDialog(
            sheet = activeSheet,
            onDismiss = { showEditHeaderDialog = false },
            onSave = { date, shift, docNum ->
                onUpdateHeader(date, shift, docNum)
                showEditHeaderDialog = false
            }
        )
    }

    if (showDeleteSheetConfirm && activeSheet != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSheetConfirm = false },
            title = { Text("Hapus Lembar Tabel?", fontWeight = FontWeight.Bold) },
            text = { Text("Lembar tanggal ${activeSheet.sheetDate} (${activeSheet.shift}) beserta seluruh tabelnya akan dihapus permanen.") },
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
                TextButton(onClick = { showDeleteSheetConfirm = false }) { Text("Batal") }
            }
        )
    }

    editingPersonnelPair?.let { (idx, initial) ->
        EditPersonnelRowDialog(
            isEditing = idx != null,
            initial = initial,
            onDismiss = { editingPersonnelPair = null },
            onSave = { updated ->
                onUpsertPersonnel(idx, updated)
                editingPersonnelPair = null
            }
        )
    }

    editingInventoryPair?.let { (idx, initial) ->
        EditInventoryRowDialog(
            isEditing = idx != null,
            initial = initial,
            onDismiss = { editingInventoryPair = null },
            onSave = { updated ->
                onUpsertInventory(idx, updated)
                editingInventoryPair = null
            }
        )
    }

    editingActivityPair?.let { (idx, initial) ->
        EditActivityRowDialog(
            isEditing = idx != null,
            initial = initial,
            onDismiss = { editingActivityPair = null },
            onSave = { updated ->
                onUpsertActivity(idx, updated)
                editingActivityPair = null
            }
        )
    }
}

@Composable
private fun TableSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    addButtonLabel: String,
    addButtonTag: String,
    onAddClick: () -> Unit,
    extraHeaderAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Navy100,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Navy900,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Navy900
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }
                extraHeaderAction?.invoke()
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag(addButtonTag)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(addButtonLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            content()
        }
    }
}

@Composable
private fun GridHeaderCell(
    text: String,
    width: Dp,
    align: TextAlign = TextAlign.Start
) {
    Box(
        modifier = Modifier
            .width(width)
            .border(0.5.dp, Navy700)
            .padding(horizontal = 8.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Gold400,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GridDataCell(
    text: String,
    width: Dp,
    align: TextAlign = TextAlign.Start,
    fontWeight: FontWeight = FontWeight.Normal,
    textColor: Color = Slate800
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 8.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = fontWeight,
            color = textColor,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewActivitySheetDialog(
    defaultShift: String,
    onDismiss: () -> Unit,
    onCreate: (sheetDate: String, shift: String, docNumber: String, includeDefault: Boolean) -> Unit
) {
    var sheetDate by remember { mutableStateOf(currentIsoDate()) }
    var shift by remember { mutableStateOf(defaultShift) }
    var docNumber by remember { mutableStateOf("") }
    var includeDefault by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Lembar Tabel Baru", fontWeight = FontWeight.Bold, color = Navy900) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sheetDate,
                    onValueChange = { sheetDate = it },
                    label = { Text("Tanggal (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = docNumber,
                    onValueChange = { docNumber = it },
                    label = { Text("No. Dokumen (Opsional)") },
                    placeholder = { Text("Contoh: 002/BPS") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Pilih Shift:", style = MaterialTheme.typography.labelMedium, color = Navy900)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShiftOption.entries.forEach { opt ->
                        FilterChip(
                            selected = shift == opt.label,
                            onClick = { shift = opt.label },
                            label = { Text(opt.label) }
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { includeDefault = !includeDefault }
                ) {
                    Checkbox(checked = includeDefault, onCheckedChange = { includeDefault = it })
                    Text(
                        text = "Sertakan 12 item inventaris standar Pos BPS & format personel",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(sheetDate, shift, docNumber, includeDefault) },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
            ) {
                Text("Buat Lembar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditSheetHeaderDialog(
    sheet: DailyActivitySheet,
    onDismiss: () -> Unit,
    onSave: (sheetDate: String, shift: String, docNumber: String) -> Unit
) {
    var sheetDate by remember(sheet.id) { mutableStateOf(sheet.sheetDate) }
    var shift by remember(sheet.id) { mutableStateOf(sheet.shift) }
    var docNumber by remember(sheet.id) { mutableStateOf(sheet.docNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Header Lembar", fontWeight = FontWeight.Bold, color = Navy900) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sheetDate,
                    onValueChange = { sheetDate = it },
                    label = { Text("Tanggal (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = docNumber,
                    onValueChange = { docNumber = it },
                    label = { Text("No. Dokumen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Shift:", style = MaterialTheme.typography.labelMedium, color = Navy900)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShiftOption.entries.forEach { opt ->
                        FilterChip(
                            selected = shift == opt.label,
                            onClick = { shift = opt.label },
                            label = { Text(opt.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(sheetDate, shift, docNumber) },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditPersonnelRowDialog(
    isEditing: Boolean,
    initial: PersonnelRow,
    onDismiss: () -> Unit,
    onSave: (PersonnelRow) -> Unit
) {
    var nama by remember { mutableStateOf(initial.nama) }
    var jabatan by remember { mutableStateOf(initial.jabatan) }
    var plot by remember { mutableStateOf(initial.plot) }
    var kehadiran by remember { mutableStateOf(initial.kehadiran) }
    var ket by remember { mutableStateOf(initial.ket) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Baris Personel" else "Tambah Anggota Jaga",
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("NAMA") },
                    placeholder = { Text("Contoh: Bapak Siswandi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = jabatan,
                    onValueChange = { jabatan = it },
                    label = { Text("JABATAN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DANRU", "KA. SHIF", "ANGGOTA").forEach { j ->
                        FilterChip(
                            selected = jabatan.equals(j, ignoreCase = true),
                            onClick = { jabatan = j },
                            label = { Text(j, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = plot,
                        onValueChange = { plot = it },
                        label = { Text("PLOT") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = kehadiran,
                        onValueChange = { kehadiran = it },
                        label = { Text("H / T") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Hadir", "Tidak", "Izin").forEach { h ->
                        FilterChip(
                            selected = kehadiran.equals(h, ignoreCase = true),
                            onClick = { kehadiran = h },
                            label = { Text(h, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = ket,
                    onValueChange = { ket = it },
                    label = { Text("KET (Keterangan)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(PersonnelRow(nama.trim(), jabatan.trim(), plot.trim(), kehadiran.trim(), ket.trim())) },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditInventoryRowDialog(
    isEditing: Boolean,
    initial: InventoryRow,
    onDismiss: () -> Unit,
    onSave: (InventoryRow) -> Unit
) {
    var inventaris by remember { mutableStateOf(initial.inventaris) }
    var jumlah by remember { mutableStateOf(initial.jumlah) }
    var kondisi by remember { mutableStateOf(initial.kondisi) }
    var keterangan by remember { mutableStateOf(initial.keterangan) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Baris Inventaris" else "Tambah Inventaris Pos",
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = inventaris,
                    onValueChange = { inventaris = it },
                    label = { Text("INVENTARIS") },
                    placeholder = { Text("Contoh: HT * Charger * Senter") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = jumlah,
                        onValueChange = { jumlah = it },
                        label = { Text("JUMLAH") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = kondisi,
                        onValueChange = { kondisi = it },
                        label = { Text("KONDISI") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Baik", "1 rusak", "Kurang").forEach { k ->
                        FilterChip(
                            selected = kondisi.equals(k, ignoreCase = true),
                            onClick = { kondisi = k },
                            label = { Text(k, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("KETERANGAN") },
                    placeholder = { Text("Lengkap / catatan kondisi barang...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(InventoryRow(inventaris.trim(), jumlah.trim(), kondisi.trim(), keterangan.trim())) },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun EditActivityRowDialog(
    isEditing: Boolean,
    initial: ActivityTableRow,
    onDismiss: () -> Unit,
    onSave: (ActivityTableRow) -> Unit
) {
    var jam by remember { mutableStateOf(initial.jam) }
    var uraian by remember { mutableStateOf(initial.uraian) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Uraian Kegiatan" else "Tambah Uraian Kegiatan",
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = jam,
                        onValueChange = { jam = it },
                        label = { Text("JAM") },
                        placeholder = { Text("15.00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            jam = SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date())
                        }
                    ) {
                        Text("Kini", style = MaterialTheme.typography.labelMedium)
                    }
                }
                OutlinedTextField(
                    value = uraian,
                    onValueChange = { uraian = it },
                    label = { Text("URAIAN KEGIATAN") },
                    placeholder = { Text("Contoh: Petugas jaga melaksanakan standby di Portal utama BPS. Situasi aman kondusif.") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(ActivityTableRow(jam.trim(), uraian.trim())) },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
