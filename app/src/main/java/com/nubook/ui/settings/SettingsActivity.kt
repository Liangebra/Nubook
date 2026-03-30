package com.nubook.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.nubook.databinding.ActivitySettingsBinding
import com.nubook.ui.theme.ColorEngine
import com.nubook.ui.base.BaseActivity

/**
 * 设置页面 (V2.1)
 * 仅保留主题色切换
 */
class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupThemeSettings()
    }

    private fun setupThemeSettings() {
        binding.layoutThemeColor.setOnClickListener { showColorPicker() }
        updateThemeUI()
    }

    private fun updateThemeUI() {
        binding.tvCurrentColor.text = ColorEngine.COLOR_NAMES[ColorEngine.getColorIndex()]
        binding.viewColorPreview.background.setTint(ColorEngine.getPrimaryColor())
    }

    private fun showColorPicker() {
        AlertDialog.Builder(this)
            .setTitle("选择主题色")
            .setItems(ColorEngine.COLOR_NAMES) { _, which ->
                ColorEngine.setColorIndex(this, which)
                updateThemeUI()
                Toast.makeText(this, "主题色已更改，重新进入页面后生效", Toast.LENGTH_SHORT).show()
                recreate()
            }
            .show()
    }
}
