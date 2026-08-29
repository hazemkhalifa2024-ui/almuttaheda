package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusAmberBgDark
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusBlueBg
import com.example.ui.theme.StatusBlueBgDark
import com.example.ui.theme.StatusDelivered
import com.example.ui.theme.StatusDeliveredBg
import com.example.ui.theme.StatusDeliveredBgDark
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusEmeraldBg
import com.example.ui.theme.StatusEmeraldBgDark

data class StatusVisual(
    val label: String,
    val dotColor: Color,
    val textColor: Color,
    val bgColorLight: Color,
    val bgColorDark: Color
)

fun getStatusVisual(status: String): StatusVisual {
    return when (status.lowercase()) {
        "received", "تم الاستلام" -> StatusVisual(
            label = "تم الاستلام",
            dotColor = StatusBlue,
            textColor = StatusBlue,
            bgColorLight = StatusBlueBg,
            bgColorDark = StatusBlueBgDark
        )
        "in_progress", "جاري الفحص", "جارى الإصلاح", "جاري الإصلاح" -> StatusVisual(
            label = "جاري الإصلاح",
            dotColor = StatusAmber,
            textColor = StatusAmber,
            bgColorLight = StatusAmberBg,
            bgColorDark = StatusAmberBgDark
        )
        "ready", "جاهز للتسليم" -> StatusVisual(
            label = "جاهز للتسليم",
            dotColor = StatusEmerald,
            textColor = StatusEmerald,
            bgColorLight = StatusEmeraldBg,
            bgColorDark = StatusEmeraldBgDark
        )
        "delivered", "تم التسليم" -> StatusVisual(
            label = "تم التسليم",
            dotColor = StatusDelivered,
            textColor = StatusDelivered,
            bgColorLight = StatusDeliveredBg,
            bgColorDark = StatusDeliveredBgDark
        )
        else -> StatusVisual(
            label = status,
            dotColor = StatusBlue,
            textColor = StatusBlue,
            bgColorLight = StatusBlueBg,
            bgColorDark = StatusBlueBgDark
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val visual = getStatusVisual(status)
    val bg = if (isDark) visual.bgColorDark else visual.bgColorLight

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(visual.dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = visual.label,
            color = visual.textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
