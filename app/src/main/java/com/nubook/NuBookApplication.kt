package com.nubook

import android.app.Application
import com.nubook.data.local.NuBookDatabase

/**
 * NuBook 应用入口类
 * 负责初始化全局单例，如数据库实例
 */
class NuBookApplication : Application() {

    // 延迟初始化数据库单例
    val database: NuBookDatabase by lazy {
        NuBookDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NuBookApplication
            private set
    }
}
