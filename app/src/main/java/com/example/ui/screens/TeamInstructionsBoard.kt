package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.QuickNote
import com.example.data.model.ShiftOption
import com.example.data.model.formatIsoDateBadge
import com.example.ui.theme.Blue800
import com.example.ui.theme.Blue800Text
import com.example.ui.theme.Blue900
import com.example.ui.theme.IceBlue100
import com.example.ui.theme.IceBlue50
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy900
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Red800
import com.example.ui.theme.Sky300
import com.example.ui.theme.Sky400
import com.example.ui.theme.Sky500
import com.example.ui.theme.Sky600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800

private val presetTeamNoteTitles = listOf(
    "Instruksi Atasan",
    "Info Pergantian Shift",
    "Atensi Patroli",
    "Pengumuman Penting"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamInstructionsBoardCard(
    teamNotes: List<QuickNote>,
    activeShift: String,
    currentUserId: String,
    isRoomCreator: Boolean,
    isSaving: Boolean,
    onSaveNote: (noteId: String, title: String, content: String, shift: String, onSuccess: () -> Unit) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }
    var showAllNotes by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<QuickNote?>(null) }
    var deletingNote by remember { mutableStateOf<QuickNote?>(null) }

    val visibleNotes = remember(teamNotes, showAllNotes) {
        if (showAllNotes || teamNotes.size <= 3) teamNotes else teamNotes.take(3)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.78f)),
        border = BorderStroke(1.2.dp, Sky400.copy(alpha = 0.60f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.testTag("team_instructions_board_card")
    ) {
        Column {
            // Sapphire Blue Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Blue900.copy(alpha = 0.88f))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Sky500.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Sky300,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Catatan & Instruksi Atasan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Sky300,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Tampil untuk semua anggota yang login",
                                style = MaterialTheme.typography.labelSmall,
                                color = Sky300
                            )
                        }
                    }
                }
                Surface(
                    color = Sky500.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Sky300.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "${teamNotes.size} Catatan",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Composer Box for Creating Shared Notes / Commander Instructions
                Surface(
                    color = IceBlue50.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Sky300.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Buat Catatan / Instruksi untuk Semua Anggota",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Blue900
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetTeamNoteTitles.forEach { preset ->
                                val selected = titleInput == preset
                                Surface(
                                    onClick = { titleInput = if (selected) "" else preset },
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) Blue900 else Color.White.copy(alpha = 0.9f),
                                    border = BorderStroke(1.dp, if (selected) Sky400 else Sky300.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Sky300 else Blue800Text,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { if (it.length <= 120) titleInput = it },
                            label = { Text("Judul / Perihal (Opsional)") },
                            placeholder = { Text("Misal: Instruksi Danru / Atensi Pimpinan") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("team_note_title_input")
                        )

                        OutlinedTextField(
                            value = contentInput,
                            onValueChange = { if (it.length <= 2000) contentInput = it },
                            label = { Text("Isi Catatan / Instruksi") },
                            placeholder = { Text("Tulis catatan atau instruksi agar langsung terbaca seluruh anggota...") },
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("team_note_content_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = IceBlue100,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$activeShift • Terbaca Semua",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Blue800Text,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    val titleToSave = titleInput.trim().ifBlank { "Instruksi Atasan / Catatan Regu" }
                                    onSaveNote("", titleToSave, contentInput, activeShift) {
                                        titleInput = ""
                                        contentInput = ""
                                    }
                                },
                                enabled = !isSaving && contentInput.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Blue900,
                                    contentColor = Sky300
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("submit_team_note_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bagikan ke Semua",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Sky300.copy(alpha = 0.35f))

                // Shared Notes Feed
                if (teamNotes.isEmpty()) {
                    Surface(
                        color = Slate100.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum ada catatan atau instruksi atasan. Setiap catatan yang dibuat di kolom atas otomatis terlihat oleh seluruh anggota yang login.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        visibleNotes.forEach { note ->
                            val isImportant = note.title.contains("Penting", ignoreCase = true) ||
                                note.title.contains("Instruksi", ignoreCase = true) ||
                                note.title.contains("Atasan", ignoreCase = true)
                            val canModify = isRoomCreator || currentUserId.isBlank() || note.userId == currentUserId
                            Surface(
                                color = if (isImportant) IceBlue50.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.88f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isImportant) Sky500.copy(alpha = 0.55f) else Sky300.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(11.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                color = Blue900,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = note.shift,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Sky300,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Sky600,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = note.officerName.ifBlank { "Petugas Security" },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Navy900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Slate500,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "${formatIsoDateBadge(note.noteDate)} • ${note.noteTime} WIB",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Slate500
                                            )
                                            if (canModify && note.id.isNotBlank()) {
                                                IconButton(
                                                    onClick = { editingNote = note },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Catatan",
                                                        tint = Navy700,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { deletingNote = note },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Hapus Catatan",
                                                        tint = Red600,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = note.title.ifBlank { "Instruksi / Catatan Anggota" },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Blue800
                                    )
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate800
                                    )
                                }
                            }
                        }
                    }

                    if (teamNotes.size > 3) {
                        OutlinedButton(
                            onClick = { showAllNotes = !showAllNotes },
                            border = BorderStroke(1.dp, Sky400.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (showAllNotes) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Blue900,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showAllNotes) "Ringkas Catatan" else "Lihat Semua Catatan (${teamNotes.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = Blue900
                            )
                        }
                    }
                }
            }
        }
    }

    editingNote?.let { target ->
        EditTeamNoteDialog(
            note = target,
            onDismiss = { editingNote = null },
            onSave = { updatedTitle, updatedContent, updatedShift ->
                onSaveNote(target.id, updatedTitle, updatedContent, updatedShift) {
                    editingNote = null
                }
            }
        )
    }

    deletingNote?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingNote = null },
            title = {
                Text(
                    text = "Hapus Catatan / Instruksi?",
                    style = MaterialTheme.typography.titleMedium,
                    color = Navy900
                )
            },
            text = {
                Text(
                    text = "Catatan \"${target.title}\" akan dihapus untuk semua anggota ruang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteNote(target.id)
                        deletingNote = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingNote = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun EditTeamNoteDialog(
    note: QuickNote,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, shift: String) -> Unit
) {
    var title by remember(note) { mutableStateOf(note.title) }
    var content by remember(note) { mutableStateOf(note.content) }
    var selectedShift by remember(note) { mutableStateOf(note.shift) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = IceBlue50.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, Sky300.copy(alpha = 0.5f)),
            shadowElevation = 10.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Blue900.copy(alpha = 0.92f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Edit Catatan / Instruksi",
                        style = MaterialTheme.typography.titleSmall,
                        color = Sky300,
                        fontWeight = FontWeight.Bold
                    )
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
                        value = title,
                        onValueChange = { if (it.length <= 120) title = it },
                        label = { Text("Judul / Perihal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Shift", style = MaterialTheme.typography.labelMedium, color = Blue900)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ShiftOption.entries.forEach { shift ->
                            val active = selectedShift == shift.label
                            Surface(
                                onClick = { selectedShift = shift.label },
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) Blue900 else Slate100.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, if (active) Sky400 else Slate200),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = shift.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) Sky300 else Slate700,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = content,
                        onValueChange = { if (it.length <= 2000) content = it },
                        label = { Text("Isi Catatan / Instruksi") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(title, content, selectedShift) },
                            enabled = content.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue900, contentColor = Sky300)
                        ) {
                            Text("Simpan Perubahan")
                        }
                    }
                }
            }
        }
    }
}
