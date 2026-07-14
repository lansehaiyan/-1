package com.phonetime.manager.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonetime.manager.TimeManager

/**
 * 儿童界面 - 显示剩余使用时间
 * 大数字倒计时，友好有趣
 */
@Composable
fun ChildScreen(
    remainingMs: Long,
    totalMs: Long,
    usedMs: Long,
    isLocked: Boolean,
    timeManager: TimeManager,
    onSettingsClick: () -> Unit
) {
    val progress = if (totalMs > 0) {
        (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayText = if (isLocked) {
        "时间到了！"
    } else {
        timeManager.formatTime(remainingMs)
    }

    val displayDesc = if (isLocked) {
        "今天的手机时间已用完\n放下手机去玩吧！"
    } else {
        "今天还有 ${timeManager.formatTimeReadable(remainingMs)}\n可以使用"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 可爱表情
            val emoji = when {
                isLocked -> "😴"
                remainingMs < 60_000 -> "⚠️"
                remainingMs < 5 * 60_000 -> "😅"
                remainingMs < 15 * 60_000 -> "🙂"
                else -> "😊"
            }

            Text(
                text = emoji,
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 副标题
            Text(
                text = if (isLocked) "今天的手机时间已用完" else "今天还能玩",
                fontSize = 18.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 大号时间
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLocked) Color(0xFFFF5252)
                        else if (remainingMs < 60_000) Color(0xFFFF9800)
                        else Color(0xFF4CAF50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!isLocked) {
                        Text(
                            text = if (remainingMs > 60 * 1000) "剩余时间" else "即将用完",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 进度条
            if (!isLocked && totalMs > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (remainingMs < 60_000) Color(0xFFFF5252)
                    else if (remainingMs < 5 * 60_000) Color(0xFFFF9800)
                    else Color(0xFF4CAF50),
                    trackColor = Color.LightGray,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "已用 ${timeManager.formatTimeReadable(usedMs)} / 共 ${timeManager.formatTimeReadable(totalMs)}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 家长入口
            OutlinedButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("家长设置", fontSize = 16.sp)
            }
        }
    }
}
