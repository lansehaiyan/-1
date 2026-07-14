package com.phonetime.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 密码和使用设置的安全存储
 * 使用 Android EncryptedSharedPreferences 加密
 */
class PreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "phone_time_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ========== 家长密码 ==========

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun getPin(): String? = prefs.getString(KEY_PIN, null)

    fun isPinSet(): Boolean = getPin() != null

    fun verifyPin(pin: String): Boolean = getPin() == pin

    // ========== 每日使用时长（分钟） ==========

    fun setDailyTimeLimit(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_LIMIT, minutes).apply()
    }

    fun getDailyTimeLimit(): Int = prefs.getInt(KEY_DAILY_LIMIT, DEFAULT_TIME_LIMIT_MINUTES)

    // ========== 每日已使用时长（毫秒） ==========

    fun getTodayUsageMs(): Long = prefs.getLong(KEY_TODAY_USAGE, 0L)

    fun addTodayUsageMs(ms: Long) {
        val current = getTodayUsageMs()
        prefs.edit().putLong(KEY_TODAY_USAGE, current + ms).apply()
    }

    fun resetTodayUsage() {
        prefs.edit().putLong(KEY_TODAY_USAGE, 0L).apply()
    }

    // ========== 每日日期（用于判断是否新的一天） ==========

    fun getTodayDate(): String = prefs.getString(KEY_TODAY_DATE, "") ?: ""

    fun setTodayDate(date: String) {
        prefs.edit().putString(KEY_TODAY_DATE, date).apply()
    }

    fun isNewDay(todayDate: String): Boolean {
        val savedDate = getTodayDate()
        return savedDate != todayDate
    }

    // ========== 额外增加时间（毫秒） ==========

    fun addExtraTime(ms: Long) {
        val current = getExtraTimeMs()
        prefs.edit().putLong(KEY_EXTRA_TIME, current + ms).apply()
    }

    fun getExtraTimeMs(): Long = prefs.getLong(KEY_EXTRA_TIME, 0L)

    fun resetExtraTime() {
        prefs.edit().putLong(KEY_EXTRA_TIME, 0L).apply()
    }

    // ========== 锁定状态 ==========

    fun setLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOCKED, locked).apply()
    }

    fun isLocked(): Boolean = prefs.getBoolean(KEY_IS_LOCKED, false)

    // ========== 应用首次设置完成 ==========

    fun setSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }

    fun isSetupComplete(): Boolean = prefs.getBoolean(KEY_SETUP_COMPLETE, false)

    // ========== 设备管理员激活状态 ==========

    fun setDeviceAdminEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVICE_ADMIN, enabled).apply()
    }

    fun isDeviceAdminEnabled(): Boolean = prefs.getBoolean(KEY_DEVICE_ADMIN, false)

    // ========== 清除所有数据（重置） ==========

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_PIN = "parent_pin"
        private const val KEY_DAILY_LIMIT = "daily_time_limit"
        private const val KEY_TODAY_USAGE = "today_usage_ms"
        private const val KEY_TODAY_DATE = "today_date"
        private const val KEY_EXTRA_TIME = "extra_time_ms"
        private const val KEY_IS_LOCKED = "is_locked"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_DEVICE_ADMIN = "device_admin_enabled"

        const val DEFAULT_TIME_LIMIT_MINUTES = 60
    }
}
