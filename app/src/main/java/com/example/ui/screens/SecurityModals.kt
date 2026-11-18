package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DEFAULT_ROOM_CODE
import com.example.data.model.OfficerProfile
import com.example.data.model.OfficerRoles
import com.example.data.model.RoomMember
import com.example.media.ImageUtils
import com.example.ui.theme.Emerald600
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
import com.example.ui.theme.Slate800

@Composable
fun WhatsAppRecapDialog(
    recapText: String,
    onDismiss: () -> Unit,
    onCopyText: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy900)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Emerald600
                        )
                        Text(
                            text = "Rekap Siap Kirim WhatsApp",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Format teks di bawah disusun otomatis dari 3 kategori laporan harian Plot BPS:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 340.dp)
                    ) {
                        Text(
                            text = recapText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate800,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate50)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Tutup")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onCopyText,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        modifier = Modifier.testTag("copy_wa_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Teks Laporan")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOfficerDialog(
    profile: OfficerProfile,
    onDismiss: () -> Unit,
    onPickProfilePhoto: () -> Unit,
    onRemoveProfilePhoto: () -> Unit,
    onSave: (name: String, nrp: String, role: String) -> Unit
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var nrp by remember(profile) { mutableStateOf(profile.nrp) }
    var role by remember(profile) { mutableStateOf(profile.role) }
    var expanded by remember { mutableStateOf(false) }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = Gold400
                        )
                        Text(
                            text = "Identitas & Foto Profil",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Photo Picker Section
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OfficerAvatarWithPresence(
                                name = name.ifBlank { profile.name },
                                isOnline = true,
                                photoData = profile.photoData,
                                size = 60.dp,
                                containerColor = Navy800,
                                textColor = Gold400,
                                borderColor = Gold500,
                                ringColor = Slate50
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Foto Profil Petugas",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Navy900
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onPickProfilePhoto,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Navy900,
                                            contentColor = Gold400
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("change_profile_photo_button")
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ganti Foto", style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (profile.photoData.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = onRemoveProfilePhoto,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("remove_profile_photo_button")
                                        ) {
                                            Text("Hapus", style = MaterialTheme.typography.labelSmall, color = Red600)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 80) name = it },
                        label = { Text("Nama Lengkap Petugas") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("officer_name_input")
                    )

                    OutlinedTextField(
                        value = nrp,
                        onValueChange = { if (it.length <= 40) nrp = it },
                        label = { Text("NRP / Nomor ID Anggota") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("officer_nrp_input")
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = role,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jabatan / Plot Tugas") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            OfficerRoles.options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        role = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank() && nrp.isNotBlank()) {
                                    onSave(name.trim(), nrp.trim(), role)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Navy900,
                                contentColor = Gold400
                            ),
                            modifier = Modifier.testTag("save_officer_button")
                        ) {
                            Text("Simpan Identitas")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResetShiftConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Red100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Red600
                )
            }
        },
        title = {
            Text(
                text = "Reset Data Laporan Shift?",
                style = MaterialTheme.typography.titleMedium,
                color = Navy900
            )
        },
        text = {
            Text(
                text = "Semua rincian kegiatan dan foto pada laporan saat ini akan dihapus untuk memulai pergantian shift baru.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate700
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Red600),
                modifier = Modifier.testTag("confirm_reset_button")
            ) {
                Text("Ya, Reset Data")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun PhotoLightboxDialog(
    base64Image: String,
    caption: String? = null,
    onDismiss: () -> Unit
) {
    val imageBitmap = remember(base64Image) {
        ImageUtils.decodeBase64ToImageBitmap(base64Image)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tutup Foto")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = caption?.ifBlank { "Dokumentasi Lapangan" } ?: "Dokumentasi Lapangan",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                if (!caption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Navy900.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Gold500.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoSourcePickerDialog(
    onDismiss: () -> Unit,
    onPickCamera: () -> Unit,
    onPickGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tambah Dokumentasi Foto",
                style = MaterialTheme.typography.titleMedium,
                color = Navy900
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pilih sumber foto bukti lapangan. Foto otomatis diberi watermark Plot BPS & jam WIB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
                Surface(
                    onClick = onPickCamera,
                    shape = RoundedCornerShape(12.dp),
                    color = Navy900,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_camera_option")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Gold400)
                        Column {
                            Text("Ambil Foto Kamera", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Gunakan kamera perangkat secara langsung", style = MaterialTheme.typography.bodySmall, color = Slate100)
                        }
                    }
                }
                Surface(
                    onClick = onPickGallery,
                    shape = RoundedCornerShape(12.dp),
                    color = Slate100,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_gallery_option")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Navy800)
                        Column {
                            Text("Pilih dari Galeri", color = Navy900, fontWeight = FontWeight.Bold)
                            Text("Pilih foto dokumentasi yang sudah ada", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun JoinOrCreateRoomDialog(
    currentRoomCode: String,
    isBusy: Boolean,
    onGenerateCode: () -> String,
    onCopyCode: (String) -> Unit,
    onJoinRoom: (String) -> Unit,
    onCreateRoom: (code: String, title: String) -> Unit,
    onDismiss: () -> Unit
) {
    var modeJoin by remember { mutableStateOf(true) }
    var joinCodeInput by remember { mutableStateOf("") }
    var createCodeInput by remember { mutableStateOf(onGenerateCode()) }
    var createTitleInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Gold400)
                        Text(
                            text = "Ruang Live Chat (Kode)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current room code banner
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Kode Ruang Aktif Saat Ini",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500
                                )
                                Text(
                                    text = currentRoomCode,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Navy900
                                )
                            }
                            OutlinedButton(
                                onClick = { onCopyCode(currentRoomCode) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { modeJoin = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (modeJoin) Navy900 else Slate100,
                            border = BorderStroke(1.dp, if (modeJoin) Gold500 else Slate200),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Login,
                                    contentDescription = null,
                                    tint = if (modeJoin) Gold400 else Slate700,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gabung Kode",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (modeJoin) Color.White else Slate700
                                )
                            }
                        }
                        Surface(
                            onClick = { modeJoin = false },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!modeJoin) Navy900 else Slate100,
                            border = BorderStroke(1.dp, if (!modeJoin) Gold500 else Slate200),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = if (!modeJoin) Gold400 else Slate700,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Buat Ruang",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (!modeJoin) Color.White else Slate700
                                )
                            }
                        }
                    }

                    if (modeJoin) {
                        Text(
                            text = "Masukkan kode ruang yang dibagikan rekan atau Danru untuk terhubung online secara real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                        OutlinedTextField(
                            value = joinCodeInput,
                            onValueChange = { joinCodeInput = it.uppercase().take(24) },
                            label = { Text("Kode Ruang (contoh: PLOT-BPS)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_room_code_input")
                        )
                        if (currentRoomCode != DEFAULT_ROOM_CODE) {
                            TextButton(
                                onClick = { onJoinRoom(DEFAULT_ROOM_CODE) },
                                enabled = !isBusy
                            ) {
                                Text("Kembali ke Ruang Utama ($DEFAULT_ROOM_CODE)")
                            }
                        }
                        Button(
                            onClick = { onJoinRoom(joinCodeInput) },
                            enabled = !isBusy && joinCodeInput.trim().length >= 3,
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_join_room_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Gold400, strokeWidth = 2.dp)
                            } else {
                                Text("Gabung ke Ruang Live Chat", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Buat ruang koordinasi baru lalu bagikan kodenya ke anggota regu yang bertugas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                        OutlinedTextField(
                            value = createTitleInput,
                            onValueChange = { if (it.length <= 60) createTitleInput = it },
                            label = { Text("Nama Ruang (mis. Regu Shift 2 BPS)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_room_title_input")
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = createCodeInput,
                                onValueChange = { createCodeInput = it.uppercase().take(24) },
                                label = { Text("Kode Ruang (3-24 karakter)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("create_room_code_input")
                            )
                            OutlinedButton(
                                onClick = { createCodeInput = onGenerateCode() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Acak Kode")
                            }
                        }
                        Button(
                            onClick = { onCreateRoom(createCodeInput, createTitleInput) },
                            enabled = !isBusy && createCodeInput.trim().length >= 3 && createTitleInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_create_room_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Gold400, strokeWidth = 2.dp)
                            } else {
                                Text("Buat & Gabung Ruang", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoomMembersDialog(
    roomCode: String,
    roomTitle: String,
    members: List<RoomMember>,
    currentUserId: String,
    onDismiss: () -> Unit
) {
    val onlineCount = members.count { it.isCurrentlyOnline() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Gold400)
                        Column {
                            Text(
                                text = "Anggota Ruang ($roomCode)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = "$onlineCount online dari ${members.size} tergabung",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold400
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                if (members.isEmpty()) {
                    Text(
                        text = "Belum ada anggota terdaftar di ruang $roomTitle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        modifier = Modifier.padding(20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(members, key = { it.userId }) { member ->
                            val online = member.isCurrentlyOnline()
                            Surface(
                                color = Slate50,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (online) Emerald600.copy(alpha = 0.35f) else Slate200)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OfficerAvatarWithPresence(
                                        name = member.name,
                                        isOnline = online,
                                        photoData = member.photoData,
                                        size = 42.dp,
                                        containerColor = if (online) Navy800 else Slate200,
                                        textColor = if (online) Gold400 else Slate700,
                                        borderColor = if (online) Gold500 else Slate400,
                                        ringColor = Slate50
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = member.name + if (member.userId == currentUserId) " (Anda)" else "",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Navy900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${member.role} • ${member.activeShift}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate500,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "NRP: ${member.nrp}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Navy700
                                        )
                                    }
                                    OnlineStatusPill(isOnline = online)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}
