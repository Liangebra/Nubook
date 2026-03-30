package com.nubook.ui.theme

import android.content.Context

/**
 * 偏好管理器 (V2.1: 已精简，仅保留主题色管理)
 * 主题色由 ColorEngine 独立管理
 */
object PreferenceManager {

    /**
     * 直接返回原始 Context (已移除语言/字体逻辑)
     */
    fun applySettings(context: Context): Context {
        return context
    }
}
