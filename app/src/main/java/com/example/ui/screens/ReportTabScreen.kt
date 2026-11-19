package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ReportCategory
import com.example.data.model.ReportStatus
import com.example.data.model.SecurityReport
import com.example.data.model.ShiftOption
import com.example.data.model.TimeKeywordFilter
import com.example.data.model.UiState
import com.example.media.ImageUtils
import com.example.ui.theme.Blue800
import com.example.ui.theme.Blue800Text
import com.example.ui.theme.Blue900
import com.example.ui.theme.Blue950
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald800
import com.example.ui.theme.IceBlue100
import com.example.ui.theme.IceBlue50
import com.example.ui.theme.Navy100
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
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
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.viewmodel.ReportFormState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReportTabScreen(
    formState: ReportFormState,
    reportsState: UiState<List<SecurityReport>>,
    reportFilter: TimeKeywordFilter,
    activeShift: String,
    currentUserId: String = "",
    isRoomCreator: Boolean = false,
    isExplicitSaving: Boolean,
    onSelectCategory: (Int) -> Unit,
    onTimeChange: (String) -> Unit,
    onReportDateChange: (String) -> Unit,
    onSetTimeNow: () -> Unit,
    onFormShiftChange: (String) -> Unit,
    onFormStatusChange: (ReportStatus) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onFillTemplate: () -> Unit,
    onRequestPhotoForSlot: (Int) -> Unit,
    onPhotoCaptionChange: (Int, String) -> Unit,
    onRemovePhotoAtSlot: (Int) -> Unit,
    onOpenLightbox: (base64: String, caption: String?) -> Unit,
    onSubmitReport: () -> Unit,
    onExplicitSave: () -> Unit,
    onDeleteReport: (String) -> Unit,
    onOpenResetConfirm: () -> Unit,
    onOpenRecap: () -> Unit,
    onPrintPdf: () -> Unit,
    onFilterKeywordChange: (String) -> Unit,
    onFilterHourChange: (Int?) -> Unit,
    onFilterDayChange: (Int?) -> Unit,
    onFilterMonthChange: (Int?) -> Unit,
    onFilterYearChange: (Int?) -> Unit,
    onFilterShiftChange: (String?) -> Unit,
    onFilterToday: () -> Unit,
    onResetFilter: () -> Unit,
    onOpenGlobalRecap: () -> Unit,
    totalRecapCount: Int
) {
    val reportsList = (reportsState as? UiState.Success)?.data.orEmpty()
    val filteredReports = remember(reportsList, reportFilter) {
        reportsList.filter { reportFilter.matchesReport(it) }
    }
    val availableYears = remember(reportsList) {
        reportsList.mapNotNull { it.resolvedYearMonthDay()?.first }.distinct()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Quick Actions Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.74f)),
                border = BorderStroke(1.dp, Sky300.copy(alpha = 0.45f))
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
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Sky500
                        )
                        Text(
                            text = "Input Rincian Kegiatan (3 Kategori Wajib)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Blue900
                        )
                    }
                    Text(
                        text = "Semua laporan & foto di ruang ini langsung tersinkron dan dapat dipantau seluruh anggota yang login.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenRecap,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("open_recap_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.rekap_wa), style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onPrintPdf,
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("print_pdf_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.cetak_pdf), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 2. Category Selector + Report Entry Form Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.74f)),
                border = BorderStroke(1.dp, Sky300.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    // Category selector header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Blue900.copy(alpha = 0.82f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PILIH KATEGORI LAPORAN HARIAN (1 - 3):",
                            style = MaterialTheme.typography.labelSmall,
                            color = Sky300,
                            fontWeight = FontWeight.Bold
                        )
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isWide = maxWidth >= 600.dp
                            if (isWide) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ReportCategory.entries.forEach { cat ->
                                        CategorySelectCard(
                                            category = cat,
                                            selected = formState.selectedCategory == cat.number,
                                            onClick = { onSelectCategory(cat.number) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ReportCategory.entries.forEach { cat ->
                                        CategorySelectCard(
                                            category = cat,
                                            selected = formState.selectedCategory == cat.number,
                                            onClick = { onSelectCategory(cat.number) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Form body
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Date + Time + Kini button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = formState.reportDateInput,
                                onValueChange = { if (it.length <= 10) onReportDateChange(it) },
                                label = { Text("Tanggal (yyyy-MM-dd)") },
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Navy700)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("report_date_input")
                            )
                            OutlinedTextField(
                                value = formState.timeInput,
                                onValueChange = { if (it.length <= 10) onTimeChange(it) },
                                label = { Text("Jam (WIB)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Navy700)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("time_input")
                            )
                            OutlinedButton(
                                onClick = onSetTimeNow,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("set_time_now_button")
                            ) {
                                Text(stringResource(R.string.set_now), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Shift & Status dropdowns
                        var shiftExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = shiftExpanded,
                            onExpandedChange = { shiftExpanded = !shiftExpanded }
                        ) {
                            val selectedShiftOpt = ShiftOption.fromLabel(formState.shiftInput)
                            OutlinedTextField(
                                value = "${selectedShiftOpt.label} (${selectedShiftOpt.timeRange})",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Keterangan Shift") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("report_form_shift_selector")
                            )
                            ExposedDropdownMenu(
                                expanded = shiftExpanded,
                                onDismissRequest = { shiftExpanded = false }
                            ) {
                                ShiftOption.entries.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text("${opt.label} (${opt.timeRange})") },
                                        onClick = {
                                            onFormShiftChange(opt.label)
                                            shiftExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        var statusExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = !statusExpanded }
                        ) {
                            OutlinedTextField(
                                value = formState.statusInput.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status Kondisi") },
                                leadingIcon = {
                                    StatusDot(status = formState.statusInput)
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                ReportStatus.entries.forEach { st ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                StatusDot(status = st)
                                                Text(st.label)
                                            }
                                        },
                                        onClick = {
                                            onFormStatusChange(st)
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Description + Template button
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rincian Kegiatan / Kejadian Lapangan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Slate700
                                )
                                TextButton(
                                    onClick = onFillTemplate,
                                    modifier = Modifier.testTag("fill_template_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.fill_template),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Navy700
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = formState.descriptionInput,
                                onValueChange = { if (it.length <= 2000) onDescriptionChange(it) },
                                placeholder = {
                                    Text("Tuliskan detail pelaksanaan tugas, lokasi pengecekan di area BPS, jumlah personil/tamu, atau temuan lapangan...")
                                },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("description_input")
                            )
                        }

                        // 4-Slot Photo Upload Box + Caption Column Under Every Photo
                        val filledPhotoCount = formState.photoSlots.count { it != null }
                        Surface(
                            color = IceBlue50.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Sky300.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Sky600,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Dokumentasi Foto & Keterangan (Maks. 4)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Blue900
                                        )
                                    }
                                    Surface(
                                        color = Navy100,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "$filledPhotoCount / 4 Foto",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Navy800,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val columns = if (maxWidth >= 560.dp) 2 else 2
                                    if (maxWidth < 330.dp) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            for (idx in 0..3) {
                                                val photo = formState.photoSlots[idx]
                                                val caption = formState.photoCaptionSlots.getOrElse(idx) { "" }
                                                PhotoSlotWithCaptionTile(
                                                    slotIndex = idx,
                                                    base64Photo = photo,
                                                    caption = caption,
                                                    onCaptionChange = { onPhotoCaptionChange(idx, it) },
                                                    onUploadClick = { onRequestPhotoForSlot(idx) },
                                                    onRemoveClick = { onRemovePhotoAtSlot(idx) },
                                                    onPreviewClick = { if (photo != null) onOpenLightbox(photo, caption) },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            for (rowStart in 0..2 step 2) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    for (offset in 0 until columns) {
                                                        val idx = rowStart + offset
                                                        val photo = formState.photoSlots[idx]
                                                        val caption = formState.photoCaptionSlots.getOrElse(idx) { "" }
                                                        PhotoSlotWithCaptionTile(
                                                            slotIndex = idx,
                                                            base64Photo = photo,
                                                            caption = caption,
                                                            onCaptionChange = { onPhotoCaptionChange(idx, it) },
                                                            onUploadClick = { onRequestPhotoForSlot(idx) },
                                                            onRemoveClick = { onRemovePhotoAtSlot(idx) },
                                                            onPreviewClick = { if (photo != null) onOpenLightbox(photo, caption) },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Ketuk slot untuk upload foto, lalu isi kolom keterangan di bawah masing-masing foto.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                            }
                        }

                        // Submit & Reset buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenResetConfirm,
                                border = BorderStroke(1.dp, Red100),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red600),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("reset_shift_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", style = MaterialTheme.typography.labelMedium)
                            }
                            Button(
                                onClick = onSubmitReport,
                                enabled = !formState.isSubmitting,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Blue900,
                                    contentColor = Sky300
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("submit_report_button")
                            ) {
                                if (formState.isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Sky300,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.save_report),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Filter & Search Panel + Output List Grouped by 3 Categories
        when (reportsState) {
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
                            text = reportsState.message,
                            color = Red800,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                item(key = "report_filter_panel") {
                    SearchAndTimeFilterPanel(
                        title = "Cari & Saring Laporan (Jam / Hari / Bulan / Tahun / Shift)",
                        filter = reportFilter,
                        availableYears = availableYears,
                        totalCount = reportsList.size,
                        filteredCount = filteredReports.size,
                        testTagPrefix = "report",
                        onKeywordChange = onFilterKeywordChange,
                        onHourChange = onFilterHourChange,
                        onDayChange = onFilterDayChange,
                        onMonthChange = onFilterMonthChange,
                        onYearChange = onFilterYearChange,
                        onShiftChange = onFilterShiftChange,
                        onSelectToday = onFilterToday,
                        onReset = onResetFilter,
                        onOpenGlobalRecap = onOpenGlobalRecap,
                        globalRecapCount = totalRecapCount
                    )
                }

                ReportCategory.entries.forEach { category ->
                    item(key = "cat_card_${category.number}") {
                        val catItems = filteredReports.filter { it.category.toInt() == category.number }
                        CategoryReportSectionCard(
                            category = category,
                            items = catItems,
                            isFiltered = reportFilter.isActive,
                            currentUserId = currentUserId,
                            isRoomCreator = isRoomCreator,
                            onDeleteReport = onDeleteReport,
                            onOpenLightbox = onOpenLightbox
                        )
                    }
                }
            }
        }

        // 4. Dedicated Bottom Save Button (very bottom of Laporan option)
        item(key = "report_bottom_save") {
            val syncedShift = formState.shiftInput.ifBlank { activeShift }
            val syncedTime = formState.timeInput.trim().ifBlank { "--:--" }
            val saveLabel = if (formState.descriptionInput.trim().isNotEmpty()) {
                "Simpan & Tambahkan Laporan ($syncedShift)"
            } else {
                "Simpan Data Laporan ($syncedShift)"
            }
            BottomOptionSaveCard(
                buttonLabel = saveLabel,
                statusSubtitle = "Simpan catatan laporan & foto ke cloud ($syncedShift • $syncedTime WIB • ${reportsList.size} entri tersimpan)",
                isSaving = isExplicitSaving || formState.isSubmitting,
                testTag = "bottom_save_laporan_button",
                onSaveClick = onExplicitSave,
                onOpenRecapSearch = onOpenGlobalRecap,
                recapCount = totalRecapCount
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CategorySelectCard(
    category: ReportCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Blue800.copy(alpha = 0.85f) else Blue950.copy(alpha = 0.62f),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Sky400 else Blue800),
        modifier = modifier.testTag("category_select_${category.number}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Sky500),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Column {
                Text(
                    text = category.shortTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White else Slate200
                )
                Text(
                    text = category.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }
        }
    }
}

@Composable
private fun StatusDot(status: ReportStatus) {
    val color = when (status) {
        ReportStatus.AMAN -> Emerald600
        ReportStatus.PEMANTAUAN -> Sky500
        ReportStatus.TINDAK_LANJUT -> Red600
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun PhotoSlotWithCaptionTile(
    slotIndex: Int,
    base64Photo: String?,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onUploadClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (base64Photo != null) {
            val imageBitmap = remember(base64Photo) { ImageUtils.decodeBase64ToImageBitmap(base64Photo) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Sky500, RoundedCornerShape(12.dp))
                    .background(Blue900)
                    .clickable(onClick = onPreviewClick)
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Foto ${slotIndex + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Surface(
                    color = Blue950.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "Foto ${slotIndex + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sky300,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Red600)
                        .clickable(onClick = onRemoveClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Foto ${slotIndex + 1}",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Caption field directly below the uploaded photo
            OutlinedTextField(
                value = caption,
                onValueChange = { if (it.length <= 200) onCaptionChange(it) },
                placeholder = {
                    Text(
                        text = "Keterangan foto ${slotIndex + 1}...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                label = {
                    Text(
                        text = "Keterangan Foto ${slotIndex + 1}",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                maxLines = 2,
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.90f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.85f),
                    focusedBorderColor = Sky500,
                    unfocusedBorderColor = Slate200
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("photo_caption_input_$slotIndex")
            )
        } else {
            Surface(
                onClick = onUploadClick,
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.76f),
                border = BorderStroke(1.5.dp, Sky300.copy(alpha = 0.50f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .testTag("photo_slot_$slotIndex")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Navy700,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Slot Foto ${slotIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Slate700,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Ketuk upload",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryReportSectionCard(
    category: ReportCategory,
    items: List<SecurityReport>,
    isFiltered: Boolean,
    currentUserId: String,
    isRoomCreator: Boolean,
    onDeleteReport: (String) -> Unit,
    onOpenLightbox: (base64: String, caption: String?) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.74f)),
        border = BorderStroke(1.dp, Sky300.copy(alpha = 0.45f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(Blue900.copy(alpha = 0.84f), Blue800.copy(alpha = 0.80f)))
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Sky500),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.number.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = category.fullTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Blue800.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Sky400.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "${items.size} Rincian",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sky300,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Sky500)
            )

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (items.isEmpty()) {
                    Surface(
                        color = IceBlue50.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Sky300.copy(alpha = 0.40f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isFiltered) {
                                "Tidak ada rincian Kategori ${category.number} yang cocok dengan kata kunci/filter waktu."
                            } else {
                                "Belum ada rincian kegiatan pada Kategori ${category.number}."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 14.dp)
                        )
                    }
                } else {
                    items.forEach { report ->
                        ReportItemCard(
                            report = report,
                            canDelete = isRoomCreator || currentUserId.isBlank() || report.userId == currentUserId,
                            onDelete = { onDeleteReport(report.id) },
                            onOpenLightbox = onOpenLightbox
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportItemCard(
    report: SecurityReport,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onOpenLightbox: (base64: String, caption: String?) -> Unit
) {
    val (statusBg, statusFg) = when (report.statusEnum) {
        ReportStatus.AMAN -> Emerald100 to Emerald800
        ReportStatus.PEMANTAUAN -> IceBlue100 to Blue800Text
        ReportStatus.TINDAK_LANJUT -> Red100 to Red800
    }
    val dateBadge = remember(report.reportDate, report.createdAt) { report.formattedDateBadge() }

    Surface(
        color = Color.White.copy(alpha = 0.76f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Sky300.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(color = Blue900, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = report.shift,
                            style = MaterialTheme.typography.labelSmall,
                            color = Sky300,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (dateBadge.isNotBlank()) {
                        Surface(color = Color.White.copy(alpha = 0.85f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, Slate200)) {
                            Text(
                                text = dateBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate700,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Surface(color = Navy100, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "${report.time} WIB",
                            style = MaterialTheme.typography.labelSmall,
                            color = Blue900,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(color = statusBg, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = report.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusFg,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Rincian",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate800
            )

            Text(
                text = "Petugas: ${report.officerName} (${report.officerNrp})",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                fontWeight = FontWeight.Medium
            )

            if (report.photos.isNotEmpty()) {
                HorizontalDivider(color = Slate200)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Sky600,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Dokumentasi Foto (${report.photos.size}/4):",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }

                // Display photos 2 per row with each photo's caption directly underneath
                val photos = report.photos.take(4)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (rowStart in photos.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            for (offset in 0..1) {
                                val idx = rowStart + offset
                                if (idx < photos.size) {
                                    val base64 = photos[idx]
                                    val caption = report.photoCaptions.getOrNull(idx)?.trim().orEmpty()
                                    val bmp = remember(base64) { ImageUtils.decodeBase64ToImageBitmap(base64) }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1.25f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                                                .background(Slate100)
                                                .clickable { onOpenLightbox(base64, caption.ifBlank { null }) }
                                        ) {
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp,
                                                    contentDescription = caption.ifBlank { "Bukti Foto ${idx + 1}" },
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        // Caption box directly under each photo
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Slate200),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = caption.ifBlank { "Foto ${idx + 1}: Tanpa keterangan" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (caption.isNotBlank()) Slate800 else Slate400,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
