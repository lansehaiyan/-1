package com.phonetime.manager.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonetime.manager.DeviceAdminReceiver
import com.phonetime.manager.PreferencesManager
import com.phonetime.manager.TimeLimitService
import com.phonetime.manager.TimeManager

/**
 * 家长设置界面
 * 设置密码、每日时长、额外加时、解锁手机
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSettingsScreen(
    timeManager: TimeManager,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var showPinForm by remember { mutableStateOf(!prefs.isPinSet()) }
    var pinError by remember { mutableStateOf<String?>(null) }

    var selectedMinutes by remember { mutableIntStateOf(prefs.getDailyTimeLimit()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }

    var showSuccess by remember { mutableStateOf<String?>(null) }

    val isDeviceAdminActive by remember {
        derivedStateOf {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as DevicePolicyManager
            val component = ComponentName(context, DeviceAdminReceiver::class.java)
            dpm.isAdminActive(component)
        }
    }

    // 时长选项
    val durationOptions = listOf(
        30 to "30分钟", 60 to "1小时", 90 to "1.5小时",
        120 to "2小时", 180 to "3小时"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部栏
        Surface(
            color = Color(0xFF1A237E),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
                Text(
                    "家长设置",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // ===== 密码设置 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFF1A237E))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (prefs.isPinSet()) "修改解锁密码" else "设置解锁密码",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // 如果已设置密码，先验证旧密码
                    if (prefs.isPinSet() && !showPinForm) {
                        TextButton(onClick = { showPinForm = true }) {
                            Text("修改密码")
                        }
                    }

                    if (showPinForm) {
                        OutlinedTextField(
                            value = pin1,
                            onValueChange = {
                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                    pin1 = it
                                    pinError = null
                                }
                            },
                            label = { Text("4-6位数字密码") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = pinError != null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = pin2,
                            onValueChange = {
                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                    pin2 = it
                                    pinError = null
                                }
                            },
                            label = { Text("确认密码") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = pinError != null,
                            supportingText = {
                                pinError?.let { Text(it, color = Color(0xFFFF5252)) }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                when {
                                    pin1.length < 4 -> pinError = "密码至少4位"
                                    pin1 != pin2 -> pinError = "两次密码不一致"
                                    else -> {
                                        prefs.setPin(pin1)
                                        showSuccess = "✅ 密码设置成功"
                                        showPinForm = false
                                        pin1 = ""
                                        pin2 = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(6.dp))
                            Text("保存密码")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===== 每日时长设置 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = Color(0xFFFF9800))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "每日使用时长",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "当前设置：${timeManager.formatTimeReadable(selectedMinutes * 60 * 1000L)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E)
                    )

                    Spacer(Modifier.height(12.dp))

                    // 时长选项按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationOptions.take(3).forEach { (min, label) ->
                            FilterChip(
                                selected = selectedMinutes == min,
                                onClick = {
                                    selectedMinutes = min
                                    prefs.setDailyTimeLimit(min)
                                    showSuccess = "✅ 已设为 ${label}"
                                },
                                label = { Text(label, fontSize = 13.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationOptions.drop(3).forEach { (min, label) ->
                            FilterChip(
                                selected = selectedMinutes == min,
                                onClick = {
                                    selectedMinutes = min
                                    prefs.setDailyTimeLimit(min)
                                    showSuccess = "✅ 已设为 ${label}"
                                },
                                label = { Text(label, fontSize = 13.sp) }
                            )
                        }

                        FilterChip(
                            selected = durationOptions.none { it.first == selectedMinutes },
                            onClick = { showTimePicker = true },
                            label = { Text("自定义", fontSize = 13.sp) }
                        )
                    }

                    if (showTimePicker) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = customMinutes,
                                onValueChange = { v ->
                                    if (v.isEmpty() || v.all { it.isDigit() }) {
                                        customMinutes = v
                                    }
                                },
                                label = { Text("分钟") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.width(120.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val mins = customMinutes.toIntOrNull()
                                    if (mins != null && mins in 5..480) {
                                        selectedMinutes = mins
                                        prefs.setDailyTimeLimit(mins)
                                        showSuccess = "✅ 已设为 ${mins}分钟"
                                        showTimePicker = false
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("确定")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===== 额外加时 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddCircle, null, tint = Color(0xFF4CAF50))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "额外增加时间",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "孩子时间用完后，可以额外增加",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15 to "+15分钟", 30 to "+30分钟", 60 to "+1小时").forEach { (min, label) ->
                            OutlinedButton(
                                onClick = {
                                    timeManager.addExtraTime(min)
                                    showSuccess = "✅ 已增加${min}分钟"
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(label, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===== 系统权限 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, null, tint = Color(0xFF9C27B0))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "系统权限",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // 设备管理员
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isDeviceAdminActive) {
                                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(
                                            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                            ComponentName(context, DeviceAdminReceiver::class.java)
                                        )
                                        putExtra(
                                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                            "需要设备管理员权限来锁定手机和管理使用时间"
                                        )
                                    }
                                    context.startActivity(intent)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isDeviceAdminActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            null,
                            tint = if (isDeviceAdminActive) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("设备管理员", fontWeight = FontWeight.Medium)
                            Text(
                                if (isDeviceAdminActive) "已激活 ✓" else "未激活 - 点击激活",
                                fontSize = 12.sp,
                                color = if (isDeviceAdminActive) Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }

                    HorizontalDivider()

                    // 电池优化
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BatteryFull, null,
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("忽略电池优化", fontWeight = FontWeight.Medium)
                            Text(
                                "防止系统杀死后台服务",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ===== 重置按钮 =====
            OutlinedButton(
                onClick = {
                    prefs.clearAll()
                    TimeLimitService.stopService(context)
                    showSuccess = "已重置全部设置"
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF5252)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, null)
                Spacer(Modifier.width(8.dp))
                Text("重置所有设置")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // 成功提示
    showSuccess?.let { msg ->
        AlertDialog(
            onDismissRequest = { showSuccess = null },
            confirmButton = {
                TextButton(onClick = { showSuccess = null }) {
                    Text("确定")
                }
            },
            text = { Text(msg) }
        )
    }
}
