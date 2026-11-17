package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.OfficerProfile
import com.example.data.model.OfficerRoles
import com.example.media.ImageUtils
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
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
                            text = "Identitas Petugas BPS",
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
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
                Spacer(modifier = Modifier.height(12.dp))
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Dokumentasi Lapangan",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(16.dp))
                    )
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
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
