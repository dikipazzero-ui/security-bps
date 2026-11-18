package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DEFAULT_ROOM_CODE
import com.example.data.model.IndonesianMonths
import com.example.data.model.NoteSourceTab
import com.example.data.model.ShiftOption
import com.example.data.model.TimeKeywordFilter
import com.example.data.model.UnifiedNoteItem
import com.example.data.model.currentIsoDate
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
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.viewmodel.currentFormattedTime
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlobalNotesSearchDialog(
    allNotes: List<UnifiedNoteItem>,
    activeRoomCode: String = DEFAULT_ROOM_CODE,
    currentUserId: String = "",
    isRoomCreator: Boolean = false,
    defaultShift: String,
    isSavingNote: Boolean,
    onDismiss: () -> Unit,
    onOpenTarget: (UnifiedNoteItem) -> Unit,
    onSaveQuickNote: (
        title: String,
        content: String,
        sourceTab: NoteSourceTab,
        shift: String,
        noteDate: String,
        noteTime: String,
        onSuccess: () -> Unit
    ) -> Unit,
    onDeleteQuickNote: (String) -> Unit
) {
    var filter by remember { mutableStateOf(TimeKeywordFilter()) }
    var sourceFilter by remember { mutableStateOf<NoteSourceTab?>(null) }
    var showAddForm by remember { mutableStateOf(false) }

    // Quick-add form states
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var newShift by remember(defaultShift) { mutableStateOf(defaultShift) }
    var newSource by remember { mutableStateOf(NoteSourceTab.CATATAN) }
    var newDate by remember { mutableStateOf(currentIsoDate()) }
    var newTime by remember { mutableStateOf(currentFormattedTime()) }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val yearChoices = remember(allNotes, currentYear) {
        (allNotes.mapNotNull { it.year } + listOf(currentYear, currentYear - 1)).distinct().sortedDescending()
    }

    val filteredNotes = remember(allNotes, filter, sourceFilter) {
        allNotes.filter { it.matches(filter, sourceFilter) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Slate50,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 780.dp)
                .fillMaxHeight(0.92f)
                .padding(horizontal = 10.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Navy Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy900)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
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
                            Icon(
                                imageVector = Icons.Default.ManageSearch,
                                contentDescription = null,
                                tint = Gold400,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Filter Hasil Rekap Penyimpanan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${filteredNotes.size} dari ${allNotes.size} data Ruang $activeRoomCode • Dipantau Semua Anggota",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gold400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                onClick = { showAddForm = !showAddForm },
                                color = if (showAddForm) Color.White else Gold500,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("toggle_quick_add_note_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Navy950,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (showAddForm) "Tutup" else "Catatan Baru",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Navy950
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("close_global_notes_dialog")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup Pencarian Catatan",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Live keyword search bar
                    OutlinedTextField(
                        value = filter.keyword,
                        onValueChange = { filter = filter.copy(keyword = it) },
                        placeholder = {
                            Text(
                                text = "Cari hasil simpan, catatan, PJU, laporan, absensi, atau jam...",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Navy700)
                        },
                        trailingIcon = {
                            if (filter.keyword.isNotEmpty()) {
                                IconButton(onClick = { filter = filter.copy(keyword = "") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Bersihkan pencarian", tint = Slate500)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Gold500,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("global_notes_search_input")
                    )
                }

                // Filter Strip: Shift Chips + Source Tabs + Jam/Hari/Bulan/Tahun Dropdowns
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Shift Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShiftFilterPill(
                            label = "Semua Shift",
                            selected = filter.shift == null,
                            onClick = { filter = filter.copy(shift = null) }
                        )
                        ShiftOption.entries.forEach { opt ->
                            ShiftFilterPill(
                                label = "${opt.label} (${opt.timeRange})",
                                selected = filter.shift == opt.label,
                                onClick = {
                                    filter = filter.copy(
                                        shift = if (filter.shift == opt.label) null else opt.label
                                    )
                                }
                            )
                        }
                    }

                    // Source Option Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SourceFilterPill(
                            label = "Semua Opsi (${allNotes.size})",
                            selected = sourceFilter == null,
                            onClick = { sourceFilter = null }
                        )
                        NoteSourceTab.entries.forEach { tab ->
                            val count = allNotes.count { it.source == tab }
                            SourceFilterPill(
                                label = "${tab.label} ($count)",
                                selected = sourceFilter == tab,
                                onClick = { sourceFilter = if (sourceFilter == tab) null else tab }
                            )
                        }
                    }

                    // Jam, Hari, Bulan, Tahun dropdowns
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterDropdownChip(
                            label = filter.hour?.let { "Jam ${it.toString().padStart(2, '0')}" } ?: "Semua Jam",
                            isSelected = filter.hour != null,
                            testTag = "global_hour_filter",
                            options = listOf(null to "Semua Jam") + (0..23).map { h ->
                                h to "Jam ${h.toString().padStart(2, '0')}:00"
                            },
                            onSelect = { filter = filter.copy(hour = it) }
                        )
                        FilterDropdownChip(
                            label = filter.day?.let { "Tgl ${it.toString().padStart(2, '0')}" } ?: "Semua Tgl",
                            isSelected = filter.day != null,
                            testTag = "global_day_filter",
                            options = listOf(null to "Semua Tanggal") + (1..31).map { d ->
                                d to "Tanggal ${d.toString().padStart(2, '0')}"
                            },
                            onSelect = { filter = filter.copy(day = it) }
                        )
                        FilterDropdownChip(
                            label = filter.month?.let { IndonesianMonths.shortNames.getOrElse(it - 1) { "$it" } } ?: "Semua Bulan",
                            isSelected = filter.month != null,
                            testTag = "global_month_filter",
                            options = listOf(null to "Semua Bulan") + IndonesianMonths.fullNames.mapIndexed { idx, name ->
                                (idx + 1) to name
                            },
                            onSelect = { filter = filter.copy(month = it) }
                        )
                        FilterDropdownChip(
                            label = filter.year?.toString() ?: "Semua Tahun",
                            isSelected = filter.year != null,
                            testTag = "global_year_filter",
                            options = listOf(null to "Semua Tahun") + yearChoices.map { it to it.toString() },
                            onSelect = { filter = filter.copy(year = it) }
                        )
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                filter = filter.copy(
                                    day = cal.get(Calendar.DAY_OF_MONTH),
                                    month = cal.get(Calendar.MONTH) + 1,
                                    year = cal.get(Calendar.YEAR)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, Navy100)
                        ) {
                            Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(13.dp), tint = Navy800)
                            Spacer(Modifier.width(4.dp))
                            Text("Hari Ini", style = MaterialTheme.typography.labelSmall, color = Navy800)
                        }
                        if (filter.isActive || sourceFilter != null) {
                            TextButton(
                                onClick = {
                                    filter = TimeKeywordFilter()
                                    sourceFilter = null
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Reset", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                // Collapsible Quick-Add Note Card
                AnimatedVisibility(visible = showAddForm) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Gold500),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Simpan Catatan Baru (Otomatis Terindeks Waktu & Shift)",
                                style = MaterialTheme.typography.labelLarge,
                                color = Navy900,
                                fontWeight = FontWeight.ExtraBold
                            )
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { if (it.length <= 120) newTitle = it },
                                label = { Text("Judul Catatan (opsional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newContent,
                                onValueChange = { if (it.length <= 2000) newContent = it },
                                label = { Text("Isi Catatan") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quick_note_content_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newDate,
                                    onValueChange = { if (it.length <= 10) newDate = it },
                                    label = { Text("Tanggal") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                                OutlinedTextField(
                                    value = newTime,
                                    onValueChange = { if (it.length <= 8) newTime = it },
                                    label = { Text("Jam WIB") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ShiftOption.entries.forEach { opt ->
                                    ShiftFilterPill(
                                        label = opt.label,
                                        selected = newShift == opt.label,
                                        onClick = { newShift = opt.label }
                                    )
                                }
                                NoteSourceTab.entries.forEach { tab ->
                                    SourceFilterPill(
                                        label = tab.label,
                                        selected = newSource == tab,
                                        onClick = { newSource = tab }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    onSaveQuickNote(
                                        newTitle,
                                        newContent,
                                        newSource,
                                        newShift,
                                        newDate,
                                        newTime
                                    ) {
                                        newTitle = ""
                                        newContent = ""
                                        showAddForm = false
                                    }
                                },
                                enabled = !isSavingNote,
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_quick_note_confirm_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Simpan Catatan Sekarang", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Results List
                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ManageSearch,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Tidak ada catatan yang cocok dengan kata kunci, waktu, atau shift tersebut.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            val canDeleteNote = isRoomCreator || currentUserId.isBlank() || note.creatorUserId.isBlank() || note.creatorUserId == currentUserId
                            UnifiedNoteCard(
                                note = note,
                                onOpenTarget = { onOpenTarget(note) },
                                onDeleteQuickNote = note.quickNoteId
                                    ?.takeIf { canDeleteNote }
                                    ?.let { id -> { onDeleteQuickNote(id) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedNoteCard(
    note: UnifiedNoteItem,
    onOpenTarget: () -> Unit,
    onDeleteQuickNote: (() -> Unit)?
) {
    val shiftOption = remember(note.shift) { ShiftOption.fromLabel(note.shift) }
    val (sourceBg, sourceFg) = when (note.source) {
        NoteSourceTab.LAPORAN -> Navy100 to Navy900
        NoteSourceTab.TABEL -> Emerald100 to Emerald800
        NoteSourceTab.ABSENSI -> Amber100 to Amber800
        NoteSourceTab.PJU -> Navy900 to Gold400
        NoteSourceTab.CHAT -> Slate200 to Navy900
        NoteSourceTab.CATATAN -> Gold500.copy(alpha = 0.25f) to Navy950
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top badges row: Source + Shift (always visible!) + Date/Time WIB
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = sourceBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = note.source.badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = sourceFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Shift Badge with full time range
                Surface(
                    color = Gold500,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${shiftOption.label} (${shiftOption.timeRange})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Navy950,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Explicit Date & Time WIB badge
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Navy800,
                            modifier = Modifier.size(12.dp)
                        )
                        val timePart = if (note.timeWib.isNotBlank()) " • ${note.timeWib} WIB" else ""
                        Text(
                            text = "${note.dateDisplay.ifBlank { note.dateIso }}$timePart",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    }
                }
            }

            Text(
                text = note.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Navy900
            )
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall,
                color = Slate700,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(color = Slate100)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Slate500, modifier = Modifier.size(14.dp))
                    Text(
                        text = note.officerName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onDeleteQuickNote != null) {
                        IconButton(
                            onClick = onDeleteQuickNote,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Catatan", tint = Red600, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (note.source != NoteSourceTab.CATATAN) {
                        OutlinedButton(
                            onClick = onOpenTarget,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Navy700),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = Navy900,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Buka di ${note.source.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Navy900,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Navy900 else Slate100,
        border = BorderStroke(1.dp, if (selected) Gold500 else Slate200)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Gold400 else Slate700,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun SourceFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) Gold500 else Color.White,
        border = BorderStroke(1.dp, if (selected) Gold500 else Slate200)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (selected) Navy950 else Navy900,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}
