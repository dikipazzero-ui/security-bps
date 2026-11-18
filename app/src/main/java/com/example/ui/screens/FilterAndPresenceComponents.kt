package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.IndonesianMonths
import com.example.data.model.TimeKeywordFilter
import com.example.media.ImageUtils
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy100
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import java.util.Calendar

@Composable
fun OfficerAvatarWithPresence(
    name: String,
    isOnline: Boolean,
    photoData: String? = null,
    onClick: (() -> Unit)? = null,
    size: Dp = 36.dp,
    containerColor: Color = Navy700,
    textColor: Color = Gold400,
    borderColor: Color = Gold500.copy(alpha = 0.6f),
    ringColor: Color = Navy900
) {
    val initials = remember(name) {
        name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "PS" }
    }
    val photoBitmap = remember(photoData) {
        if (!photoData.isNullOrBlank()) ImageUtils.decodeBase64ToImageBitmap(photoData) else null
    }
    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(containerColor)
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap,
                    contentDescription = "Foto Profil $name",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
            }
        }
        Box(
            modifier = Modifier
                .size((size * 0.34f).coerceAtLeast(10.dp))
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(if (isOnline) Emerald400 else Slate400)
                .border(1.5.dp, ringColor, CircleShape)
        )
    }
}

@Composable
fun OnlineStatusPill(isOnline: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = if (isOnline) Emerald100 else Slate100,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) Emerald600 else Slate400)
            )
            Text(
                text = if (isOnline) "Online" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isOnline) Emerald800 else Slate500
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchAndTimeFilterPanel(
    title: String,
    filter: TimeKeywordFilter,
    availableYears: List<Int>,
    totalCount: Int,
    filteredCount: Int,
    testTagPrefix: String,
    onKeywordChange: (String) -> Unit,
    onHourChange: (Int?) -> Unit,
    onDayChange: (Int?) -> Unit,
    onMonthChange: (Int?) -> Unit,
    onYearChange: (Int?) -> Unit,
    onSelectToday: (() -> Unit)? = null,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val yearChoices = remember(availableYears, currentYear) {
        (availableYears + listOf(currentYear, currentYear - 1)).distinct().sortedDescending()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier.fillMaxWidth()
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = Navy800,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = Navy900
                    )
                }
                Surface(
                    color = if (filter.isActive) Navy900 else Slate100,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (filter.isActive) "$filteredCount / $totalCount hasil" else "$totalCount data",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (filter.isActive) Gold400 else Slate500,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            OutlinedTextField(
                value = filter.keyword,
                onValueChange = onKeywordChange,
                placeholder = {
                    Text(
                        text = "Cari kata kunci, nama petugas, atau keterangan foto...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Navy700)
                },
                trailingIcon = {
                    if (filter.keyword.isNotEmpty()) {
                        IconButton(onClick = { onKeywordChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus kata kunci", tint = Slate500)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Slate50,
                    focusedBorderColor = Gold500,
                    unfocusedBorderColor = Slate200
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_keyword_input")
            )

            // Granular filters: Jam, Hari, Bulan, Tahun
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterDropdownChip(
                    label = filter.hour?.let { "Jam ${it.toString().padStart(2, '0')}" } ?: "Semua Jam",
                    isSelected = filter.hour != null,
                    testTag = "${testTagPrefix}_hour_filter",
                    options = listOf(null to "Semua Jam") + (0..23).map { h ->
                        h to "Jam ${h.toString().padStart(2, '0')}:00"
                    },
                    onSelect = onHourChange
                )

                FilterDropdownChip(
                    label = filter.day?.let { "Tgl ${it.toString().padStart(2, '0')}" } ?: "Semua Hari",
                    isSelected = filter.day != null,
                    testTag = "${testTagPrefix}_day_filter",
                    options = listOf(null to "Semua Hari") + (1..31).map { d ->
                        d to "Tanggal ${d.toString().padStart(2, '0')}"
                    },
                    onSelect = onDayChange
                )

                FilterDropdownChip(
                    label = filter.month?.let { IndonesianMonths.shortNames.getOrElse(it - 1) { "$it" } } ?: "Semua Bulan",
                    isSelected = filter.month != null,
                    testTag = "${testTagPrefix}_month_filter",
                    options = listOf(null to "Semua Bulan") + IndonesianMonths.fullNames.mapIndexed { idx, name ->
                        (idx + 1) to name
                    },
                    onSelect = onMonthChange
                )

                FilterDropdownChip(
                    label = filter.year?.toString() ?: "Semua Tahun",
                    isSelected = filter.year != null,
                    testTag = "${testTagPrefix}_year_filter",
                    options = listOf(null to "Semua Tahun") + yearChoices.map { y -> y to y.toString() },
                    onSelect = onYearChange
                )

                if (onSelectToday != null) {
                    OutlinedButton(
                        onClick = onSelectToday,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, Navy100)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp), tint = Navy800)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hari Ini", style = MaterialTheme.typography.labelSmall, color = Navy800)
                    }
                }

                if (filter.isActive) {
                    TextButton(
                        onClick = onReset,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("${testTagPrefix}_reset_filter")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Filter", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> FilterDropdownChip(
    label: String,
    isSelected: Boolean,
    testTag: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) Navy900 else Slate50,
            border = BorderStroke(1.dp, if (isSelected) Gold500 else Slate200),
            modifier = Modifier.testTag(testTag)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Gold400 else Slate700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
