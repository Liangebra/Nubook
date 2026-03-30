package com.nubook.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 基础 Activity (V2.1: 已精简)
 */
abstract class BaseActivity : AppCompatActivity() {

    private var baseThemeIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.nubook.ui.theme.ColorEngine.init(this)
        baseThemeIndex = com.nubook.ui.theme.ColorEngine.getColorIndex()
    }

    override fun onResume() {
        super.onResume()
        if (baseThemeIndex != -1 && baseThemeIndex != com.nubook.ui.theme.ColorEngine.getColorIndex()) {
            recreate()
        }
    }
}
