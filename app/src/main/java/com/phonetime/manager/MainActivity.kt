package com.phonetime.manager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.phonetime.manager.ui.ChildScreen
import com.phonetime.manager.ui.ParentSettingsScreen

/**
 * 主活动
 * 第一次打开显示设置向导（设置密码 + 每日时长）
 * 之后显示儿童计时界面
 * 点击"家长设置"进入家长管理界面
 */
class MainActivity : ComponentActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var timeManager: TimeManager
    private val handler = Handler(Looper.getMainLooper())

    // 计时器更新频率（毫秒）
    private val tickInterval = 1000L

    private var tickRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PreferencesManager(this)
        timeManager = TimeManager(prefs)

        // 初始化新的一天
        timeManager.initializeDay()

        // 请求通知权限 (Android 13+)
        requestNotificationPermission()

        // 启动后台服务
        TimeLimitService.startService(this)

        // 检查是否从家长验证进入
        val openParentSettings = intent.getBooleanExtra("open_parent_settings", false)
        var showParentSettings = openParentSettings

        setContent {
            var showSettings by remember { mutableStateOf(showParentSettings) }
            // 实时剩余时间
            var remainingMs by remember { mutableStateOf(timeManager.getRemainingMs()) }
            var totalMs by remember { mutableStateOf(timeManager.getTotalAvailableMs()) }
            var usedMs by remember { mutableStateOf(timeManager.getUsedMs()) }
            var isLocked by remember { mutableStateOf(prefs.isLocked()) }

            Surface(modifier = Modifier.fillMaxSize()) {
                if (showSettings) {
                    ParentSettingsScreen(
                        timeManager = timeManager,
                        onClose = {
                            showSettings = false
                            // 刷新数据
                            remainingMs = timeManager.getRemainingMs()
                            totalMs = timeManager.getTotalAvailableMs()
                            usedMs = timeManager.getUsedMs()
                            isLocked = prefs.isLocked()
                        }
                    )
                } else {
                    // 未设置密码 → 显示设置界面
                    if (!prefs.isPinSet()) {
                        // 强制进设置
                        showSettings = true
                    }

                    ChildScreen(
                        remainingMs = remainingMs,
                        totalMs = totalMs,
                        usedMs = usedMs,
                        isLocked = isLocked,
                        timeManager = timeManager,
                        onSettingsClick = {
                            // 打开家长验证
                            val intent = Intent(this@MainActivity, ParentGateActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }

        // 开始计时器
        startTimer(
            onTick = { ms, total, used, locked ->
                remainingMs = ms
                totalMs = total
                usedMs = used
                isLocked = locked
            }
        )
    }

    /**
     * 启动周期性计时器
     */
    private fun startTimer(onTick: (Long, Long, Long, Boolean) -> Unit) {
        tickRunnable?.let { handler.removeCallbacks(it) }

        tickRunnable = object : Runnable {
            override fun run() {
                // 检查新的一天
                timeManager.checkNewDayAndReset()

                val remaining = timeManager.getRemainingMs()
                val total = timeManager.getTotalAvailableMs()
                val used = timeManager.getUsedMs()
                val locked = prefs.isLocked()

                onTick(remaining, total, used, locked)

                handler.postDelayed(this, tickInterval)
            }
        }

        tickRunnable?.let { handler.post(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 从家长验证返回时刷新
        if (intent.getBooleanExtra("open_parent_settings", false)) {
            // 重启 Activity
            recreate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tickRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "请允许通知以接收使用时长提醒", Toast.LENGTH_LONG).show()
        }
    }
}
