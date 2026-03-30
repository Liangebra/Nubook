package com.nubook.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils

/**
 * 全局色彩双变量变换引擎
 * 管理8种基准色的主变量和柔和变量的计算与切换
 */
object ColorEngine {

    // 8种基准色（主变量）
    val BASE_COLORS = intArrayOf(
        Color.parseColor("#f57c6e"), // 0 珊瑚红（支出/警告）
        Color.parseColor("#f2b56e"), // 1 琥珀橘（标记/提醒）
        Color.parseColor("#fbe79e"), // 2 香草黄（中性/备注）
        Color.parseColor("#84c3b7"), // 3 薄荷绿（收入/增长）
        Color.parseColor("#88d7da"), // 4 湖水青（默认/导航）
        Color.parseColor("#71b8ed"), // 5 天空蓝（链接/图表）
        Color.parseColor("#b8aeea"), // 6 丁香紫（分类/特殊）
        Color.parseColor("#f2a8da")  // 7 樱花粉（个性/定制）
    )

    // 颜色名称
    val COLOR_NAMES = arrayOf(
        "珊瑚红", "琥珀橘", "香草黄", "薄荷绿",
        "湖水青", "天空蓝", "丁香紫", "樱花粉"
    )

    private const val PREFS_NAME = "nubook_theme"
    private const val KEY_COLOR_INDEX = "color_index"

    private var currentIndex = 4 // 默认湖水青

    /**
     * 初始化色彩引擎（从SharedPreferences恢复用户选择）
     */
    fun init(context: Context) {
        val prefs = getPrefs(context)
        currentIndex = prefs.getInt(KEY_COLOR_INDEX, 4)
    }

    /**
     * 设置当前主题色索引
     */
    fun setColorIndex(context: Context, index: Int) {
        currentIndex = index.coerceIn(0, BASE_COLORS.size - 1)
        getPrefs(context).edit().putInt(KEY_COLOR_INDEX, currentIndex).apply()
    }

    /**
     * 获取当前主题色索引
     */
    fun getColorIndex(): Int = currentIndex

    /**
     * 获取当前主变量色（强视觉元素使用）
     */
    @ColorInt
    fun getPrimaryColor(): Int = BASE_COLORS[currentIndex]

    /**
     * 获取当前柔和变量色（弱视觉元素/背景使用）
     * 通过 HSL 色彩空间：降低饱和度30%，提升亮度25%
     */
    @ColorInt
    fun getSoftColor(): Int = computeSoftVariant(BASE_COLORS[currentIndex])

    /**
     * 获取指定索引的主变量色
     */
    @ColorInt
    fun getPrimaryColor(index: Int): Int = BASE_COLORS[index.coerceIn(0, BASE_COLORS.size - 1)]

    /**
     * 获取指定索引的柔和变量色
     */
    @ColorInt
    fun getSoftColor(index: Int): Int =
        computeSoftVariant(BASE_COLORS[index.coerceIn(0, BASE_COLORS.size - 1)])

    /**
     * 获取按下态颜色（加深15%）
     */
    @ColorInt
    fun getPressedColor(): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(getPrimaryColor(), hsl)
        hsl[2] = (hsl[2] * 0.85f).coerceIn(0f, 1f)
        return ColorUtils.HSLToColor(hsl)
    }

    /**
     * 生成柔和变量色列表（用于图表）
     * @param count 需要的颜色数量
     * @return 柔和变量色数组
     */
    fun getSoftColorPalette(count: Int): List<Int> {
        val colors = mutableListOf<Int>()
        for (i in 0 until count) {
            val baseIndex = i % BASE_COLORS.size
            colors.add(computeSoftVariant(BASE_COLORS[baseIndex]))
        }
        return colors
    }

    /**
     * 计算柔和变量色
     * 通过HSL变换：降低饱和度30%，提升亮度25%
     */
    @ColorInt
    private fun computeSoftVariant(@ColorInt baseColor: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(baseColor, hsl)
        // 降低饱和度30%
        hsl[1] = (hsl[1] * 0.7f).coerceIn(0f, 1f)
        // 提升亮度25%（向白色方向偏移）
        hsl[2] = (hsl[2] + (1f - hsl[2]) * 0.25f).coerceIn(0f, 1f)
        return ColorUtils.HSLToColor(hsl)
    }

    /**
     * 获取收入色（薄荷绿）
     */
    @ColorInt
    fun getIncomeColor(): Int = BASE_COLORS[3]

    /**
     * 获取支出色（珊瑚红）
     */
    @ColorInt
    fun getExpenseColor(): Int = BASE_COLORS[0]

    /**
     * 获取资源文件中的颜色 Helper (V2)
     */
    @ColorInt
    fun getColor(context: Context, resId: Int): Int = context.getColor(resId)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
