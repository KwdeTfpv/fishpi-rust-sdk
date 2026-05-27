package dev.fishpi.mobile.feature.redpacket

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fishpi.mobile.data.RedPacketPreview

@Composable
internal fun DefaultRedPacketCard(
    preview: RedPacketPreview,
    onClick: () -> Unit,
) {
    RedPacketCard(preview = preview, onClick = onClick)
}
