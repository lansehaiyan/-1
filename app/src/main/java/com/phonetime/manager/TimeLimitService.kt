package com.phonetime.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 前台服务 - 后台持续计时
 * 即使 App 被关闭，服务仍在运行，持续监控使用时长
 */
class TimeLimitService : Service() {

    private lateinit var prefs: PreferencesManager
    private lateinit var timeManager: TimeManager

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var tickFuture: ScheduledFuture<*>? = null
    private var lastTickWallClock: Long = 0L
    private var lastTickElapsed: Long = 0L

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        timeManager = TimeManager(prefs)

        createNotificationChannel()
        lastTickWallClock = System.currentTimeMillis()
        lastTickElapsed = SystemClock.elapsedRealtime()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification(timeManager.getRemainingMs())
        startForeground(NOTIFICATION_ID, notification)

        // 检查新的一天
        timeManager.checkNewDayAndReset()

        // 如果已经是锁定状态，显示锁定通知
        if (prefs.isLocked()) {
            updateLockedNotification()
        }

        // 每秒计时
        startTicking()

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTicking()
        super.onDestroy()
    }

    /**
     * 开始每秒计时
     */
    private fun startTicking() {
        stopTicking()

        tickFuture = executor.scheduleAtFixedRate({
            try {
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - lastTickElapsed
                lastTickElapsed = now

                val remainingMs = timeManager.getRemainingMs()
                val usedMs = timeManager.getUsedMs()

                // 更新使用时长
                if (remainingMs > 0) {
                    prefs.addTodayUsageMs(elapsed)
                }

                val newRemaining = timeManager.getRemainingMs()

                // 检查是否超时
                if (newRemaining <= 0 && !prefs.isLocked()) {
                    onTimeUp()
                } else if (newRemaining > 0) {
                    // 更新通知
                    updateTimerNotification(newRemaining)
                }

                lastTickWallClock = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0, 1, TimeUnit.SECONDS)
    }

    /**
     * 停止计时
     */
    private fun stopTicking() {
        tickFuture?.cancel(false)
        tickFuture = null
    }

    /**
     * 时间到了 - 触发锁定
     */
    private fun onTimeUp() {
        prefs.setLocked(true)
        updateLockedNotification()

        // 启动锁屏 Activity
        val lockIntent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(lockIntent)

        // 尝试用设备管理员锁屏（更强制）
        tryLockDevice()
    }

    /**
     * 尝试用设备管理员强制锁屏
     */
    private fun tryLockDevice() {
        if (prefs.isDeviceAdminEnabled()) {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE)
                as android.app.admin.DevicePolicyManager
            val component = android.content.ComponentName(this, DeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(component)) {
                dpm.lockNow()
            }
        }
    }

    /**
     * 更新计时通知
     */
    private fun updateTimerNotification(remainingMs: Long) {
        val notification = createNotification(remainingMs)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 更新锁定通知
     */
    private fun updateLockedNotification() {
        val notification = createLockedNotification()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 创建计时通知
     */
    private fun createNotification(remainingMs: Long): Notification {
        val remainingSeconds = remainingMs / 1000
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60

        val body = if (remainingMs > 0) {
            "剩余 ${timeManager.formatTimeReadable(remainingMs)}"
        } else {
            "使用时间已用完"
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📱 手机时间管家")
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .build()
    }

    /**
     * 创建锁定通知
     */
    private fun createLockedNotification(): Notification {
        val lockIntent = Intent(this, LockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val lockPendingIntent = PendingIntent.getActivity(
            this, 1, lockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 手机已锁定")
            .setContentText("使用时间已用完，请联系家长解锁")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(lockPendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "使用时长提醒",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示剩余使用时间和锁定状态"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    /**
     * 启动本服务
     */
    companion object {
        private const val CHANNEL_ID = "time_limit_channel"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, TimeLimitService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimeLimitService::class.java)
            context.stopService(intent)
        }
    }
}
