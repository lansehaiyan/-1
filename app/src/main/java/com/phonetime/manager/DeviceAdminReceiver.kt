package com.phonetime.manager

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * 设备管理员接收器
 * 防止孩子卸载 App，同时在需要时可以锁定屏幕
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        val prefs = PreferencesManager(context)
        prefs.setDeviceAdminEnabled(true)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // 家长必须先输入密码才能禁用设备管理员
        return "请输入家长密码后才能关闭设备管理员权限"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        val prefs = PreferencesManager(context)
        prefs.setDeviceAdminEnabled(false)
    }

    override fun onPasswordChanged(context: Context, intent: Intent) {
        super.onPasswordChanged(context, intent)
    }
}
