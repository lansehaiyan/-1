package com.phonetime.manager

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 时间管理核心逻辑
 * 计算剩余时间、判断是否超时等
 */
class TimeManager(private val prefs: PreferencesManager) {

    /**
     * 获取今天的日期字符串（用于判断是否新的一天）
     */
    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * 检查是否新的一天，如果是则重置使用量
     */
    fun checkNewDayAndReset() {
        val today = getTodayDateString()
        if (prefs.isNewDay(today)) {
            prefs.resetTodayUsage()
            prefs.resetExtraTime()
            prefs.setTodayDate(today)
            prefs.setLocked(false)
        }
    }

    /**
     * 获取总可用时间（毫秒）
     * = 每日限额 + 家长额外增加的时间
     */
    fun getTotalAvailableMs(): Long {
        val dailyLimitMin = prefs.getDailyTimeLimit().toLong()
        val dailyLimitMs = dailyLimitMin * 60 * 1000L
        val extraMs = prefs.getExtraTimeMs()
        return dailyLimitMs + extraMs
    }

    /**
     * 获取今日已使用时长（毫秒）
     */
    fun getUsedMs(): Long = prefs.getTodayUsageMs()

    /**
     * 获取剩余可用时间（毫秒）
     */
    fun getRemainingMs(): Long {
        val remaining = getTotalAvailableMs() - getUsedMs()
        return maxOf(0, remaining)
    }

    /**
     * 是否已超时（用完时间）
     */
    fun isTimeUp(): Boolean = getRemainingMs() <= 0

    /**
     * 添加使用时长
     */
    fun addUsage(ms: Long) {
        prefs.addTodayUsageMs(ms)
    }

    /**
     * 家长增加额外时间
     */
    fun addExtraTime(minutes: Int) {
        prefs.addExtraTime(minutes * 60 * 1000L)
        prefs.setLocked(false) // 解锁
    }

    /**
     * 锁定
     */
    fun lock() {
        prefs.setLocked(true)
    }

    /**
     * 解锁
     */
    fun unlock() {
        prefs.setLocked(false)
    }

    /**
     * 每日限额（分钟）
     */
    fun getDailyLimitMinutes(): Int = prefs.getDailyTimeLimit()

    /**
     * 格式化毫秒为 mm:ss
     */
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    /**
     * 格式化毫秒为更友好的显示
     */
    fun formatTimeReadable(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "%d小时 %d分钟".format(hours, minutes)
            minutes > 0 -> "%d分钟 %d秒".format(minutes, seconds)
            else -> "%d秒".format(seconds)
        }
    }

    /**
     * 初始化新的一天
     */
    fun initializeDay() {
        checkNewDayAndReset()
    }
}
