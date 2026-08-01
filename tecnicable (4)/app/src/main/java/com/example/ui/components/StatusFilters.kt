package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusFilters(
    statuses: List<String>,
    selected: String,
    onTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(statuses) { status ->
            val isSelected = status == selected
            FilterChip(
                selected = isSelected,
                onClick = { onTap(status) },
                label = { Text(status, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF8B5CF6),
                    selectedLabelColor = Color.White,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color.LightGray.copy(alpha = 0.5f),
                    selectedBorderColor = Color(0xFF8B5CF6)
                )
            )
        }
    }
}

@Composable
fun TecnicableStatusFilters(
    statuses: List<String>,
    selected: String,
    onTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    StatusFilters(statuses = statuses, selected = selected, onTap = onTap, modifier = modifier)
}
