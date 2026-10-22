package com.example.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MeasurementRecord
import com.example.domain.MeasureUnit
import com.example.domain.PhotogrammetryEngine
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.LevelEmerald
import com.example.ui.theme.PrecisionAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningCoral

@Composable
fun MeasurementHistoryScreen(
    records: List<MeasurementRecord>,
    selectedUnit: MeasureUnit,
    onDeleteRecord: (Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = LaserCyan)
                Text(
                    text = "سجل القياسات المحفوظة (${records.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
        }

        if (records.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SlateCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = LaserCyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "لا توجد قياسات محفوظة بعد",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "التقط صورة لأي جسم أو قس أبعاده في استوديو الصور ثم اضغط على زر الحفظ للرجوع إليها لاحقاً",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("history_list")
            ) {
                items(records, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, SlateCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_item_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = LevelEmerald.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, LevelEmerald.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "دقة ${item.confidencePercent}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = LevelEmerald,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteRecord(item.id) },
                                    modifier = Modifier.testTag("delete_record_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "حذف القياس",
                                        tint = WarningCoral
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = PhotogrammetryEngine.formatLength(item.primaryLengthCm, selectedUnit),
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = PrecisionAmber
                                        )
                                    )
                                    if (item.secondaryLengthCm != null) {
                                        val secFormatted = PhotogrammetryEngine.formatLength(item.secondaryLengthCm, selectedUnit)
                                        val areaCm2 = item.primaryLengthCm * item.secondaryLengthCm
                                        Text(
                                            text = "البعد الثاني: $secFormatted • المساحة: ${String.format(java.util.Locale.US, "%.1f", areaCm2)} سم²",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = LaserCyan
                                        )
                                    }
                                }

                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", item.primaryLengthCm * 10)} ملم | ${String.format(java.util.Locale.US, "%.2f", item.primaryLengthCm / 2.54)} إنش",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }

                            Surface(
                                color = SlateSurfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "${item.calibrationMethod} • ${item.referenceDetails}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
